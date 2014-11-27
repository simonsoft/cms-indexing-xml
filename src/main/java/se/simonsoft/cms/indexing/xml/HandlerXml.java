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
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;
import se.simonsoft.cms.xmlsource.handler.XmlSourceHandler;
import se.simonsoft.cms.xmlsource.handler.XmlSourceReader;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceReaderS9api;

public class HandlerXml implements IndexingItemHandler {

	public static final String FLAG_XML = "hasxml";
	
	public final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private XmlFileFilter xmlFileFilter = new XmlFileFilterExtensionAndSvnMimeType(); // TODO inject, important customization point
	
	private XmlIndexRestrictFields supportLegacySchema = new XmlIndexRestrictFields(); // TODO do away with gradually
	
	private XmlSourceReader sourceReader = new XmlSourceReaderS9api(); // Hard coding this impl for now (might want to keep injecting JDOM impl in other modules).
	
	private Set<XmlIndexFieldExtraction> fieldExtraction = null;

	private XmlIndexWriter indexWriter;
	
	/**
	 * @param fieldExtraction a sequence of pluggable extractors that add fields
	 * @param xmlSourceReader that processes the XML into {@link XmlSourceElement}s for the extractors
	 */
	@Inject
	public void setFieldExtraction (
			Set<XmlIndexFieldExtraction> fieldExtraction
			) {
		this.fieldExtraction = fieldExtraction;
	}

	@Inject
	public void setDependenciesIndexing(
			XmlIndexWriter indexAddProvider) {
		this.indexWriter = indexAddProvider;
	}
	
	/**
	 * Intentionally not injected since we currently want to select implementation independently
	 * of what other modules use.
	 * @param xmlSourceReader
	 */
	public void setSourceReader(XmlSourceReader xmlSourceReader) {
		
		this.sourceReader = xmlSourceReader;
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
				logger.trace("Changeset content update item {} found", c);
				if (c.isDelete()) {
					indexWriter.deletePath(progress.getRepository(), c);
				} else {
					indexWriter.deletePath(progress.getRepository(), c);
					index(progress);
				}
			} else {
				logger.trace("Ignoring content update item {}, not an XML candidate file type", c);
			}
		} else {
			logger.trace("Ignoring changeset item {}, not a file", c);
		}
	}

	protected void index(IndexingItemProgress progress) {
		
		if (sourceReader == null) {
			throw new IllegalStateException("No XmlSourceHandler has been provided.");
		}
		
		IndexingDoc itemDoc = cloneItemFields(progress.getFields());
		supportLegacySchema.handle(itemDoc);
		XmlIndexAddSession docHandler = indexWriter.get();
		XmlSourceHandler sourceHandler = new XmlSourceHandlerFieldExtractors(itemDoc, fieldExtraction, docHandler);
		try {
			sourceReader.read(progress.getContents(), sourceHandler);
			// success, flag this
			progress.getFields().addField("flag", FLAG_XML);
		} catch (XmlNotWellFormedException e) { 
			// We assume that fulltext indexing will get the same error and set a text_error for this item.
			// Otherwise there'll be no trace other than the log of why the file was skipped.
			logger.error("Invalid XML {} skipped. {}", progress.getFields().getFieldValue("path"), e.getCause(), e);
			// Leave a trace, but don't overwrite text_error
			progress.getFields().addField("flag", FLAG_XML + "error");
		}
	}
	
	private IndexingDoc cloneItemFields(IndexingDoc fields) {
		// TODO: Do copy of selective fields instead of clone.
		IndexingDoc doc = fields.deepCopy();
		return doc;
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
