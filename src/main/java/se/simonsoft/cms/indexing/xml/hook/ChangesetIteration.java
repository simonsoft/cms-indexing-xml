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
