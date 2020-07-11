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

import java.util.Collection;
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
import se.simonsoft.cms.item.impl.CmsItemIdFragment;
import se.simonsoft.cms.item.indexing.IdStrategy;

/**
 * Supplement to abx:*Masters properties.
 * The properties are used for tracking Releases and Translations.
 * This XML extraction provides additional reporting. 
 * Depends on extraction of relations from XML. 
 */
public class HandlerXmlMasters extends HandlerAbxFolders {

	private static final Logger logger = LoggerFactory.getLogger(HandlerXmlMasters.class);
	
	private static final String HOSTFIELD = "repohost";
	private static final String ABX_AM_FIELD = "rel_abx.AuthorMaster";
	private static final String ABX_RM_FIELD = "rel_abx.ReleaseMaster";
	private static final String ABX_TM_FIELD = "rel_abx.TranslationMaster";
	
	private static final String REL_FIELD_PREFIX = "rel_xml_";
	private static final String REL_ITEMID_FIELD_PREFIX = "rel_itemid_";
	
	/**
	 * @param idStrategy to fill the refid field
	 */
	@Inject
	public HandlerXmlMasters(IdStrategy idStrategy) {
		super(idStrategy);
	}
	
	@Override
	public void handle(IndexingItemProgress progress) {
		IndexingDoc fields = progress.getFields();
		String host = (String) fields.getFieldValue(HOSTFIELD);
		if (host == null) {
			throw new IllegalStateException("Depending on indexer that adds host field " + HOSTFIELD);
		}
		
		String[] relationCategories = {"rlogicalid"};
		
		for (String relName : relationCategories) {
			try {
				Set<CmsItemId> ids = handleRelations(fields, host, relName);
				logger.trace("Rels '{}' extracted: {}", relName, ids.size());
				
			} catch (Exception e) {
				logger.warn("Failed to parse {}: {}", relName, e.getMessage(), e);
				throw new IndexingHandlerException("Failed to parse " + relName + ": " + e.getMessage(), e);
			}
		}
		// Copy rlogicalid field to different fields depending on whether this item is a Release or Translation.
		handleRelationsRlogicalid(fields);
		
		// Always remove the rel_itemid_* fields:
		// - Reduce index size
		// - Solr6 does not allow large string fields. Must otherwise split into multiValue.
		for (String relName : relationCategories) {
			fields.removeField(REL_ITEMID_FIELD_PREFIX + relName);
		}
	}
	

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return new HashSet<Class<? extends IndexingItemHandler>>() {private static final long serialVersionUID = 1L;{
			add(HandlerPathinfo.class);
			add(HandlerProperties.class);
		}};
	}
	
	protected Set<CmsItemId> handleRelations(IndexingDoc fields, String host, String relName) {

		Set<CmsItemId> result = new HashSet<CmsItemId>();
		String itemIds = (String) fields.getFieldValue(REL_ITEMID_FIELD_PREFIX + relName);
		
		if (itemIds != null && !itemIds.trim().isEmpty()) {
			logger.trace("Rels '{}' extracted by XSL: {}", relName, itemIds);
		}
		

		if (itemIds != null) {
			
			if (itemIds.length() != 0) {
				
				String strategyId;
				
				for (String d : itemIds.trim().split("\\s+")) {
					CmsItemIdFragment idFrag = new CmsItemIdFragment(d);
					CmsItemIdArg id = (CmsItemIdArg) idFrag.getItemId();
					id.setHostname(host);
					
					strategyId = id.getPegRev() != null ?
							idStrategy.getId(id, new RepoRevision(id.getPegRev(), null)) :
							idStrategy.getIdHead(id);
					
					fields.addField(REL_FIELD_PREFIX + relName, strategyId);
					
					result.add(id);
				}
				
			} 
			
		}
		return result;
	}
	
	
	
	protected void handleRelationsRlogicalid(IndexingDoc fields) {
		
		Collection<Object> rlogicalId = fields.getFieldValues(REL_FIELD_PREFIX + "rlogicalid");
		
		if (rlogicalId == null || rlogicalId.isEmpty()) {
			return;
		}
	
		if (fields.containsKey(ABX_AM_FIELD)) {
			addFieldValues(fields, REL_FIELD_PREFIX + "AuthorMaster", rlogicalId);
		}
		if (fields.containsKey(ABX_RM_FIELD)) {
			addFieldValues(fields, REL_FIELD_PREFIX + "ReleaseMaster", rlogicalId);
		}
		if (fields.containsKey(ABX_TM_FIELD)) {
			addFieldValues(fields, REL_FIELD_PREFIX + "TranslationMaster", rlogicalId);
		}
	}
	
	private void addFieldValues(IndexingDoc fields, String fieldname, Collection<Object> values) {
		
		for (Object v: values) {
			fields.addField(fieldname, v);
		}
	}

}
