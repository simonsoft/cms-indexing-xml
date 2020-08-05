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

import se.repos.indexing.IndexingHandlerException;
import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.HandlerProperties;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.indexing.IdStrategy;

/**
 * Uses the abx:Dependencies and abx:CrossRefs properties splitting on newline.
 */
public class HandlerAbxDependencies extends HandlerAbxFolders {

	private static final Logger logger = LoggerFactory.getLogger(HandlerAbxDependencies.class);
	
	private static final String HOSTFIELD = "repohost";
	
	/**
	 * @param idStrategy to fill the refid field
	 */
	@Inject
	public HandlerAbxDependencies(IdStrategy idStrategy) {
		super(idStrategy);
	}
	
	@Override
	public void handle(IndexingItemProgress progress) {
		IndexingDoc fields = progress.getFields();
		String host = (String) fields.getFieldValue(HOSTFIELD);
		if (host == null) {
			throw new IllegalStateException("Depending on indexer that adds host field " + HOSTFIELD);
		}
		String[] abxProperties = {"abx.Dependencies", "abx.CrossRefs"};
		
		Set<CmsItemId> dependencyIds = new HashSet<CmsItemId>();
		for (String propertyName : abxProperties) {
			try {
				dependencyIds.addAll(handleAbxProperty(fields, host, propertyName, (String) fields.getFieldValue("prop_" + propertyName)));
			} catch (Exception e) {
				logger.warn("Failed to parse {}: {}", propertyName, e.getMessage(), e);
				throw new IndexingHandlerException("Failed to parse " + propertyName + ": " + e.getMessage(), e);
			}
		}
		
		// This handler does no longer set "refid", moved to XML extraction.
		/*
		String refId;
		for (CmsItemId depItemId : dependencyIds) {
			
			refId = depItemId.getPegRev() == null ?
					idStrategy.getIdHead(depItemId) :
					idStrategy.getId(depItemId, new RepoRevision(depItemId.getPegRev(), null));
			
			fields.addField("refid", refId);
			
		}
		
		handleFolders(fields, "ref_pathparents", dependencyIds);
		*/
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return new HashSet<Class<? extends IndexingItemHandler>>() {private static final long serialVersionUID = 1L;{
			add(HandlerPathinfo.class);
			add(HandlerProperties.class);
		}};
	}
	
	protected Set<CmsItemId> handleAbxProperty(IndexingDoc fields, String host, String propertyName, String abxprop) {

		Set<CmsItemId> result = new HashSet<CmsItemId>();

		if (abxprop != null) {
			
			if (abxprop.length() != 0) {
				
				String strategyId;
				
				for (String d : abxprop.split("\n")) {
					CmsItemIdArg id = new CmsItemIdArg(d);
					id.setHostname(host);
					
					strategyId = id.getPegRev() != null ?
							idStrategy.getId(id, new RepoRevision(id.getPegRev(), null)) :
							idStrategy.getIdHead(id);
					
					fields.addField("ref_" + propertyName, strategyId);
					
					result.add(id);
				}
				
			} else {
				logger.debug("{} property exists but is empty", propertyName);
			}
			
		}
		
		return result;

	}

}
