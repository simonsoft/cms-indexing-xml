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
package se.simonsoft.cms.indexing.abx;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingHandlerException;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.HandlerProperties;
import se.repos.indexing.item.IndexingItemProgress;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.indexing.IdStrategy;

/**
 * Replacement of the abx:Dependencies and abx:CrossRefs properties.
 * Depends on extraction of references from XML. 
 */
public class HandlerXmlReferences extends HandlerAbxFolders {

	private static final Logger logger = LoggerFactory.getLogger(HandlerXmlReferences.class);
	
	private static final String HOSTFIELD = "repohost";
	
	private static final String REF_FIELD_PREFIX = "ref_xml_";
	private static final String REF_ITEMID_FIELD_PREFIX = "ref_itemid_";
	
	private static final String CATEGORY_DEPENDENCIES = "dependencies";
	
	/**
	 * @param idStrategy to fill the refid field
	 */
	@Inject
	public HandlerXmlReferences(IdStrategy idStrategy) {
		super(idStrategy);
	}
	
	@Override
	public void handle(IndexingItemProgress progress) {
		IndexingDoc fields = progress.getFields();
		String host = (String) fields.getFieldValue(HOSTFIELD);
		if (host == null) {
			throw new IllegalStateException("Depending on indexer that adds host field " + HOSTFIELD);
		}
		
		Set<CmsItemId> dependencyIds = null;
		String[] referenceCategories = {CATEGORY_DEPENDENCIES, "keydefmaps", "graphics", "includes"};
		
		for (String referenceName : referenceCategories) {
			try {
				Set<CmsItemId> ids = handleReferences(fields, host, referenceName);
				
				if (referenceName.equals(CATEGORY_DEPENDENCIES)) {
					dependencyIds = ids;
				}
			} catch (Exception e) {
				logger.warn("Failed to parse {}: {}", referenceName, e.getMessage(), e);
				throw new IndexingHandlerException("Failed to parse " + referenceName + ": " + e.getMessage(), e);
			}
		}
		
		// Add the output of the full dependency list (deduplicated) to refid, the generic field.
		handleCmsItemIds(fields, "refid", dependencyIds);
		handleFolders(fields, "ref_pathparents", dependencyIds);
	}
	

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return new HashSet<Class<? extends IndexingItemHandler>>() {private static final long serialVersionUID = 1L;{
			add(HandlerPathinfo.class);
			add(HandlerProperties.class);
		}};
	}
	
	protected Set<CmsItemId> handleReferences(IndexingDoc fields, String host, String refName) {

		Set<CmsItemId> result = new HashSet<CmsItemId>();
		String itemIds = (String) fields.getFieldValue(REF_ITEMID_FIELD_PREFIX + refName);
		if (itemIds != null && !itemIds.trim().isEmpty()) {
			logger.debug("Refs '{}' extracted by XSL: {}", refName, itemIds);
		}
		

		if (itemIds != null) {
			
			if (itemIds.length() != 0) {
				
				String strategyId;
				
				for (String d : itemIds.trim().split("\\s+")) {
					CmsItemIdArg id = new CmsItemIdArg(d);
					id.setHostname(host);
					
					strategyId = id.getPegRev() != null ?
							idStrategy.getId(id, new RepoRevision(id.getPegRev(), null)) :
							idStrategy.getIdHead(id);
					
					fields.addField(REF_FIELD_PREFIX + refName, strategyId);
					
					result.add(id);
				}
				
			} 
			
		}

		return result;
	}

}
