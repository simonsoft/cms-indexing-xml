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

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.impl.CmsItemIdArg;

/**
 * Normalize properties containing a single itemId.
 */
public class HandlerAbxBaseLogicalId implements IndexingItemHandler {

	private static final Logger logger = LoggerFactory.getLogger(HandlerAbxBaseLogicalId.class);
	
	@Override
	public void handle(IndexingItemProgress progress) {
		String[] abxProperties = {"abx.BaseLogicalId"};
		
		for (String propertyName : abxProperties) {
			String fieldName = "prop_" + propertyName;
			String idString = (String) progress.getFields().getFieldValue(fieldName);
			if (idString == null || idString.trim().isEmpty()) {
				continue;
			}
			try {
				CmsItemId id = new CmsItemIdArg(idString);
				progress.getFields().setField(fieldName, id.getLogicalId());
			} catch (Exception e) {
				logger.warn("Failed to parse {}: {}", propertyName, e.getMessage(), e);
				// Non-fatal, for now.
			}
		}
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}

}
