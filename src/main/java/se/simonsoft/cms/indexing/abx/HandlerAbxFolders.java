/**
 * Copyright (C) 2009-2017 Simonsoft Nordic AB
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
package se.simonsoft.cms.indexing.abx;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.indexing.IdStrategy;

public abstract class HandlerAbxFolders implements IndexingItemHandler {

	private static final Logger logger = LoggerFactory.getLogger(HandlerAbxFolders.class);
	
	protected final IdStrategy idStrategy;
	
	@Inject
	public HandlerAbxFolders(IdStrategy idStrategy) {
		this.idStrategy = idStrategy;
	}
	
	/**
	 * Takes a set of unique CmsItemId's and filters out unique parent paths
	 * before adding them to the provided folderField.
	 *
	 * @param fields
	 * @param folderField
	 * @param ids Set of CmsItemIdBase since we need to know they are unique
	 * @return 
	 */
	protected IndexingDoc handleFolders(IndexingDoc fields, String folderField, Set<CmsItemId> ids) {

		if (ids != null) {
			
			Set<String> parentPaths = new HashSet<String>();
			String tempPath;
			RepoRevision revision;
			
			for (CmsItemId id : ids) {
				
				for (CmsItemPath ancestor : id.getRelPath().getAncestors()) {
					
					revision = id.getPegRev() != null ? new RepoRevision(id.getPegRev(), null) : null;
					tempPath = revision != null ? idStrategy.getId(id.getRepository(), revision, ancestor) : idStrategy.getIdHead(id.getRepository(), ancestor);
					
					if (!parentPaths.add(tempPath)) {
						logger.trace("Ignored {} from {} as it already exists in the set.", ancestor, id);
					}
				}
				
			}
			
			for (String path : parentPaths) {
				fields.addField(folderField, path);
			}
			
		}
		
		return fields;
	}
	
	protected void handleCmsItemIds(IndexingDoc fields, String fieldName, Set<CmsItemId> ids) {

		if (ids != null) {
			String refId;
			for (CmsItemId itemId : ids) {

				refId = itemId.getPegRev() == null ?
						idStrategy.getIdHead(itemId) :
						idStrategy.getId(itemId, new RepoRevision(itemId.getPegRev(), null));

				fields.addField(fieldName, refId);

			}
		}
	}

}
