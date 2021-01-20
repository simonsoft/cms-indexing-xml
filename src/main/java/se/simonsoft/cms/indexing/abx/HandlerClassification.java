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

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.HandlerProperties;
import se.repos.indexing.item.IndexingItemProgress;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.structure.CmsItemClassification;
import se.simonsoft.cms.item.structure.CmsItemClassificationAdapterFiletypes;
import se.simonsoft.cms.item.structure.CmsItemClassificationGraphicWeb;
import se.simonsoft.cms.item.structure.CmsItemClassificationXml;

/**
 * Classification of items providing one or more flags for known item types. 
 *
 */
public class HandlerClassification implements IndexingItemHandler {

	private static final String FLAG_FIELD = "flag";
	
	
	private static final CmsItemClassification itemClassificationXml = new CmsItemClassificationXml();
	private static final CmsItemClassification itemClassificationAbx = new CmsItemClassificationAdapterFiletypes();
	private static final CmsItemClassification itemClassificationGraphicWeb = new CmsItemClassificationGraphicWeb();
	
	
	public HandlerClassification() {
	}

	/***
	 * Attempt to get a suitably specific category for the item.
	 */
	@Override
	public void handle(IndexingItemProgress progress) {
		
		CmsChangesetItem item = progress.getItem();
		
		if (item.isFolder()) {
			// Folders can easily be detected by the "type" field.
			return;
		}
		
		if (item.isDelete()) {
			// No reason to process delete.
			return;
		}

		if (item.isFile() && item.getFilesize() == 0) {
			// Tika has not executed on empty file.
			// Often XML files when reserving the name/number. Can not extract document element name.
			// Neither XML nor graphics are useful when empty.
			return;
		}
		
		setClassificationFlags(progress);
	}
	
	
	private void setClassificationFlags(IndexingItemProgress progress) {
		
		CmsItemPath itemPath = progress.getItem().getPath();
		
		if (itemPath == null) {
			return;
		}
		CmsItemId itemId = progress.getRepository().getItemId(itemPath, null);
		
		if (itemClassificationXml.isXml(itemId)) {
			progress.getFields().addField(FLAG_FIELD, "isxml");
		}
		
		// Includes a number of extensions containing XML but more specific to Arbortext. 
		if (itemClassificationAbx.isXml(itemId)) {
			progress.getFields().addField(FLAG_FIELD, "isxmlabx");
		}
		
		// TODO: Implement flag "isgraphic". Needs some consideration.
		
		// TODO: Improve selection of graphics supported by Thumbnailing. Currently same as Arbortext.
		if (itemClassificationAbx.isGraphic(itemId)) {
			progress.getFields().addField(FLAG_FIELD, "isgraphicthn");
		}
		
		// Includes formats that require transformation by CMS, e.g. Photoshop.
		if (itemClassificationAbx.isGraphic(itemId)) {
			progress.getFields().addField(FLAG_FIELD, "isgraphicabx");
		}
		
		if (itemClassificationGraphicWeb.isGraphic(itemId)) {
			progress.getFields().addField(FLAG_FIELD, "isgraphicweb");
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
