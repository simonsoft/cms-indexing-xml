package se.simonsoft.cms.indexing.xml;

import java.util.LinkedList;

import se.repos.indexing.IndexingHandlers;
import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.repository.HandlerContentDisable;
import se.repos.indexing.repository.MarkerRevisionComplete;
import se.repos.indexing.solrj.HandlerSendSolrjRepositem;
import se.repos.indexing.solrj.MarkerCommitSolrjRepositem;
import se.simonsoft.cms.indexing.abx.IndexingItemHandlerAbxDependencies;
import se.simonsoft.cms.indexing.abx.IndexingItemHandlerAreaFromProperties;
import se.simonsoft.cms.indexing.xml.fields.IndexFieldDeletionsToSaveSpace;
import se.simonsoft.cms.indexing.xml.fields.IndexReuseJoinFields;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldElement;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldExtractionChecksum;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexIdAppendTreeLocation;
import se.simonsoft.xmltracking.source.saxon.IndexFieldExtractionCustomXsl;
import se.simonsoft.xmltracking.source.saxon.XmlMatchingFieldExtractionSourceDefault;

/**
 * Adds XML indexing handlers to those defined by {@link IndexingHandlers}.
 */
public abstract class IndexingHandlersXml {

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
				IndexingItemHandlerAbxDependencies.class,
				IndexingItemHandlerAreaFromProperties.class);
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
