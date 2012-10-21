package se.simonsoft.cms.indexing.history;

import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangeset;

/**
 * Called first and last of every revision's indexing operation to track,
 * in a way that survives application restarts, the highest indexed revision.
 */
public interface CmsIndexingHistory {

	/**
	 * Fast index and commit an entry containing basic changeset information such as date, comment and author.
	 * @param revision The commit
	 */
	void begin(CmsChangeset revision);
	
	/**
	 * Update {@link #begin(CmsChangeset)}'s index entry with a completeness flag, commit to mark revision indexing completed.
	 * @param revision The commit, same instance as at begin
	 */
	void end(CmsChangeset revision);
	
	/**
	 * @return true if there is an indexed changeset begun bot not ended
	 */
	boolean isIncomplete();
	
	/**
	 * @return Highest revision that has been completed
	 */
	RepoRevision getHeadCompleted();
	
}
