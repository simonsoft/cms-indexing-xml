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
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangeset;

/**
 * Called first and last of every revision's indexing operation to track,
 * in a way that survives application restarts, the highest indexed revision.
 * <p>
 * Note that revprop changes are currently not supported.
 * Reindex is required after revprop change.
 */
public interface CmsIndexingHistory {

	/**
	 * Fast index and commit an entry containing basic changeset information such as date, comment and author.
	 * @param revision The commit
	 */
	void begin(CmsChangeset revision, IndexingCore core);
	
	/**
	 * Update {@link #begin(CmsChangeset)}'s index entry with a completeness flag, commit to mark revision indexing completed.
	 * @param revision The commit, same instance as at begin
	 */
	void end(CmsChangeset revision, IndexingCore core);
	
	/**
	 * @return true if there is an indexed changeset begun bot not ended,
	 * currently always false after application restart - started revisions must be overwritten
	 */
	boolean isIncomplete();
	
	/**
	 * @return Highest revision that has been completed
	 */
	RepoRevision getHeadCompleted(SolrServer core);
	
}
