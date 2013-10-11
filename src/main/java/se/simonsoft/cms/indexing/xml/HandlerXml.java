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
package se.simonsoft.cms.indexing.xml;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.HandlerProperties;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;
import se.simonsoft.cms.xmlsource.handler.XmlSourceHandler;
import se.simonsoft.cms.xmlsource.handler.XmlSourceReader;

public class HandlerXml implements IndexingItemHandler {

	public final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private XmlFileFilter xmlFileFilter = new XmlFileFilterExtensionAndSvnMimeType(); // TODO inject, mportant customization point
	
	private XmlIndexRestrictFields supportLegacySchema = new XmlIndexRestrictFields(); // TODO do away with gradually
	
	private XmlSourceReader sourceReader = null;
	
	private Set<XmlIndexFieldExtraction> fieldExtraction = null;

	private XmlIndexWriter indexWriter;
	
	/**
	 * @param fieldExtraction a sequence of pluggable extractors that add fields
	 * @param xmlSourceReader that processes the XML into {@link XmlSourceElement}s for the extractors
	 */
	@Inject
	public void setDependenciesXml(
			Set<XmlIndexFieldExtraction> fieldExtraction,
			XmlSourceReader xmlSourceReader
			) {
		this.fieldExtraction = fieldExtraction;
		this.sourceReader = xmlSourceReader;
	}

	@Inject
	public void setDependenciesIndexing(
			XmlIndexWriter indexAddProvider) {
		this.indexWriter = indexAddProvider;
	}
	
	@Override
	public void handle(IndexingItemProgress progress) {
		CmsChangesetItem c = progress.getItem();
		if (c.isOverwritten()) {
			logger.debug("Head indexing skips later overwritten {} at {}", c.getPath(), progress.getRevision());
			return;
		}
		if (c.isFile()) {
			// TODO here we should probably read mime type too, or probably after conversion to CmsItem
			if (xmlFileFilter.isXml(c, progress.getFields())) {
				logger.debug("Changeset content update item {} found", c);
				if (c.isDelete()) {
					indexWriter.deletePath(progress.getRepository(), c);
				} else {
					indexWriter.deletePath(progress.getRepository(), c);
					index(progress);
				}
			} else {
				logger.debug("Ignoring content update item {}, not an XML candidate file type", c);
			}
		} else {
			logger.debug("Ignoring changeset item {}, not a file", c);
		}
		// TODO until we have notification on revision end we commit always
		onRevisionEnd(progress.getRevision());
	}

	protected void index(IndexingItemProgress progress) {
		IndexingDoc itemDoc = cloneItemFields(progress.getFields());
		supportLegacySchema.handle(itemDoc);
		XmlIndexAddSession docHandler = indexWriter.get();
		XmlSourceHandler sourceHandler = new XmlSourceHandlerFieldExtractors(itemDoc, fieldExtraction, docHandler);
		try {
			sourceReader.read(progress.getContents(), sourceHandler);
		} catch (XmlNotWellFormedException e) {
			logger.warn("Skipping XML {}: {}", progress.getFields().getFieldValue("path"), e.getCause());
		}
	}
	
	private IndexingDoc cloneItemFields(IndexingDoc fields) {
		IndexingDoc doc = fields.deepCopy();
		return doc;
	}

	// TODO will never be called now, implement an indexing event interface
	//@Override
	public void onRevisionEnd(RepoRevision revision) {
		indexWriter.onRevisionEnd(revision);
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
