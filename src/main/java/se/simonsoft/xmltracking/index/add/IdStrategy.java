package se.simonsoft.xmltracking.index.add;

import se.simonsoft.xmltracking.source.XmlSourceElement;

public interface IdStrategy {

	/**
	 * Signals that a new document is started.
	 * 
	 * If we don't need to reuse implementations this method can be scrapped.
	 * 
	 * At this point there should be enough information to produce a
	 * full ID prefix, with only element id within document to be appended.
	 * 
	 * Impl expected to get document identification by some other means,
	 * constructor or event handlers.
	 */
	void start();
	
	/**
	 * Solr id strategy for elements.
	 * 
	 * TODO Per document and repository deletions should be possible.
	 * 
	 * @param element From current document, in order
	 * @return Solr document id for the element
	 */
	String getElementId(XmlSourceElement element);
	
}
