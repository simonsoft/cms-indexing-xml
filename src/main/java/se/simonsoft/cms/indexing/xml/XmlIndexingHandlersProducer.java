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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.s9api.Processor;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.config.IndexingHandlersProducer;
import se.repos.indexing.item.HandlerChecksum;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.repos.indexing.item.ItemPropertiesBufferStrategy;
import se.repos.indexing.solrj.HandlerSendIncrementalSolrjRepositem;
import se.simonsoft.cms.indexing.abx.HandlerAbxBaseLogicalId;
import se.simonsoft.cms.indexing.abx.HandlerAbxDependencies;
import se.simonsoft.cms.indexing.abx.HandlerAbxMasters;
import se.simonsoft.cms.indexing.abx.HandlerLogicalIdFromUrl;
import se.simonsoft.cms.indexing.abx.HandlerPathareaFromProperties;
import se.simonsoft.cms.indexing.abx.HandlerReleaseLabel;
import se.simonsoft.cms.indexing.abx.HandlerXmlMasters;
import se.simonsoft.cms.indexing.abx.HandlerXmlReferences;
import se.simonsoft.cms.indexing.xml.fields.IndexFieldDeletionsToSaveSpace;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldElement;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldXslPipeline;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexReleaseReuseChecksum;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexRidDuplicateDetection;
import se.simonsoft.cms.indexing.xml.solr.XmlIndexWriterSolrjBackground;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.cms.item.inspection.CmsChangesetReader;
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

public class XmlIndexingHandlersProducer {

	static final String CONFIG_MAX_FILESIZE = "se.simonsoft.cms.indexing.xml.maxFilesize";
	static final String CONFIG_SUPPRESS_RID_BEFORE = "se.simonsoft.cms.indexing.xml.suppressRidBefore";
	static final String CONFIG_TSOURCE_ALLOWED = "se.simonsoft.cms.indexing.xml.tsourceAllowed";

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
		Map<String, String> stylesheets = new LinkedHashMap<>();
		stylesheets.put("identity.xsl", "se/simonsoft/cms/xmlsource/transform/identity.xsl");
		stylesheets.put("reuse-normalize.xsl", "se/simonsoft/cms/xmlsource/transform/reuse-normalize.xsl");
		stylesheets.put("itemid-normalize.xsl", "se/simonsoft/cms/xmlsource/transform/itemid-normalize.xsl");
		stylesheets.put("xml-indexing-repositem.xsl", "se/simonsoft/cms/indexing/xml/source/xml-indexing-repositem.xsl");
		stylesheets.put("xml-indexing-reposxml.xsl", "se/simonsoft/cms/indexing/xml/source/xml-indexing-reposxml.xsl");
		TransformStylesheetSource stylesheetSource = new TransformStylesheetSourceConfig(stylesheets);
		return new TransformerServiceFactory(processor, sourceReader, stylesheetSource);
	}

	@Produces
	@Named("se.simonsoft.cms.indexing.xml.maxFilesize")
	public Integer createMaxFilesize(
			@ConfigProperty(name = CONFIG_MAX_FILESIZE, defaultValue = "10485760") int maxFilesize) {
		return maxFilesize;
	}

	@Produces
	@Named("se.simonsoft.cms.indexing.xml.suppressRidBefore")
	public String createSuppressRidBefore(
			@ConfigProperty(name = CONFIG_SUPPRESS_RID_BEFORE) Optional<String> suppressRidBefore) {
		return suppressRidBefore.orElse("");
	}

	@Produces
	@Named("se.simonsoft.cms.indexing.xml.tsourceAllowed")
	public String createTsourceAllowed(
			@ConfigProperty(name = CONFIG_TSOURCE_ALLOWED, defaultValue = "tsp") String tsourceAllowed) {
		return tsourceAllowed;
	}

	@Produces
	@Dependent
	public XmlIndexWriter createXmlIndexWriter(@Named("reposxml") SolrClient reposxml) {
		return new XmlIndexWriterSolrjBackground(reposxml);
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
	@Alternative
	@Priority(1)
	public Set<IndexingItemHandler> createIndexingItemHandlers(
			IdStrategy idStrategy,
			@Named("repositem") SolrClient repositem,
			@Named("reposxml") SolrClient reposxml,
			CmsRepository repository,
			CmsChangesetReader changesetReader,
			ItemContentBufferStrategy contentBufferStrategy,
			ItemPropertiesBufferStrategy propertiesBufferStrategy,
			HandlerXml handlerXml) {
		Set<IndexingItemHandler> standard = new IndexingHandlersProducer().createIndexingItemHandlers(
				idStrategy, repositem, repository, contentBufferStrategy, propertiesBufferStrategy);
		Set<IndexingItemHandler> handlers = new LinkedHashSet<>();
		boolean abxAdded = false;
		boolean xmlAdded = false;
		for (IndexingItemHandler handler : standard) {
			if (!abxAdded && handler.getClass().equals(HandlerSendIncrementalSolrjRepositem.class)) {
				handlers.add(new HandlerLogicalIdFromUrl());
				handlers.add(new HandlerAbxBaseLogicalId());
				handlers.add(new HandlerAbxDependencies(idStrategy));
				HandlerAbxMasters abxMasters = new HandlerAbxMasters(idStrategy);
				abxMasters.setCmsChangesetReader(changesetReader);
				handlers.add(abxMasters);
				handlers.add(new HandlerReleaseLabel());
				handlers.add(new HandlerPathareaFromProperties());
				abxAdded = true;
			}
			if (!xmlAdded && handler.getClass().equals(HandlerChecksum.class)) {
				handlers.add(handlerXml);
				handlers.add(new HandlerXmlReferences(idStrategy));
				handlers.add(new HandlerXmlMasters(idStrategy));
				xmlAdded = true;
			}
			handlers.add(handler);
		}
		if (!abxAdded) {
			throw new IllegalStateException("Could not place ABX handlers in handler chain");
		}
		if (!xmlAdded) {
			throw new IllegalStateException("Could not place " + HandlerXml.class.getSimpleName() + " in handler chain");
		}
		handlers.add(new MarkerXmlCommit(reposxml));
		return handlers;
	}
}
