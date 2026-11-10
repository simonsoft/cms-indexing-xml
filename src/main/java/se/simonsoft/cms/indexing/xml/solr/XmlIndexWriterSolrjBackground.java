/**
 * Copyright (C) 2009-2017 Simonsoft Nordic AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.simonsoft.cms.indexing.xml.solr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;


public class XmlIndexWriterSolrjBackground extends XmlIndexWriterSolrj {

	private final Logger logger = LoggerFactory.getLogger(XmlIndexWriterSolrjBackground.class);
	
	private ThreadPoolExecutor executor = null;
	private final List<Future<Object>> pendingFutures = new ArrayList<Future<Object>>();

	private long count = 0;
	
	@Inject
	public XmlIndexWriterSolrjBackground(@Named("reposxml") SolrClient core) {
		super(core);
	}

	@Override
	protected void batchSend(Session session) {
		
		if (session.size() == 0) {
			logger.warn("Send to solr attempted with empty document list");
			return;
		}
		submitSend(session);
	}
	
	private void submitSend(Session session) {
		session.checkCurrent();
		if (executor == null) {
			executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
		}
		
		logger.debug("Scheduling xml batch {}, {} elements, {} total", ++count, session.size(), session.sizeContentTotal());
		
		Collection<SolrInputDocument> pending = session.rotatePending();
		// Throws RejectedExecutionException if executor is shutting down.
		// Future is kept and inspected in waitForCompletion() so a failed batch is never silently dropped (CMS-1892).
		Future<Object> future = executor.submit(new IndexSend(pending, count, session));
		pendingFutures.add(future);
	}
	
	@Override
	protected void sessionEnd(Session session) {
		
		// Send the last batch.
		if (session.size() != 0) {
			submitSend(session);
		}
		
		Date start = new Date();
		waitForCompletion();
		Date completed = new Date();
		// Logging in info level because this can show if XML processing outpaces Solr, which would build RAM consumption.
		logger.info("Awaited completion of Solr Background executor: {} ms", completed.getTime() - start.getTime());
	}
	
	@Override
	protected void sessionAbort(Session session) {
		// Do not interrupt an active Solr request. Await it before the caller cleans up.
		if (executor != null) {
			for (Future<Object> future : pendingFutures) {
				// Only cancel tasks removed from the queue; running failures must remain observable.
				if (executor.getQueue().remove(future)) {
					future.cancel(false);
				}
			}
		}
		waitForCompletion();
	}

	// Probably needed for unit tests
	// TODO: This wait could be moved to MarkerXmlCommit, per-commit instead of per-document.
	// Things might change when supporting indexing in Lambda.
	public void waitForCompletion() {
		// Is there anything in the ExecutorService API for this? Yes, but we need to shutdown.
		ExecutorService executor = this.executor;
		if (executor == null) {
			return;
		}
		List<Future<Object>> futures = new ArrayList<Future<Object>>(pendingFutures);
		executor.shutdown();
		try {
			// #1094 Issuing SolR commit without awaiting full completion will make the resulting searcher incomplete.
			boolean terminated = executor.awaitTermination(60, TimeUnit.SECONDS);
			if (!terminated) {
				logger.error("Completion of Solr Background executor timed out, XML index will likely be incomplete until next commit.");
				// #1346 Probably need to treat background executor timeout as a failure to ensure another attempt is made.
				throw new RuntimeException("Completion of Solr Background executor timed out.");
			}
		} catch (InterruptedException e) {
			String msg = MessageFormatter.format("Failed to await shutdown of Solr Background executor: {}", e.getMessage()).getMessage();
			logger.warn(msg, e);
			throw new RuntimeException(msg);
		}
		// Keep a timed-out executor reachable so abort can still drain it before cleanup.
		this.executor = null;
		pendingFutures.clear();
		// CMS-1892: awaitTermination only confirms the tasks have finished, not that they succeeded.
		// A batch that failed inside IndexSend.call() would otherwise be silently discarded here.
		for (Future<Object> future : futures) {
			if (future.isCancelled()) {
				continue;
			}
			try {
				future.get();
			} catch (ExecutionException e) {
				Throwable cause = e.getCause() != null ? e.getCause() : e;
				String msg = MessageFormatter.format("Solr Background executor batch failed: {}", cause.getMessage()).getMessage();
				logger.error(msg, cause);
				throw new RuntimeException(msg, cause);
			} catch (InterruptedException e) {
				String msg = MessageFormatter.format("Interrupted while collecting result of Solr Background executor batch: {}", e.getMessage()).getMessage();
				logger.warn(msg, e);
				throw new RuntimeException(msg, e);
			}
		}
	}
	
	
	private class IndexSend implements Callable<Object> {
		
		private Collection<SolrInputDocument> pending;
		private long id;
		private final Session session;

		IndexSend(Collection<SolrInputDocument> pending, long id, Session session) {
			this.pending = pending;
			this.id = id;
			this.session = session;
		}
		
		@Override
		public Object call() throws Exception {
			doBatchSend(pending, session);
			logger.debug("Scheduled batch {} completed", id);
			return null;
		}
		
	}

}
