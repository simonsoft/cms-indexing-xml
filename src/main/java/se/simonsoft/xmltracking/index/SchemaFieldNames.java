package se.simonsoft.xmltracking.index;

public interface SchemaFieldNames {

	String getAttribute(String xmlAttributeName);
	
	String getAttributeInherited(String xmlAttributeName);
	
	String getAttributeRoot(String xmlAttributeName);

	String getAttributeSiblingPreceding(String xmlAttributeName);
	
}
