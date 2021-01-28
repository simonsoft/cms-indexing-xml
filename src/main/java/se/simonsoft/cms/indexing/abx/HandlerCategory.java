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
import se.simonsoft.cms.indexing.xml.HandlerXml;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.structure.CmsItemClassification;
import se.simonsoft.cms.item.structure.CmsItemClassificationXml;

/**
 * Determines a category useful for display / filtering in UI.
 * The exact labels / buckets can change frequently without warning or use a 
 * different implementation adjusted for specific use-cases.
 * Should not be used in code to determine item type, see {@link HandlerClassification}.
 *
 */
public class HandlerCategory implements IndexingItemHandler {

	private static final String CATEGORY_FIELD = "category";
	
	private static final String CONTENT_TYPE_FIELD = "embd_Content-Type";
	private static final String XML_ELEMENT_FIELD = "embd_xml_name";
	
	private static final String CATEGORY_GRAPHICS = "graphics"; // Consider adding graphics-raster, graphics-vector, graphics-legacy

	private static final String CATEGORY_XML = "xml";

	private static final String CATEGORY_OFFICE = "office";
	private static final String CATEGORY_OFFICE_WORDPROCESSING = "office-wordprocessing";
	private static final String CATEGORY_OFFICE_SPREADSHEEET = "office-spreadsheet";
	private static final String CATEGORY_OFFICE_PRESENTATION = "office-presentation";
	
	private static final CmsItemClassification itemClassificationXml = new CmsItemClassificationXml();
	
	private static final Logger logger = LoggerFactory.getLogger(HandlerCategory.class);
	
	
	public HandlerCategory() {
	}

	/***
	 * Attempt to get a suitably specific category for the item.
	 */
	@Override
	public void handle(IndexingItemProgress progress) {
		
		CmsChangesetItem item = progress.getItem();
		CmsItemPath itemPath = progress.getItem().getPath();
		
		if (item.isDelete()) {
			// No reason to process delete.
			return;
		}

		if (item.isFile() && item.getFilesize() == 0) {
			// Tika has not executed on empty file.
			// Often XML files when reserving the name/number. Can not extract document element name.
			return;
		}
				
		if (itemPath == null) {
			// Repository root.
			return;
		}
		CmsItemId itemId = progress.getRepository().getItemId(itemPath, null);
		
		IndexingDoc doc = progress.getFields();
		String value = "unknown";
		
		String category = getCategory(itemId, doc);
		if (category != null) {
			value = category;
		}

		doc.setField(CATEGORY_FIELD, value);
	}
	
	
	private String getCategory(CmsItemId itemId, IndexingDoc doc) {
		
		String type = (String) doc.getFieldValue("type");
		if (!"file".equals(type)) {
			return type;
		}
		
		String xmlCategory = getXmlCategory(itemId, doc);
		if (xmlCategory != null) {
			return xmlCategory;
		}
		
		String graphicCategory = getGraphicCategory(itemId, doc);
		if (graphicCategory != null) {
			return graphicCategory;
		}
		
		String officeCategory = getOfficeCategory(itemId, doc);
		if (officeCategory != null) {
			return officeCategory;
		}
		
		// Fallback to first component of mime-type. However "application" is not suitable for users.
		String[] mimeCategory = getMimeCategory(doc);
		if (mimeCategory != null) {
			if ("application".equals(mimeCategory[0])) {
				return "other";
			}
			return mimeCategory[0];
		}
		return null;
	}


	private String getGraphicCategory(CmsItemId itemId, IndexingDoc doc) {
		String[] mime = getMimeCategory(doc);

		if (mime == null) {
			return null;
		}

		// Tika struggles to recognize eps files.
		if ("eps".equals(itemId.getRelPath().getExtension())) {
			return CATEGORY_GRAPHICS; // Consider returning "graphics-legacy"
		}
		if ("ai".equals(itemId.getRelPath().getExtension())) {
			return CATEGORY_GRAPHICS;
		}
		
		// TODO: Consider separating in "graphics-raster" and "graphics-vector" (potentially "graphics-model" for 3D etc). 
		// Any supported graphics format that Tika does does not detect as "image/*" ?
		// We have traditionally used the term "graphics" instead of "image". It is more generic and more suitable for vector.
		if ("image".equals(mime[0])) {
			return CATEGORY_GRAPHICS;
		}
		return null;
	}
	
	
	private String getXmlCategory(CmsItemId itemId, IndexingDoc doc) {
		
		if (!itemClassificationXml.isXml(itemId)) {
			// Adding this condition before checking XML error in order to avoid error from html, svg etc.
			return null;
		}
		
		Collection<Object> flags = doc.getFieldValues("flag");
		if (flags != null && flags.contains(HandlerXml.FLAG_XML_ERROR)) {
			return "error-xml";
		}
		if (itemClassificationXml.isXml(itemId)) {
			// Use a stricter criteria to catch only XML useful for authoring.
			// Must avoid classifying SVG as xml instead of graphic.
			if (doc.containsKey(XML_ELEMENT_FIELD)) {
				String element = (String) doc.getFieldValue(XML_ELEMENT_FIELD);
				return CATEGORY_XML + "-" + element;
			} else {
				return CATEGORY_XML; // Fallback, should be very rare if the file is considered well-formed.
			}
		}
		return null;
	}


	private String getOfficeCategory(CmsItemId itemId, IndexingDoc doc) {
		String[] mime = getMimeCategory(doc);

		if (mime == null) {
			return null;
		}

		String mimeType = mime[0];
		String mimeSubtype = mime[1];

		if ("application".equals(mimeType)) {
			switch (mimeSubtype) {
				case "vnd.ms-powerpoint":
				case "vnd.ms-powerpoint.slideshow.macroenabled.12":
				case "vnd.ms-powerpoint.presentation.macroenabled.12":
				case "vnd.openxmlformats-officedocument.presentationml.template":
				case "vnd.openxmlformats-officedocument.presentationml.slideshow":
				case "vnd.openxmlformats-officedocument.presentationml.presentation":
					return CATEGORY_OFFICE_PRESENTATION;
				case "msword":
				case "vnd.ms-word.document.macroenabled.12":
				case "vnd.ms-word.template.macroenabled.12":
				case "vnd.openxmlformats-officedocument.wordprocessingml.template":
				case "vnd.openxmlformats-officedocument.wordprocessingml.document":
					return CATEGORY_OFFICE_WORDPROCESSING;
				case "vnd.ms-excel":
				case "vnd.ms-excel.sheet.macroenabled.12":
				case "vnd.openxmlformats-officedocument.spreadsheetml.sheet":
				case "vnd.openxmlformats-officedocument.spreadsheetml.template":
					return CATEGORY_OFFICE_SPREADSHEEET;
				case "x-tika-ooxml":
				case "x-tika-ooxml-protected":
				case "x-tika-msoffice-embedded; format=ole10_native":
					return CATEGORY_OFFICE;
			}
		}
		return null;
	}


	private String[] getMimeCategory(IndexingDoc doc) {
		
		if (!doc.containsKey(CONTENT_TYPE_FIELD)) {
			logger.warn("No Content-Type / mime-type extracted for item: {}", doc.getFieldValue("pathfull"));
			return null;
		}
		
		String mime = (String) doc.getFieldValue(CONTENT_TYPE_FIELD);
		String[] mimeParts = mime.split("[/;]");
		if (mimeParts.length < 2) {
			logger.warn("Content-Type / mime-type malformed '{}' for item: {}", mime, doc.getFieldValue("pathfull"));
			return null;
		}
		return mimeParts;
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
