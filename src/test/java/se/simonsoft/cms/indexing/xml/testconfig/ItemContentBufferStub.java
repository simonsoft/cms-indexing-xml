package se.simonsoft.cms.indexing.xml.testconfig;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.ItemContentBuffer;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.inspection.CmsRepositoryInspection;

@SuppressWarnings("deprecation")
public class ItemContentBufferStub implements ItemContentBufferStrategy {

	public ItemContentBufferStub() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public ItemContentBuffer getBuffer(CmsRepositoryInspection repository, RepoRevision revision, CmsItemPath path, IndexingDoc pathinfo) {
		// TODO Auto-generated method stub
		return null;
	}

}
