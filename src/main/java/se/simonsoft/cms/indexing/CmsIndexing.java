/**
 * Copyright (C) 2009-2013 Simonsoft Nordic AB
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

import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.inspection.CmsRepositoryInspection;

/**
 * Per repository service responsible for keeping all indexes consistent.
 */
public interface CmsIndexing {

	enum Phase { structure, meta, full };
	
	/**
	 * @param repo Service is per repository so this is just to authorize the caller for administrative access
	 * @return The current highest revision that is sync'd
	 */
	RepoRevision getHead(CmsRepositoryInspection repo);
	
	/**
	 * @param repo Service is per repository so this is just to authorize the caller for administrative access
	 * @param head the revision to index up to
	 */
	void sync(CmsRepositoryInspection repo, RepoRevision head);
	
	/**
	 * @param repo Service is per repository so this is just to authorize the caller for administrative access
	 * @param head the revision to index up to
	 */
	void sync(CmsRepositoryInspection repo, RepoRevision head, Phase waitFor);
	
	/**
	 * Remove all indexed data for the current repository. 
	 */
	void clear();
	
	/**
	 * Make private?
	 * Remove all indexed data for a specific revision.
	 * Will most likely be allowed for current highest indexed (or highest + 1),
	 * primarily to set indexing to a known state after application restart.
	 * @param revision The revision to clear
	 */
	void clearRevision(RepoRevision revision);
	
}
