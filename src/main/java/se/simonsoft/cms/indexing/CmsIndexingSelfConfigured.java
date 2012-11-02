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
package se.simonsoft.cms.indexing;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrServer;

import se.simonsoft.cms.indexing.history.CmsIndexingHistoryImpl;
import se.simonsoft.cms.indexing.solr.IndexingCoreSolrj;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.inspection.CmsRepositoryInspection;

/**
 * Initial impl instantiating all dependencies,
 * supporting only the indeixng classes in this module, no extra processing.
 * 
 * We need to switch to instance using dependency injection when we want'
 * to customize indexing based on startup/repository parameters.
 */
public class CmsIndexingSelfConfigured implements CmsIndexing {

	private IndexingCore solrItemCore;
	private CmsIndexingHistoryImpl history;

	@Inject
	public CmsIndexingSelfConfigured(
			CmsRepository repository,
			@Named("repositem") SolrServer solrItemCore) {
		this.solrItemCore = new IndexingCoreSolrj(solrItemCore);
		
		// dependencies
		this.history = new CmsIndexingHistoryImpl(repository);
	}
	
	@Override
	public RepoRevision getHead(CmsRepositoryInspection repo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sync(CmsRepositoryInspection repo, RepoRevision head) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sync(CmsRepositoryInspection repo, RepoRevision head,
			Phase waitFor) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearRevision(RepoRevision revision) {
		// TODO Auto-generated method stub
		
	}
	
}
