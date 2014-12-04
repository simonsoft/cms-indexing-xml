package se.simonsoft.cms.indexing.xml.solr;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlIndexWriterSolrjBackground extends XmlIndexWriterSolrj {

	private final Logger logger = LoggerFactory.getLogger(XmlIndexWriterSolrjBackground.class);
	
	private ExecutorService executor = Executors.newSingleThreadExecutor();

	private long count = 0;
	
	@Inject
	public XmlIndexWriterSolrjBackground(@Named("reposxml") SolrServer core) {
		super(core);
	}

	@Override
	protected void batchSend(Session session) {
		logger.debug("Scheduling xml batch {}, {} elements", ++count, session.size());
		executor.submit(new IndexSend(session, count));
	}
	
	// Probably needed for unit tests
	public void waitForCompletion() {
		// TODO is there anything in the ExecutorService API for this?
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
			batchSend(session);
			logger.debug("Scheduled batch {} completed", id);
			return null;
		}
		
	}

}
