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
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.HandlerProperties;
import se.repos.indexing.item.IndexingItemProgress;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;

// NOTE: This handler is NOT active. 
// Inability to modify Tika 'text' field depends on order, must be configured in repos-indexing-standalone.
public class HandlerTextSelection implements IndexingItemHandler{

	private static final String TEXT_FIELD = "text";
	private static final Logger logger = LoggerFactory.getLogger(HandlerTextSelection.class);
	private Set<String> fields = new LinkedHashSet<String>();
	/***
	 * Constructor populates a set with possible title properties keys.
	 */
	public HandlerTextSelection() {
		super();
		fields.add("embd_xml_text"); // cms-indexing-xml
	}

	/***
	 * Looks after possible text values with given keys. The first hit will be indexed.
	 * The order of the set decides the priority.
	 * Fallback is the Tika field already placed in 'text' field by repos-indexing-fulltext.
	 */
	@Override
	public void handle(IndexingItemProgress progress) {
		CmsChangesetItem item = progress.getItem();
		if (item.isDelete()) {
			// No reason to process delete.
			return;
		}
		
		IndexingDoc doc = progress.getFields();
		
		// TODO: Remove all fulltext search if head:false, see repos-indexing-fulltext.
		
		for(String key: this.fields) {
			if(doc.containsKey(key)){
			 	String value = doc.getFieldValues(key).iterator().next().toString();
				if(value != null && !value.trim().equals("")) {
					doc.setField(TEXT_FIELD, value);
					logger.info("Indexing text from {}", key);
					break;
				}
			}
		}
		
		// Removing the temporary text fields, might be large.
		for(String key: this.fields) {
			doc.removeField(key);
		}
		
	}

	@SuppressWarnings("serial")
	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return new HashSet<Class<? extends IndexingItemHandler>>() {{
			add(HandlerPathinfo.class);
			add(HandlerProperties.class);
		}};
	}


}
