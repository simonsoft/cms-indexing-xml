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
package se.simonsoft.cms.indexing.xml.testconfig;

import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import se.simonsoft.cms.indexing.xml.HandlerXml;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.indexing.xml.XmlIndexWriter;
import se.simonsoft.cms.indexing.xml.custom.IndexFieldExtractionCustomXsl;
import se.simonsoft.cms.indexing.xml.custom.XmlMatchingFieldExtractionSource;
import se.simonsoft.cms.indexing.xml.custom.XmlMatchingFieldExtractionSourceDefault;
import se.simonsoft.cms.indexing.xml.fields.IndexFieldDeletionsToSaveSpace;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldElement;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldExtractionChecksum;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexIdAppendTreeLocation;
import se.simonsoft.cms.indexing.xml.solr.XmlIndexWriterSolrj;
import se.simonsoft.cms.xmlsource.handler.XmlSourceReader;
import se.simonsoft.cms.xmlsource.handler.jdom.XmlSourceReaderJdom;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class IndexingConfigXml extends AbstractModule {

	@Override
	protected void configure() {

		bind(XmlIndexWriter.class).to(XmlIndexWriterSolrj.class);		
		
		// Indexing fields extraction, in order.
		// A service dependency framework could be added if maintaining order here is difficult.
		Multibinder<XmlIndexFieldExtraction> fieldExtraction = Multibinder.newSetBinder(binder(), XmlIndexFieldExtraction.class);
		// Note that the basic xml element fields, source etc, are currently embedded in the solrj handler (above)
		fieldExtraction.addBinding().to(XmlIndexIdAppendTreeLocation.class);
		fieldExtraction.addBinding().to(XmlIndexFieldElement.class);
		// Saxon based text and word count extraction
		fieldExtraction.addBinding().to(IndexFieldExtractionCustomXsl.class);
		// We don't have a strategy yet for placement of the custom xsl, read from jar
		bind(IndexFieldExtractionCustomXsl.class).toInstance(new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSourceDefault()));
		// Checksums of text and source fields (default settings)
		fieldExtraction.addBinding().to(XmlIndexFieldExtractionChecksum.class);
		fieldExtraction.addBinding().to(IndexFieldDeletionsToSaveSpace.class);
		
		// XML services, also used for pretranslate
		bind(XmlSourceReader.class).to(XmlSourceReaderJdom.class);
		
		// The starting point
		bind(HandlerXml.class);
	}

}
