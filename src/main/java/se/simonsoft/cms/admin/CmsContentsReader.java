package se.simonsoft.cms.admin;

import java.io.OutputStream;

import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangeset;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.properties.CmsItemProperties;

/**
 * Provides privileged and fast access to item contents.
 * 
 * Using a separate service instead of access through {@link CmsChangeset}
 * because there is no relation to the current commit.
 * 
 * In particular we must support reading of {@link CmsChangesetItem}
 * followed by the item from {@link CmsChangesetItem#getPreviousChange()}.
 */
public interface CmsContentsReader {

	void getContents(CmsRepositoryInspection repository, RepoRevision revision, CmsItemPath path, OutputStream out);

	CmsItemProperties getProperties(CmsRepositoryInspection repository, RepoRevision revision, CmsItemPath path);
	
	/**
	 * Can this be done with svnlook or do we need to parse the full changeset diff?
	 * Maybe diff is better suited for inclusion in the Changeset API,
	 * supporting a changeset viewer like the one in Trac.
	 */
	void getDiff();
	
}
