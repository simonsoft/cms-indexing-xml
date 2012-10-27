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
