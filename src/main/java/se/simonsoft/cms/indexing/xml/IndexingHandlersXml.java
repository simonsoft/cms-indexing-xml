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

import java.util.LinkedList;

import se.repos.indexing.IndexingHandlers;
import se.simonsoft.cms.indexing.abx.HandlerAbxDependencies;
import se.simonsoft.cms.indexing.abx.HandlerAbxMasters;
import se.simonsoft.cms.indexing.abx.HandlerLogicalIdFromUrl;
import se.simonsoft.cms.indexing.abx.HandlerPathareaFromProperties;
import se.simonsoft.cms.indexing.xml.custom.IndexFieldExtractionCustomXsl;
import se.simonsoft.cms.indexing.xml.fields.IndexFieldDeletionsToSaveSpace;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexContentReferences;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldElement;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldExtractionChecksum;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldExtractionSource;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexReleaseReuseChecksum;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexRidDuplicateDetection;

/**
 * Adds XML indexing handlers to those defined by {@link IndexingHandlers}.
 */
public abstract class IndexingHandlersXml {
	
	/**
	 * Naming may be confusing, but these are the handlers inside XML extraction.
	 */
	public static final Iterable<Class<? extends XmlIndexFieldExtraction>> STANDARD_XML_EXTRACTION = new LinkedList<Class<? extends XmlIndexFieldExtraction>>() {
		private static final long serialVersionUID = 1L;
		{
			// ID generation is no longer done in a normal handler.
			add(XmlIndexFieldElement.class);
			// Saxon based text and word count extraction
			add(IndexFieldExtractionCustomXsl.class);
			// Checksums of text and source fields (default settings)
			add(XmlIndexFieldExtractionChecksum.class);
			// The special Join-fields were not used in the Pretranslate algorithm.
			// add(IndexReuseJoinFields.class);
			// Detect duplication of RIDs for document root element.
			add(XmlIndexRidDuplicateDetection.class);
			// Remove some fields that are not needed on each XML element.
			add(IndexFieldDeletionsToSaveSpace.class);
			// References to all child elements
			// TODO: Re-enable content references.
			//add(XmlIndexContentReferences.class);
			// Source is now a separate handler, must be after IndexFieldExtractionCustomXsl.
			add(XmlIndexFieldExtractionSource.class);
			// Get checksum from the Release
			add(XmlIndexReleaseReuseChecksum.class);
		}
	};
	
	/**
	 * Bind standard handler classes to a guice multibinder, without compile time dependencies
	 * @param guiceMultibinder instance of com.google.inject.multibindings.Multibinder for IndexingItemHandler
	 */
	@SuppressWarnings("unchecked")
	public static void configureFirst(Object guiceMultibinder) {
		IndexingHandlers.to(guiceMultibinder, IndexingHandlers.STANDARD.get(IndexingHandlers.Group.Unblock));
		IndexingHandlers.to(guiceMultibinder, IndexingHandlers.STANDARD.get(IndexingHandlers.Group.Structure));
		IndexingHandlers.to(guiceMultibinder, IndexingHandlers.STANDARD.get(IndexingHandlers.Group.Fast));
		IndexingHandlers.to(guiceMultibinder,
				HandlerLogicalIdFromUrl.class,
				HandlerAbxDependencies.class,
				HandlerAbxMasters.class,
				HandlerPathareaFromProperties.class);
		IndexingHandlers.to(guiceMultibinder, IndexingHandlers.STANDARD.get(IndexingHandlers.Group.Nice));
		IndexingHandlers.to(guiceMultibinder, HandlerXml.class);
		IndexingHandlers.to(guiceMultibinder, IndexingHandlers.STANDARD.get(IndexingHandlers.Group.Content));
	}

	/**
	 * Bind standard handler classes to a guice multibinder, without compile time dependencies
	 * @param guiceMultibinder instance of com.google.inject.multibindings.Multibinder for IndexingItemHandler
	 */
	@SuppressWarnings("unchecked")
	public static void configureLast(Object guiceMultibinder) {
		IndexingHandlers.to(guiceMultibinder, IndexingHandlers.STANDARD.get(IndexingHandlers.Group.Final));
		IndexingHandlers.to(guiceMultibinder, MarkerXmlCommit.class);
	}
	
	public static void configureXmlFieldExtraction(Object guiceMultibinderXmlIndexFieldExtraction) {
		IndexingHandlers.toArbitrary(guiceMultibinderXmlIndexFieldExtraction, STANDARD_XML_EXTRACTION);
	}
	
}
