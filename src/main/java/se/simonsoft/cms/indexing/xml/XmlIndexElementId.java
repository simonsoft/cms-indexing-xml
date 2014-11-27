package se.simonsoft.cms.indexing.xml;

import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;

/**
 * Provide a string identifier for each Element in XML source.
 * Suitable as id field in index.
 * 
 * @author takesson
 *
 */
public interface XmlIndexElementId {

	
	public String getXmlElementId(XmlSourceElement processedElement);
}
