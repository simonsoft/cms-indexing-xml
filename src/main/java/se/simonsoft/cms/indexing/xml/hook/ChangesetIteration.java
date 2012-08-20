package se.simonsoft.cms.indexing.xml.hook;

import se.simonsoft.cms.admin.CmsRepositoryInspection;
import se.simonsoft.cms.item.CmsItemLookup;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangeset;

/**
 * Some more design thought is needed for efficient and reusable changeset iteration
 * so we'll let it be TMS-centric and a bit messy in terms of dependencies for now.
 */
public interface ChangesetIteration {

	void onHook(CmsRepositoryInspection repository, RepoRevision revision);
	
	/**
	 * By now we should not need the {@link CmsRepositoryInspection} instance.
	 * @param changeset The full changeset with revision
	 * @param itemAndContentsReader Expected to produce {@link CmsItemAndContents} including info on repository/item ID
	 */
	void onChangeset(CmsChangeset changeset, CmsItemLookup itemAndContentsReader);
	
	/**
	 * Called for any modifications, even if only properties because all elements are indexed with document's svn props.
	 * @param item Likely to be XML
	 */
	void onUpdate(CmsItemAndContents item);
	
}
