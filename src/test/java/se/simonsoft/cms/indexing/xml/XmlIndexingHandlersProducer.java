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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;

import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.s9api.Processor;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.config.IndexingHandlersProducer;
import se.repos.indexing.item.HandlerChecksum;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.repos.indexing.item.ItemPropertiesBufferStrategy;
import se.simonsoft.cms.indexing.xml.fields.IndexFieldDeletionsToSaveSpace;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldElement;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldXslPipeline;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexReleaseReuseChecksum;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexRidDuplicateDetection;
import se.simonsoft.cms.indexing.xml.solr.XmlIndexWriterSolrj;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.cms.xmlsource.SaxonConfiguration;
import se.simonsoft.cms.xmlsource.handler.XmlSourceReader;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceReaderS9api;
import se.simonsoft.cms.xmlsource.transform.TransformStylesheetSource;
import se.simonsoft.cms.xmlsource.transform.TransformStylesheetSourceConfig;
import se.simonsoft.cms.xmlsource.transform.TransformerServiceFactory;
import se.simonsoft.cms.xmlsource.transform.function.GetChecksum;
import se.simonsoft.cms.xmlsource.transform.function.GetLogicalId;
import se.simonsoft.cms.xmlsource.transform.function.GetPegRev;
import se.simonsoft.cms.xmlsource.transform.function.WithPegRev;

@Alternative
@Priority(1)
public class XmlIndexingHandlersProducer {

	@Produces
	@Singleton
	public Processor createProcessor() {
		Set<ExtensionFunctionDefinition> functions = new LinkedHashSet<>();
		functions.add(new GetChecksum());
		functions.add(new GetPegRev());
		functions.add(new WithPegRev());
		functions.add(new GetLogicalId());
		return new SaxonConfiguration(functions).get();
	}

	@Produces
	@Singleton
	public XmlSourceReaderS9api createXmlSourceReader(Processor processor) {
		return new XmlSourceReaderS9api(processor);
	}

	@Produces
	@Singleton
	public TransformerServiceFactory createTransformerServiceFactory(
			Processor processor, XmlSourceReader sourceReader) {
		Map<String, String> stylesheets = TransformerServiceFactory.getStylesheetsForTestingMap();
		stylesheets.put("xml-indexing-repositem.xsl", "se/simonsoft/cms/indexing/xml/source/xml-indexing-repositem.xsl");
		stylesheets.put("xml-indexing-reposxml.xsl", "se/simonsoft/cms/indexing/xml/source/xml-indexing-reposxml.xsl");
		TransformStylesheetSource stylesheetSource = new TransformStylesheetSourceConfig(stylesheets);
		return new TransformerServiceFactory(processor, sourceReader, stylesheetSource);
	}

	@Produces
	@Named("se.simonsoft.cms.indexing.xml.maxFilesize")
	public Integer createMaxFilesize() {
		return 10 * 1048576;
	}

	@Produces
	@Named("se.simonsoft.cms.indexing.xml.suppressRidBefore")
	public String createSuppressRidBefore() {
		return "";
	}

	@Produces
	@Named("se.simonsoft.cms.indexing.xml.tsourceAllowed")
	public String createTsourceAllowed() {
		return "tsp";
	}

	@Produces
	@Dependent
	public XmlIndexWriter createXmlIndexWriter(@Named("reposxml") SolrClient reposxml) {
		return new XmlIndexWriterSolrj(reposxml);
	}

	@Produces
	@Dependent
	public XmlIndexFieldXslPipeline createXmlIndexFieldXslPipeline(
			TransformerServiceFactory transformerServiceFactory,
			@Named("se.simonsoft.cms.indexing.xml.tsourceAllowed") String tsourceAllowed) {
		return new XmlIndexFieldXslPipeline(transformerServiceFactory, tsourceAllowed);
	}

	@Produces
	@Dependent
	public Set<XmlIndexFieldExtraction> createXmlIndexFieldExtractions(
			XmlIndexFieldXslPipeline xslPipeline,
			XmlSourceReader sourceReader,
			TransformerServiceFactory transformerServiceFactory,
			ItemContentBufferStrategy contentBufferStrategy) {
		XmlIndexReleaseReuseChecksum releaseReuseChecksum =
				new XmlIndexReleaseReuseChecksum(sourceReader, transformerServiceFactory);
		releaseReuseChecksum.setItemContentBufferStrategy(contentBufferStrategy);

		Set<XmlIndexFieldExtraction> fieldExtractions = new LinkedHashSet<>();
		fieldExtractions.add(new XmlIndexFieldElement());
		fieldExtractions.add(xslPipeline);
		fieldExtractions.add(new XmlIndexRidDuplicateDetection());
		fieldExtractions.add(new IndexFieldDeletionsToSaveSpace());
		fieldExtractions.add(releaseReuseChecksum);
		return fieldExtractions;
	}

	@Produces
	@RequestScoped
	public Set<IndexingItemHandler> createIndexingItemHandlers(
			IdStrategy idStrategy,
			@Named("repositem") SolrClient repositem,
			@Named("reposxml") SolrClient reposxml,
			CmsRepository repository,
			ItemContentBufferStrategy contentBufferStrategy,
			ItemPropertiesBufferStrategy propertiesBufferStrategy,
			HandlerXml handlerXml) {
		Set<IndexingItemHandler> standard = new IndexingHandlersProducer().createIndexingItemHandlers(
				idStrategy, repositem, repository, contentBufferStrategy, propertiesBufferStrategy);
		Set<IndexingItemHandler> handlers = new LinkedHashSet<>();
		boolean xmlAdded = false;
		for (IndexingItemHandler handler : standard) {
			if (!xmlAdded && handler.getClass().equals(HandlerChecksum.class)) {
				handlers.add(handlerXml);
				xmlAdded = true;
			}
			handlers.add(handler);
		}
		if (!xmlAdded) {
			throw new IllegalStateException("Could not place " + HandlerXml.class.getSimpleName() + " in handler chain");
		}
		handlers.add(new MarkerXmlCommit(reposxml));
		return handlers;
	}
}
