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

import java.util.Collection;
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
	public static final String FLAG_XML_REPOSITEM = "hasxmlrepositem";
	public static final String FLAG_XML_ERROR = "hasxmlerror";
	public static final String FLAG_XML_COMMIT = "hasxmlcommit"; // Indicate the need to commit reposxml schema.
	
	public final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private XmlFileFilter xmlFileFilter = new XmlFileFilterExtensionAndTikaContentType(); // TODO inject, important customization point
	
	private XmlIndexRestrictFields supportLegacySchema = new XmlIndexRestrictFields(); // TODO do away with gradually
	
	private Processor processor;
	
	private XmlSourceReaderS9api sourceReader;
	
	private Set<XmlIndexFieldExtraction> fieldExtraction = null;

	private XmlIndexWriter indexWriter;
	
	private HandlerXmlRepositem handlerXmlRepositem;
	
	private Integer maxFilesize = null;
	private String suppressRidBefore = null;
	
	
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
					logger.info("Performing commit (expunge: {}) of deleted changeset item: {}", expunge, c);
					this.commitWithRetry(expunge); // Best effort with retry.
				} else if (c.getFilesize() == 0) {
					logger.info("Deferring XML extraction when file is empty: {}", c);
					if (!c.isAdd()) {
						// Rare situation: iterating an empty file or making it empty.
						indexWriter.deletePath(progress.getRepository(), c);
						this.commitWithRetry(true); // Best effort with retry.
					}
				} else {
					boolean expunge = true;
					if (!c.isAdd()) {
						indexWriter.deletePath(progress.getRepository(), c);
					}
					
					// Determine if the XML file is too large.
					if (maxFilesize != null && c.getFilesize() > maxFilesize) {
						String msg = MessageFormatter.format("Deferring XML extraction when file size {} gt {}: " + c, c.getFilesize(), maxFilesize).getMessage();
						throw new IndexingHandlerException(msg);
					}
					if (c.isAdd() || (maxFilesize != null && (2*c.getFilesize()) < maxFilesize)) {
						// Expunge only if the file is larger than half of maxFilesize (reposxml).
						expunge = false;
					}
					
					try {
						index(progress);
						// Doing intermediate commit of each XML file to manage solr core growth during huge changesets.
						// This will cause files in reposxml to be replaced one-by-one instead of whole commit.
						// TODO: Consider removing this per-item commit if reposxml size is significantly smaller after refactoring Translations into one SolR doc.

						Collection<Object> flags = progress.getFields().getFieldValues("flag");
						if (flags.contains(FLAG_XML_COMMIT)) {
							// Commit reposxml only if extraction has flagged the need to commit.
							// Can be moved to a Marker implementation if changing to per-revision reposxml commit.							
							logger.info("Performing commit (expunge: {}) of changeset item: {}", expunge, c);
							this.commit(expunge); // No retry to ensure that a failure is noticed (SolR restart btw commit attempts).
						}
						
					} catch (IndexingHandlerException ex) {
						// We should ideally revert the index if indexing of the file fails (does Solr have revert?)
						logger.warn("Failed to perform XML extraction of {}: {}", c, ex.getMessage());
						indexWriter.deletePath(progress.getRepository(), c);
						this.commitWithRetry(true); // Best effort with retry.
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
	
	private void commitWithRetry(boolean expunge) {
		
		try {
			logger.debug("Commit reposxml first attempt (expunge: {})", expunge);
			indexWriter.commit(expunge);
			logger.info("Commit reposxml first attempt successful");
		} catch (Exception e) {
			long pause = 10000;
			// TODO: #1346 This retry can result in incomplete index if SolR restarts btw attempts.
			logger.warn("Commit reposxml first attempt failed, retry in {} ms", pause, e);
			try {
				Thread.sleep(pause);
			} catch (InterruptedException e1) {
				throw new RuntimeException("Recovery sleep after failed indexing commit attempt interrupted: " +  e.getMessage());
			}
			
			logger.info("Commit reposxml second attempt (expunge: {})", expunge);
			indexWriter.commit(expunge);
			logger.info("Commit reposxml second attempt successful");
		}
	}
	
	private void commit(boolean expunge) {
		// #1346: Reverted to a strict single commit approach for now.
		try {
			logger.debug("Commit reposxml (expunge: {})", expunge);
			indexWriter.commit(expunge);
			logger.info("Commit reposxml successful");
		} catch (Exception e) {
			String msg = MessageFormatter.format("Commit reposxml failed: {}", e.getMessage()).getMessage();
			logger.error(msg, e);
			throw new RuntimeException(msg, e);
		}
	}

	protected void index(IndexingItemProgress progress) {
		
		if (sourceReader == null) {
			throw new IllegalStateException("No XmlSourceHandler has been provided.");
		}
		
		boolean indexReposxml = true;
		CmsChangesetItem c = progress.getItem();
		if (c.isOverwritten()) {
			logger.info("Suppressing reposxml indexing of later overwritten {} at {}", c.getPath(), progress.getRevision());
			// Not requesting commit, during reindex there will be nothing to delete.
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
			// Important to flag commit in order to delete the content that was made Obsolete.
			progress.getFields().addField("flag", FLAG_XML_COMMIT);
		}
		// Don't index "Pending_Pretranslate" in reposxml.
		if (progress.getFields().containsKey(HandlerXmlRepositem.STATUS_FIELD_NAME) && "Pending_Pretranslate".equals(progress.getFields().getFieldValue(HandlerXmlRepositem.STATUS_FIELD_NAME))) {
			logger.info("Suppressing reposxml indexing of 'Pending_Pretranslate' item: {}", progress.getItem());
			// Not requesting commit, this was likely an add (nothing to delete) alternatively some experiment (soon next commit anyway).
			indexReposxml = false;
		}
		// Don't index "Pending_Pretranslate_Analysis" in reposxml, only need the repositem content for Analysis.
		if (progress.getFields().containsKey(HandlerXmlRepositem.STATUS_FIELD_NAME) && "Pending_Pretranslate_Analysis".equals(progress.getFields().getFieldValue(HandlerXmlRepositem.STATUS_FIELD_NAME))) {
			logger.info("Suppressing reposxml indexing of 'Pending_Pretranslate' item: {}", progress.getItem());
			// Not requesting commit, this will always be followed by another commit.
			// Might be important to commit the delete when re-Pretranslating but committing each of 20 Translations can be 1min.
			// Must ensure the stability of the Analysis event handler so some status transition is always committed.
			indexReposxml = false;
		}
		

		XmlIndexAddSession docHandler = indexWriter.get();
		try {
			XmlSourceDocumentS9api xmlDoc = sourceReader.read(progress.getContents());
			// Perform repositem extraction.
			handlerXmlRepositem.handle(progress, xmlDoc);
			// Flag that it was indexed in repositem.
			progress.getFields().addField("flag", FLAG_XML_REPOSITEM);
			
			if (indexReposxml) {
				// Clone the repositem document selectively. Used as base for creating one clone per element.
				IndexingDoc itemDoc = cloneItemFields(progress.getFields());
				XmlIndexProgress xmlProgress = new XmlIndexProgress(progress.getRepository(), itemDoc);
				XmlSourceHandler sourceHandler = new XmlSourceHandlerFieldExtractors(xmlProgress, fieldExtraction, docHandler);
			
				sourceReader.handle(xmlDoc, sourceHandler);
				// success, flag this
				progress.getFields().addField("flag", FLAG_XML);
				progress.getFields().addField("flag", FLAG_XML_COMMIT);
			}
		} catch (XmlNotWellFormedException e) { 
			// failure, flag with error
			progress.getFields().addField("flag", FLAG_XML_ERROR);
			String msg = MessageFormatter.format("Invalid XML {} skipped. {}", progress.getFields().getFieldValue("path"), e.getCause()).getMessage();
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
