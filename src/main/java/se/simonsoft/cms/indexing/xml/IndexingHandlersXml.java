package se.simonsoft.cms.indexing.xml;

import se.repos.indexing.IndexingHandlers;

/**
 * Adds XML indexing handlers to those defined by {@link IndexingHandlers}.
 */
public abstract class IndexingHandlersXml {

	/**
	 * Bind standard handler classes to a guice multibinder, without compile time dependencies
	 * @param guiceMultibinder instance of com.google.inject.multibindings.Multibinder for IndexingItemHandler
	 */
	@SuppressWarnings("unchecked")
	public static void configureFirst(Object guiceMultibinder) {
		IndexingHandlers.configureFirst(guiceMultibinder);
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
	
}
