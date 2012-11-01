/**
 * Copyright (C) 2009-2012 Simonsoft Nordic AB
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
package se.simonsoft.cms.indexing.history;

import org.apache.solr.client.solrj.SolrServer;

import se.simonsoft.cms.indexing.IndexingCore;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangeset;

public class CmsIndexingHistoryImpl implements CmsIndexingHistory {

	private CmsRepository repo;

	public CmsIndexingHistoryImpl(CmsRepository repository) {
		this.repo = repository;
	}

	@Override
	public void begin(CmsChangeset revision, IndexingCore core) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void end(CmsChangeset revision, IndexingCore core) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isIncomplete() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public RepoRevision getHeadCompleted(SolrServer core) {
		// TODO Auto-generated method stub
		return null;
	}

}
