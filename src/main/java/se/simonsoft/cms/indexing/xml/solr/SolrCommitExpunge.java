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
	
	private static final Logger logger = LoggerFactory.getLogger(SolrCommitExpunge.class);
	
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
		UpdateResponse response = req.process(core);
		doLogSlowQuery(core, "commit (expungeDeletes=" + expungeDeletes + ")", "-", response);
		return response;
	}
	
	@Override
	protected boolean isRetryAllowed() {
		// Retry is a risk for commit but could sometimes be prioritized, e.g. massive deletes with expunge.
		return retryAllowed;
	}
}
