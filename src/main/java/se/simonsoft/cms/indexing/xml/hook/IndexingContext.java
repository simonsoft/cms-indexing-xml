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
package se.simonsoft.cms.indexing.xml.hook;

import javax.inject.Singleton;

import se.repos.indexing.item.IndexingItemProgress;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.properties.CmsItemProperties;

/**
 * Provides access to current item during chamgeset processing.
 * 
 * A single instance could be reused if all indexing operations
 * are done from the same changeset iteration; a challenging design
 * but a reasonable goal of hook execution when considering performance.
 * 
 * This impl is compatible with single thread execution for hook,
 * for parallell execution or indexing from other places than hooks
 * (such as reindexing using service layer) thread local storage would be needed.
 * 
 * Values must be set as start of each item's processing.
 */
@Singleton
@Deprecated // should switch to deepClone of fields from ItemPathinfo
public class IndexingContext {

	private IndexingItemProgress progress;

	public RepoRevision getRevision() {
		return progress.getRevision();
	}
	
	/**
	 * @return quite possibly with only name and parent path known
	 */
	public CmsRepository getRepository() {
		return progress.getRepository();
	}
	
	/**
	 * @return when needed we can probably get a CmsItemId here because it will be needed anyway during indexing
	 */
	public CmsItemPath getItemPath() {
		return progress.getItem().getPath();
	}

	/**
	 * @return properties backed by fast or cached access, called from multiple indexing handlers
	 */
	public CmsItemProperties getItemProperties() {
		return progress.getProperties();
	}

	public void setItem(IndexingItemProgress progress) {
		this.progress = progress;
		if (progress.getRevision().getDate() == null) {
			throw new IllegalArgumentException("Revision must be qualified with date, got " + progress.getRevision());
		}
	}
	
}
