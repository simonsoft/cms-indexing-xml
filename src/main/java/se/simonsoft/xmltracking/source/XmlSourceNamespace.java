package se.simonsoft.xmltracking.source;

public class XmlSourceNamespace {

	private String name;
	private String uri;
	
	public XmlSourceNamespace(String name, String uri) {
		if (name == null) {
			throw new IllegalArgumentException("name can not be null");
		}
		if (uri == null) {
			throw new IllegalArgumentException("uri can not be null");
		}		
		this.name = name;
		this.uri = uri;
	}
	
	public String getName() {
		return name;
	}
	
	public String getUri() {
		return uri;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj != null && obj instanceof XmlSourceNamespace
				&& name.equals(((XmlSourceNamespace) obj).getName())
				&& uri.equals(((XmlSourceNamespace) obj).getUri());
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return "xmlns:" + name + "=\"" + uri + "\"";
	}
	
}
