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
import se.simonsoft.cms.indexing.abx.HandlerLogicalIdFromProperty;
import se.simonsoft.cms.indexing.abx.HandlerPathareaFromProperties;
import se.simonsoft.cms.indexing.xml.custom.IndexFieldExtractionCustomXsl;
import se.simonsoft.cms.indexing.xml.fields.IndexFieldDeletionsToSaveSpace;
import se.simonsoft.cms.indexing.xml.fields.IndexReuseJoinFields;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldElement;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldExtractionChecksum;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexIdAppendTreeLocation;

/**
 * Adds XML indexing handlers to those defined by {@link IndexingHandlers}.
 */
public abstract class IndexingHandlersXml {

	/*
		// Indexing fields extraction, in order.
		// A service dependency framework could be added if maintaining order here is difficult.
		Multibinder<XmlIndexFieldExtraction> fieldExtraction = Multibinder.newSetBinder(binder(), XmlIndexFieldExtraction.class);
		fieldExtraction.addBinding().to(XmlIndexIdAppendTreeLocation.class);
		fieldExtraction.addBinding().to(XmlIndexFieldElement.class);
		// Saxon based text and word count extraction
		fieldExtraction.addBinding().to(IndexFieldExtractionCustomXsl.class);
		// We don't have a strategy yet for placement of the custom xsl, read from jar
		bind(IndexFieldExtractionCustomXsl.class).toInstance(new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
				return new StreamSource(xsl);
			}
		}));
		// Checksums of text and source fields (default settings)
		fieldExtraction.addBinding().to(XmlIndexFieldExtractionChecksum.class);
		fieldExtraction.addBinding().to(IndexFieldDeletionsToSaveSpace.class);
	 */
	
	public static final Iterable<Class<? extends XmlIndexFieldExtraction>> STANDARD_XML_EXTRACTION = new LinkedList<Class<? extends XmlIndexFieldExtraction>>() {
		private static final long serialVersionUID = 1L;
		{
			add(XmlIndexIdAppendTreeLocation.class);
			add(XmlIndexFieldElement.class);
			add(IndexFieldExtractionCustomXsl.class);
			add(XmlIndexFieldExtractionChecksum.class);
			add(IndexReuseJoinFields.class);
			add(IndexFieldDeletionsToSaveSpace.class);
		}
	};
	
	/**
	 * Bind standard handler classes to a guice multibinder, without compile time dependencies
	 * @param guiceMultibinder instance of com.google.inject.multibindings.Multibinder for IndexingItemHandler
	 */
	@SuppressWarnings("unchecked")
	public static void configureFirst(Object guiceMultibinder) {
		IndexingHandlers.configureFirst(guiceMultibinder);
		// high priority, TODO this should be inserted before ScheduleAwaitNewer
		IndexingHandlers.to(guiceMultibinder,
				HandlerLogicalIdFromProperty.class,
				HandlerAbxDependencies.class,
				HandlerPathareaFromProperties.class);
		// slow
		IndexingHandlers.to(guiceMultibinder, HandlerXml.class);
	}

	/**
	 * Bind standard handler classes to a guice multibinder, without compile time dependencies
	 * @param guiceMultibinder instance of com.google.inject.multibindings.Multibinder for IndexingItemHandler
	 */
	@SuppressWarnings("unchecked")
	public static void configureLast(Object guiceMultibinder) {
		IndexingHandlers.to(guiceMultibinder, MarkerXmlCommit.class);
		IndexingHandlers.configureLast(guiceMultibinder);
	}
	
	public static void configureXmlFieldExtraction(Object guiceMultibinderXmlIndexFieldExtraction) {
		IndexingHandlers.toArbitrary(guiceMultibinderXmlIndexFieldExtraction, STANDARD_XML_EXTRACTION);
	}
	
}
