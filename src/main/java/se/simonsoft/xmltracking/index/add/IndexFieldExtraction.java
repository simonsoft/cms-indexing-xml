package se.simonsoft.xmltracking.index.add;

import se.simonsoft.xmltracking.source.XmlSourceElement;

/**
 * Services that extract fields from source element or from earlier extracted fields.
 * 
 * A list of these services is for example provided to {@link XmlSourceHandlerSolrj}.
 */
public interface IndexFieldExtraction {

	/**
	 * @param fields to read from and add/overwrite to
	 * @param processedElement original element
	 */
	void extract(IndexFields fields, XmlSourceElement processedElement);
	
}
