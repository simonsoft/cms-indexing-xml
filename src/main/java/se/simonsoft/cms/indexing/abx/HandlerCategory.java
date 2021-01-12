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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.HandlerProperties;
import se.repos.indexing.item.IndexingItemProgress;

public class HandlerCategory implements IndexingItemHandler {

	private static final String CATEGORY_FIELD = "category";
	
	private static final String CONTENT_TYPE_FIELD = "embd_Content-Type";
	private static final String XML_ELEMENT_FIELD = "embd_xml_name";
	private static final Logger logger = LoggerFactory.getLogger(HandlerCategory.class);
	
	
	public HandlerCategory() {
	}

	/***
	 * Attempt to get a suitably specific category for the item.
	 */
	@Override
	public void handle(IndexingItemProgress progress) {
		IndexingDoc doc = progress.getFields();
		String value = "unknown";
		
		String category = getCategory(doc);
		if (category != null) {
			value = category;
		}

		doc.setField(CATEGORY_FIELD, value);
	}
	
	
	private String getCategory(IndexingDoc doc) {
		
		String xmlCategory = getXmlCategory(doc);
		if (xmlCategory != null) {
			return xmlCategory;
		}
		
		String graphicCategory = getGraphicCategory(doc);
		if (graphicCategory != null) {
			return graphicCategory;
		}
		
		// TODO: Detect Office documents. Likely need a list of mime-types.
		
		// Fallback to first component of mime-type. However "application" is not suitable for users.
		String mimeCategory = getMimeTypeCategory(doc);
		if (mimeCategory != null) {
			if ("application".equals(mimeCategory)) {
				return "other";
			}
			return mimeCategory;
		}
		return null;
	}
	
	
	private String getGraphicCategory(IndexingDoc doc) {
		String mime = getMimeTypeCategory(doc);
		
		// TODO: Consider separating in "graphics-raster" and "graphics-vector" (potentially "graphics-model" for 3D etc). 
		// Any supported graphics format that Tika does does not detect as "image/*" ?
		if ("image".equals(mime)) {
			return "graphics";
		}
		return null;
	}
	
	
	private String getXmlCategory(IndexingDoc doc) {
		
		Collection<Object> flags = doc.getFieldValues("flag");
		if (flags != null && flags.contains("hasxmlerror")) {
			return "xml-error";
		}
		if (flags != null && flags.contains("hasxml")) {
			if (doc.containsKey(XML_ELEMENT_FIELD)) {
				String element = (String) doc.getFieldValue(XML_ELEMENT_FIELD);
				return "xml-" + element;
			} else {
				return "xml"; // Fallback, should be very rare if the file is considered well-formed.
			}
		}
		return null;
	}
	
	
	private String getMimeTypeCategory(IndexingDoc doc) {
		
		if (!doc.containsKey(CONTENT_TYPE_FIELD)) {
			logger.warn("No Content-Type / mime-type extracted for item: {}", doc.getFieldValue("pathfull"));
			return null;
		}
		
		String mime = (String) doc.getFieldValue(CONTENT_TYPE_FIELD);
		String[] mimeParts = mime.split("/;");
		if (mimeParts.length < 2) {
			logger.warn("Content-Type / mime-type malformed '{}' for item: {}", mime, doc.getFieldValue("pathfull"));
			return null;
		}
		return mimeParts[0];
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
