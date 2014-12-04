/**
 * Copyright (C) 2009-2013 Simonsoft Nordic AB
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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;


public class XmlIndexWriterSolrjBackground extends XmlIndexWriterSolrj {

	private final Logger logger = LoggerFactory.getLogger(XmlIndexWriterSolrjBackground.class);
	
	private ExecutorService executor = null;

	private long count = 0;
	
	@Inject
	public XmlIndexWriterSolrjBackground(@Named("reposxml") SolrServer core) {
		super(core);
	}

	@Override
	protected void batchSend(Session session) {
		
		if (executor == null) {
			executor = Executors.newSingleThreadExecutor();
		}
		
		logger.debug("Scheduling xml batch {}, {} elements", ++count, session.size());
		executor.submit(new IndexSend(session, count)); // Throws RejectedExecutionException if executor is shutting down.
	}
	
	@Override
	protected void sessionEnd(Session session) {
		
		batchSend(session);
		waitForCompletion();
		logger.debug("Awaited completion of Solr Background executor");
	}
	
	// Probably needed for unit tests
	public void waitForCompletion() {
		// Is there anything in the ExecutorService API for this? Yes, but we need to shutdown.
		executor.shutdown();
		try {
			executor.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			String msg = MessageFormatter.format("Failed to await shutdown of Solr Background executor: {}", e.getMessage()).getMessage();
			logger.warn(msg, e);
			throw new RuntimeException(msg);
		}
		executor = null;
	}
	
	// Access from unit tests would probably require some design changes unless we expose waiting this ugly way
	public static void testingWaitForCompletion() {
		// TODO in that case
	}
	
	private class IndexSend implements Callable<Object> {
		
		private Session session;
		private long id;
		
		IndexSend(Session session, long id) {
			this.session = session;
			this.id = id;
		}
		
		@Override
		public Object call() throws Exception {
			doBatchSend(session);
			logger.debug("Scheduled batch {} completed", id);
			return null;
		}
		
	}

}
