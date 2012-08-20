package se.simonsoft.xmltracking.index;

/**
 * Attribute fields prefixed with "a_", names not escaped at all.
 */
public class SchemaFieldNamesReposxml implements SchemaFieldNames {

	@Override
	public String getAttribute(String xmlAttributeName) {
		return "a_" + xmlAttributeName;
	}

	@Override
	public String getAttributeInherited(String xmlAttributeName) {
		return "i" + getAttribute(xmlAttributeName);
	}

	@Override
	public String getAttributeRoot(String xmlAttributeName) {
		return "r" + getAttribute(xmlAttributeName);
	}
	
	@Override
	public String getAttributeSiblingPreceding(String xmlAttributeName) {
		return "s" + getAttribute(xmlAttributeName);
	}

}
