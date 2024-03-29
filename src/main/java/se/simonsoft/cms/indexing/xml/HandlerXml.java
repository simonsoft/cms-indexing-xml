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
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldExtractionSource;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldXslPipeline;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;
import se.simonsoft.cms.xmlsource.handler.XmlSourceHandler;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceDocumentS9api;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceReaderS9api;
import se.simonsoft.cms.xmlsource.transform.TransformOptions;
import se.simonsoft.cms.xmlsource.transform.TransformerService;
import se.simonsoft.cms.xmlsource.transform.TransformerServiceFactory;

public class HandlerXml implements IndexingItemHandler {

	public static final String FLAG_XML = "hasxml";
	public static final String FLAG_XML_REPOSITEM = "hasxmlrepositem";
	public static final String FLAG_XML_ERROR = "hasxmlerror";
	
	public final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private XmlFileFilter xmlFileFilter = new XmlFileFilterExtensionAndTikaContentType(); // TODO inject, important customization point
	
	private XmlIndexRestrictFields supportLegacySchema = new XmlIndexRestrictFields(); // TODO do away with gradually
	
	private Processor processor;
	
	private XmlSourceReaderS9api sourceReader;
	private TransformerService transformerNormalize;
	
	private Set<XmlIndexFieldExtraction> fieldExtraction = null;

	private XmlIndexWriter indexWriter;
	
	private HandlerXmlRepositem handlerXmlRepositem;
	@Inject
	private XmlIndexFieldXslPipeline xslPipeline; // Requesting preprocess XSL by handler.
	
