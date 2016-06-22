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
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.HandlerProperties;
import se.repos.indexing.item.IndexingItemProgress;

public class HandlerTitleSelection implements IndexingItemHandler{

	private static final String TITLE_FIELD = "title";
	private static final Logger logger = LoggerFactory.getLogger(HandlerTitleSelection.class);
	private Set<String> fieldTitleKeys = new LinkedHashSet<String>();
	/***
	 * Constructor populates a set with possible title properties keys.
	 */
	public HandlerTitleSelection() {
		super();
		fieldTitleKeys.add("prop_cms.title");
		fieldTitleKeys.add("embd_xml_title"); // cms-indexing-xml
		fieldTitleKeys.add("embd_title"); // Tika
		fieldTitleKeys.add("xmp_dc.title");
		fieldTitleKeys.add("xmp_dc.subject");
		fieldTitleKeys.add("xmp_dc.description");
	}

	/***
	 * Looks after possible title values with given keys. The first hit will be indexed.
	 * The order of the set decides the priority.
	 */
	@Override
	public void handle(IndexingItemProgress progress) {
		IndexingDoc doc = progress.getFields();
		
		for(String key: getFieldKeys()) {
			if(doc.containsKey(key)){
			 	String value = doc.getFieldValues(key).iterator().next().toString();
				if(value != null && !value.trim().equals("")) {
					doc.setField(TITLE_FIELD, value);
					logger.info("Indexing value from: " + key + ", the value: " + value);
					break;
				}
			}
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

	public void setFieldKeys(Set<String> fieldTitleKeys) {
		this.fieldTitleKeys = fieldTitleKeys;
	}

	public Set<String> getFieldKeys() {
		return fieldTitleKeys;
	}

}
