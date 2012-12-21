package se.simonsoft.xmltracking.source;

public class XmlSourceDoctype {

	private String elementName;
	private String publicID;
	private String systemID;

	public XmlSourceDoctype(java.lang.String elementName, java.lang.String publicID, java.lang.String systemID) {
		this.elementName = elementName;
		this.publicID = publicID;
		this.systemID = systemID;
	}
	
	public String getElementName() {
		return elementName;
	}

	public String getPublicID() {
		return publicID;
	}

	public String getSystemID() {
		return systemID;
	}
	
}