	private Integer maxFilesize = null;
	private String suppressRidBefore = null;
	
	
	@Inject
	public HandlerXml(Processor processor, XmlSourceReaderS9api sourceReader, TransformerServiceFactory transformerServiceFactory) {
		
		this.processor = processor;
		this.sourceReader = sourceReader;
		this.transformerNormalize = transformerServiceFactory.buildTransformerService("reuse-normalize.xsl");
		
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
			@Named("se.simonsoft.cms.indexing.xml.maxFilesize") Integer maxFilesize,
			@Named("se.simonsoft.cms.indexing.xml.suppressRidBefore") String suppressRidBefore
			) {
		
		this.maxFilesize = maxFilesize;
		this.suppressRidBefore = suppressRidBefore;
		
		logger.info("Configured to suppress files with size above: {}", this.maxFilesize);
		if (suppressRidBefore != null && !suppressRidBefore.isEmpty()) {
			logger.info("Configured to suppress reposxml before RID: {}", this.suppressRidBefore);
		}
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
					logger.info("Deleted XML index content for changeset item: {}", expunge, c);
				} else if (c.getFilesize() == 0) {
					logger.info("Deferring XML extraction when file is empty: {}", c);
				} else {
					if (!c.isAdd()) {
						indexWriter.deletePath(progress.getRepository(), c);
					}
					
					// Determine if the XML file is too large.
					if (maxFilesize != null && c.getFilesize() > maxFilesize) {
						String msg = MessageFormatter.format("Deferring XML extraction when file size {} gt {}: " + c, c.getFilesize(), maxFilesize).getMessage();
						throw new IndexingHandlerException(msg);
					}
					
					try {
						index(progress);
						// No longer doing intermediate commit of each XML file.
						// Previously in order to manage solr core growth during huge changesets.
					} catch (IndexingHandlerException ex) {
						// We should ideally revert the index if indexing of the file fails (does Solr have revert?)
						logger.warn("Failed to perform XML extraction of {}: {}", c, ex.getMessage());
						indexWriter.deletePath(progress.getRepository(), c);
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
	
	
	@SuppressWarnings("deprecation")
	private TransformOptions getTransformOptionsNormalize() {
		TransformOptions options = new TransformOptions();
		options.setParameter("source-reuse-tags-param", "*");
		// Limiting for large elements, previously done in Java handler.
		options.setParameter("source-reuse-max-chars", XmlIndexFieldExtractionSource.MAX_CHARACTERS_SOURCE);
		return options;
	}

	protected void index(IndexingItemProgress progress) {
		
		TransformOptions options = getTransformOptionsNormalize();
		
		if (sourceReader == null) {
			throw new IllegalStateException("No XmlSourceHandler has been provided.");
		}
		
		boolean indexReposxml = true;
		CmsChangesetItem c = progress.getItem();
		if (c.isOverwritten()) {
			logger.info("Suppressing reposxml indexing of later overwritten {} at {}", c.getPath(), progress.getRevision());
			indexReposxml = false;
		}
		// Don't index in reposxml if Finalized before configured timestamp (RID).
		if (suppressRidBefore != null && !suppressRidBefore.isEmpty() && progress.getFields().containsKey(HandlerXmlRepositem.RID_PROP_FIELD_NAME) && suppressRidBefore.compareTo((String) progress.getFields().getFieldValue(HandlerXmlRepositem.RID_PROP_FIELD_NAME)) > 0) {
			logger.info("Suppressing reposxml indexing of item finalized before {}: {}", suppressRidBefore, progress.getItem());
			indexReposxml = false;
		}
		// Don't index in reposxml if Translation was prepared with old CMS (2.0-3.?) and there is a configured timestamp (RID).
		if (suppressRidBefore != null && !suppressRidBefore.isEmpty() && progress.getFields().containsKey(HandlerXmlRepositem.TPROJECT_PROP_FIELD_NAME) && !progress.getFields().containsKey(HandlerXmlRepositem.RID_PROP_FIELD_NAME)) {
			logger.info("Suppressing reposxml indexing of Translation without RID property: {}", progress.getItem());
			indexReposxml = false;
		}
		
		// Don't index "Obsolete" in reposxml.
		if (progress.getFields().containsKey(HandlerXmlRepositem.STATUS_FIELD_NAME) && "Obsolete".equals(progress.getFields().getFieldValue(HandlerXmlRepositem.STATUS_FIELD_NAME))) {
			logger.info("Suppressing reposxml indexing of 'Obsolete' item: {}", progress.getItem());
			indexReposxml = false;
		}
		// Don't index "Pending_Pretranslate" in reposxml.
		if (progress.getFields().containsKey(HandlerXmlRepositem.STATUS_FIELD_NAME) && "Pending_Pretranslate".equals(progress.getFields().getFieldValue(HandlerXmlRepositem.STATUS_FIELD_NAME))) {
			logger.info("Suppressing reposxml indexing of 'Pending_Pretranslate' item: {}", progress.getItem());
			indexReposxml = false;
		}
		// Don't index "Pending_Pretranslate_Analysis" in reposxml, only need the repositem content for Analysis.
		if (progress.getFields().containsKey(HandlerXmlRepositem.STATUS_FIELD_NAME) && "Pending_Pretranslate_Analysis".equals(progress.getFields().getFieldValue(HandlerXmlRepositem.STATUS_FIELD_NAME))) {
			logger.info("Suppressing reposxml indexing of 'Pending_Pretranslate' item: {}", progress.getItem());
			indexReposxml = false;
		}
		

		XmlIndexAddSession docHandler = indexWriter.get();
		try {
			// Performing repositem extraction based on non-transformed XML (preserves DOCTYPE).
			XmlSourceDocumentS9api xmlDoc = sourceReader.read(progress.getContents());
			// Perform repositem extraction.
			handlerXmlRepositem.handle(progress, xmlDoc);
			
			if (indexReposxml) {
				// Calculate source_reuse.
				// Suppress source_reuse for Translations (depth = 1).
				Integer depth = XmlIndexFieldExtraction.getDepthReposxml(progress.getFields());
				if (depth == null) { // Depth is non-null for Translations (gets source_reuse from the Release instead)
					xmlDoc = transformerNormalize.transform(xmlDoc, options);
				} else {
					// The normal path for Translations since CMS 5.0.
					logger.debug("Suppress normalize transform (depth: {}): {}", depth, progress.getItem());
				}
				// Next XSL in pipeline, specific to reposxml.
				xmlDoc = xslPipeline.doTransformPipeline(xmlDoc, progress.getFields());
				
				// Clone the repositem document selectively. Used as base for creating one clone per element.
				IndexingDoc itemDoc = cloneItemFields(progress.getFields());
				XmlIndexProgress xmlProgress = new XmlIndexProgress(progress.getRepository(), itemDoc);
				XmlSourceHandler sourceHandler = new XmlSourceHandlerFieldExtractors(xmlProgress, fieldExtraction, docHandler);
			
				sourceReader.handle(xmlDoc, sourceHandler);
				// success, flag this
				progress.getFields().addField("flag", FLAG_XML);
			}

			// Flag that it was indexed in repositem.
			progress.getFields().addField("flag", FLAG_XML_REPOSITEM);
			
		} catch (IndexingHandlerException e) {
			// Already handled exception, improve error in index.
			logger.error("IndexingHandlerException for {}: {}",  progress.getFields().getFieldValue("path"), e.getMessage());
			throw e;
		
		// TODO: Ensure that Transformer framework figures this out and throws XmlNotWellFormedException.
		} catch (XmlNotWellFormedException e) { 
			// failure, flag with error
			progress.getFields().addField("flag", FLAG_XML_ERROR);
			String msg = MessageFormatter.format("Invalid XML {} skipped. {}", progress.getFields().getFieldValue("path"), e.getCause()).getMessage();
			logger.error(msg); // Suppress stack trace for normal log levels.
			logger.debug(msg, e);
			throw new IndexingHandlerException(msg, e);
		} catch (RuntimeException e) { 
			// failure, flag with error
			progress.getFields().addField("flag", FLAG_XML_ERROR);
			String msg = MessageFormatter.format("Unexpected XML error {} skipped. {}", progress.getFields().getFieldValue("path"), e.getMessage()).getMessage();
			logger.error(msg, e);
			throw new IndexingHandlerException(msg, e);
		}
		// TODO: Should we catch other forms of errors, from XSL?
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
