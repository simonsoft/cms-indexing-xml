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

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;


public class XmlIndexWriterSolrjBackground extends XmlIndexWriterSolrj {

	private final Logger logger = LoggerFactory.getLogger(XmlIndexWriterSolrjBackground.class);
	
	private ExecutorService executor = null;

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
		
		if (executor == null) {
			executor = Executors.newSingleThreadExecutor();
		}
		
		logger.debug("Scheduling xml batch {}, {} elements, {} total", ++count, session.size(), session.sizeContentTotal());
		
		Collection<SolrInputDocument> pending = session.rotatePending();
		executor.submit(new IndexSend(pending, count)); // Throws RejectedExecutionException if executor is shutting down.
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
	
	// Probably needed for unit tests
	// TODO: This wait could be moved to MarkerXmlCommit, per-commit instead of per-document.
	// Things might change when supporting indexing in Lambda.
	public void waitForCompletion() {
		// Is there anything in the ExecutorService API for this? Yes, but we need to shutdown.
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
		executor = null;
	}
	
	
	private class IndexSend implements Callable<Object> {
		
		private Collection<SolrInputDocument> pending;
		private long id;
		
		IndexSend(Collection<SolrInputDocument> pending, long id) {
			this.pending = pending;
			this.id = id;
		}
		
		@Override
		public Object call() throws Exception {
			doBatchSend(pending);
			logger.debug("Scheduled batch {} completed", id);
			return null;
		}
		
	}

}
