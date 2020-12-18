package se.simonsoft.cms.indexing.xml.solr;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.solrj.SolrCommit;
import se.repos.indexing.solrj.SolrOp;

public class SolrCommitExpunge extends SolrOp<UpdateResponse> {

	private boolean expungeDeletes;
	private boolean retryAllowed;
	
	private static final Logger logger = LoggerFactory.getLogger(SolrCommit.class);
	
	public SolrCommitExpunge(SolrClient core, boolean expungeDeletes, boolean retryAllowed) {
		super(core);
		this.expungeDeletes = expungeDeletes;
		this.retryAllowed = retryAllowed;
	}

	@Override
	public UpdateResponse runOp() throws SolrServerException, IOException {
		logger.debug("Committing {} with (expungeDeletes={})", core, expungeDeletes);
		
		// Workaround for Solrj not exposing 'expungeDeletes' in commit().
		AbstractUpdateRequest req = new UpdateRequest().setAction(UpdateRequest.ACTION.COMMIT, true, true, 0, false, expungeDeletes);
		return req.process(core);
	}
	
	@Override
	protected boolean isRetryAllowed() {
		// Retry is a risk for commit but could sometimes be prioritized, e.g. massive deletes with expunge.
		return retryAllowed;
	}
}
