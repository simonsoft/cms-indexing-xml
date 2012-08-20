package se.simonsoft.xmltracking.source;

/**
 * 
 * TODO implement equals
 */
public class XmlSourceAttribute {

	private String name;
	private String value;

	public XmlSourceAttribute(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	/**
	 * @return Name including namespace if there is one
	 */
	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return getName() + "=" + getValue();
	}	
	
}
