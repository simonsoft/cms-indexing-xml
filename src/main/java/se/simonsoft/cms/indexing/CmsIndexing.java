package se.simonsoft.cms.indexing;

import se.simonsoft.cms.admin.CmsRepositoryInspection;
import se.simonsoft.cms.item.RepoRevision;

/**
 * Per repository service responsible for keeping all indexes consistent.
 */
public interface CmsIndexing {

	/**
	 * @param repo Service is per repository so this is just to authorize the caller
	 * @return The current highest revision that is sync'd
	 */
	RepoRevision getHead(CmsRepositoryInspection repo);
	
	/**
	 * @param repo Service is per repository so this is just to authorize the caller
	 * @param head the revision to index up to
	 */
	void sync(CmsRepositoryInspection repo, RepoRevision head);
	
}
