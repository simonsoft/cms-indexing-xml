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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.HandlerProperties;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.ItemPropertiesBufferStrategy;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.info.CmsItemNotFoundException;
import se.simonsoft.cms.item.properties.CmsItemProperties;
import se.simonsoft.cms.item.structure.CmsItemClassification;
import se.simonsoft.cms.item.structure.CmsItemClassificationAdapterFiletypes;
import se.simonsoft.cms.item.structure.CmsItemClassificationGraphicWeb;
import se.simonsoft.cms.item.structure.CmsItemClassificationXml;

/**
 * Classification of items providing one or more flags for known item types. 
 *
 */
public class HandlerClassification implements IndexingItemHandler {

	private static final Logger logger = LoggerFactory.getLogger(HandlerClassification.class);
	
	private static final String FLAG_FIELD = "flag";
	
	
	private static final CmsItemClassification itemClassificationXml = new CmsItemClassificationXml();
	private static final CmsItemClassification itemClassificationAbx = new CmsItemClassificationAdapterFiletypes();
	private static final CmsItemClassification itemClassificationGraphicWeb = new CmsItemClassificationGraphicWeb();
	
	private final ItemPropertiesBufferStrategy itemPropertiesBuffer;
	
	@Inject
	public HandlerClassification(ItemPropertiesBufferStrategy itemPropertiesBuffer) {
		this.itemPropertiesBuffer = itemPropertiesBuffer;
	}

	/***
	 * Attempt to get a suitably specific category for the item.
	 */
	@Override
	public void handle(IndexingItemProgress progress) {
		
		CmsChangesetItem item = progress.getItem();
		
		if (item.isFolder()) {
			// Folders can easily be detected by the "type" field.
			setFolderFlags(progress);
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
		// Classification of files.
		setClassificationFlags(progress);
		
		// #1613 Extract pathdirname and pathdirnonshard for faceting. 
		setPathdirFacet(progress);
	}
	
	private void setFolderFlags(IndexingItemProgress progress) {
		if (isCmsClass(progress.getFields(), "shardparent")) {
			progress.getFields().addField(FLAG_FIELD, "isshardparent");
		}
		
		// #1511 Mark shardparent children with 'isshard' flag.
		if (isShard(progress.getItem().getPath(), progress.getRevision())) {
			progress.getFields().addField(FLAG_FIELD, "isshard");
		}
	}
	
	// path is a shard if the parent is a "shardparent".
	private boolean isShard(CmsItemPath path, RepoRevision revision) {
		
		CmsItemProperties propsParent = getPropertiesParent(path, revision);
		if (propsParent != null && isCmsClass(propsParent, "shardparent")) {
			return true;
		}
		return false;
	}
	
	private CmsItemProperties getPropertiesParent(CmsItemPath path, RepoRevision revision) {
		if (path == null) {
			return null;
		}
		CmsItemPath parent = path.getParent();
		if (parent == null) {
			return null;
		}
		
		CmsItemProperties props = null;
		try {
			// Typically supported by cms-backend-filexml
			props = this.itemPropertiesBuffer.getProperties(revision, parent);
		} catch (UnsupportedOperationException e) {
			logger.warn("Parent folder getProperties failed: {}", e.getMessage());
			logger.trace("Parent folder getProperties failed: {}", e.getMessage(), e);
		} catch (CmsItemNotFoundException e) {
			logger.info("Parent folder not found at {}: {}", revision, parent, e);
		} catch (Exception e) {
			logger.warn("Parent folder getProperties failed: {}", e.getMessage(), e);
		}
		return props;
	}
	
	// #1613 Extract pathdirname and pathdirnonshard for faceting.
	private void setPathdirFacet(IndexingItemProgress progress) {
		CmsItemPath itemPath = progress.getItem().getPath();
		
		if (itemPath == null) {
			return;
		}
		CmsItemPath folder = itemPath.getParent();
		if (folder == null) {
			return;
		}
		
		int num = itemPath.getPathSegments().size();
		String pathdir;
		// Determine if the folder if this file is a shard.
		if (isShard(folder, progress.getRevision())) {
			pathdir = itemPath.getPathSegments().get(num-3);
		} else {
			pathdir = itemPath.getPathSegments().get(num-2);
		}
		progress.getFields().addField("meta_s_s_pathdirname", itemPath.getPathSegments().get(num-2));
		progress.getFields().addField("meta_s_s_pathdirnonshard", pathdir);
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
		
		// TODO: The flag "isgraphic" will likely be a customization point in the future.
		// Systems with web-editor will == "isgraphicweb" while systems with arbortext == "isgraphicabx".
		if (itemClassificationAbx.isGraphic(itemId)) {
			progress.getFields().addField(FLAG_FIELD, "isgraphic");
		}
		
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
		
		if (isCmsClass(progress.getFields(), "template")) {
			progress.getFields().addField(FLAG_FIELD, "istemplate");
		}
	}
	
	public static boolean isCmsClass(IndexingDoc f, String name) {
		String itemClass = (String) f.getFieldValue("prop_cms.class");
		if (itemClass == null) {
			return false;
		}
		String[] a = itemClass.split(" ");
		return Arrays.asList(a).contains(name);
	}
	
	public static boolean isCmsClass(CmsItemProperties props, String name) {
		String itemClass = props.getString("cms:class");
		if (itemClass == null) {
			return false;
		}
		String[] a = itemClass.split(" ");
		return Arrays.asList(a).contains(name);
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
