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
import javax.inject.Named;

import net.sf.saxon.s9api.Processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import se.repos.indexing.IndexingHandlerException;
import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.HandlerProperties;
import se.repos.indexing.item.IndexingItemProgress;
import se.simonsoft.cms.indexing.xml.custom.HandlerXmlRepositem;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;
import se.simonsoft.cms.xmlsource.handler.XmlSourceHandler;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceDocumentS9api;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceReaderS9api;

public class HandlerXml implements IndexingItemHandler {

	public static final String FLAG_XML = "hasxml";
	
	public final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private XmlFileFilter xmlFileFilter = new XmlFileFilterExtensionAndTikaContentType(); // TODO inject, important customization point
	
	private XmlIndexRestrictFields supportLegacySchema = new XmlIndexRestrictFields(); // TODO do away with gradually
	
	private Processor processor;
	
	private XmlSourceReaderS9api sourceReader;
	
	private Set<XmlIndexFieldExtraction> fieldExtraction = null;

	private XmlIndexWriter indexWriter;
	
	private HandlerXmlRepositem handlerXmlRepositem;
	
	private Integer maxFilesize = null;
	
	
	@Inject
	public HandlerXml(Processor processor, XmlSourceReaderS9api sourceReader) {
		
		this.processor = processor;
		this.sourceReader = sourceReader;
		
		this.handlerXmlRepositem = new HandlerXmlRepositem(this.processor);
	}
	
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
	
	@Inject
	public void setConfigIndexing(
			@Named("se.simonsoft.cms.indexing.xml.maxFilesize") Integer maxFilesize) {
		
		this.maxFilesize = maxFilesize;
	}
	
	@Override
	public void handle(IndexingItemProgress progress) {
		CmsChangesetItem c = progress.getItem();
		
		if (c.isFile()) {
			if (xmlFileFilter.isXml(c, progress.getFields())) {
				logger.trace("Changeset content update item {} found", c);
				if (c.isDelete()) {
					indexWriter.deletePath(progress.getRepository(), c);
					boolean expunge = true;
					logger.info("Performing commit (expunge: {}) of deleted changeset item: {}", expunge, c);
					this.commit(expunge);
				} else {
					indexWriter.deletePath(progress.getRepository(), c);
					
					// Determine if the XML file is too large.
					if (maxFilesize != null && c.getFilesize() > maxFilesize) {
						String msg = MessageFormatter.format("Deferring XML extraction when file size {} gt {}: " + c, c.getFilesize(), maxFilesize).getMessage();
						throw new IndexingHandlerException(msg);
					}
					
					try {
						index(progress);
						// Doing intermediate commit of each XML file to manage solr core growth during huge changesets.
						// This will cause files in reposxml to be replaced one-by-one instead of whole commit.
						boolean expunge = true;
						logger.info("Performing commit (expunge: {}) of changeset item: {}", expunge, c);
						this.commit(expunge);
					} catch (IndexingHandlerException ex) {
						// We should ideally revert the index if indexing of the file fails (does Solr have revert?)
						logger.warn("Failed to perform XML extraction of {}: {}", c, ex.getMessage());
						indexWriter.deletePath(progress.getRepository(), c);
						this.commit(true);
						// The message/stacktrace in exception will be logged in repositem.
						throw ex;
					}
					
					
				}
			} else {
				logger.trace("Ignoring content update item {}, not an XML candidate file type", c);
			}
		} else {
			logger.trace("Ignoring changeset item {}, not a file", c);
		}
	}
	
	private void commit(boolean expunge) {
		
		try {
			logger.debug("Commit first attempt (expunge: {})", expunge);
			indexWriter.commit(expunge);
			logger.info("Commit first attempt successful");
		} catch (Exception e) {
			long pause = 10000;
			logger.warn("Commit first attempt failed, retry in {} ms", pause, e);
			try {
				Thread.sleep(pause);
			} catch (InterruptedException e1) {
				throw new RuntimeException("Recovery sleep after failed indexing commit attempt interrupted: " +  e.getMessage());
			}
			
			logger.info("Commit second attempt (expunge: {})", expunge);
			indexWriter.commit(expunge);
			logger.info("Commit second attempt successful");
		}
	}

	protected void index(IndexingItemProgress progress) {
		
		if (sourceReader == null) {
			throw new IllegalStateException("No XmlSourceHandler has been provided.");
		}
		
		boolean indexReposxml = true;
		CmsChangesetItem c = progress.getItem();
		if (c.isOverwritten()) {
			logger.debug("Suppressing reposxml indexing of later overwritten {} at {}", c.getPath(), progress.getRevision());
			indexReposxml = false;
		}
		if (progress.getFields().containsKey(HandlerXmlRepositem.STATUS_FIELD_NAME) && "Obsolete".equals(progress.getFields().getFieldValue(HandlerXmlRepositem.STATUS_FIELD_NAME))) {
			logger.info("Suppressing reposxml indexing of 'Obsolete' item: {}", progress.getItem());
			indexReposxml = false;
		}

		XmlIndexAddSession docHandler = indexWriter.get();
		try {
			XmlSourceDocumentS9api xmlDoc = sourceReader.read(progress.getContents());
			// Perform repositem extraction.
			handlerXmlRepositem.handle(progress, xmlDoc);
			
			if (indexReposxml) {
				// Clone the repositem document selectively. Used as base for creating one clone per element.
				IndexingDoc itemDoc = cloneItemFields(progress.getFields());
				XmlIndexProgress xmlProgress = new XmlIndexProgress(progress.getRepository(), itemDoc);
				XmlSourceHandler sourceHandler = new XmlSourceHandlerFieldExtractors(xmlProgress, fieldExtraction, docHandler);
			
				sourceReader.handle(xmlDoc, sourceHandler);
				// success, flag this
				progress.getFields().addField("flag", FLAG_XML);
			}
		} catch (XmlNotWellFormedException e) { 
			// failure, flag with error
			progress.getFields().addField("flag", FLAG_XML + "error");
			String msg = MessageFormatter.format("Invalid XML {} skipped. {}", progress.getFields().getFieldValue("path"), e.getCause()).getMessage();
			logger.error(msg, e);
			throw new IndexingHandlerException(msg, e);
		}
	}
	
	private IndexingDoc cloneItemFields(IndexingDoc fields) {
		/*
		IndexingDoc doc = fields.deepCopy();
		supportLegacySchema.handle(doc);
		*/
		// Now doing copy of selective fields instead of clone.
		// Not really a deep copy of the values. Assumes that the values are immutable objects.
		// TODO: A better way might be to implement an IndexingDoc impl that can keep a read-only set of fields in another IndexingDoc.
		IndexingDoc doc = supportLegacySchema.clone(fields);
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
