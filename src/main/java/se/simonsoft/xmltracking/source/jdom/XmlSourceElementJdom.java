package se.simonsoft.xmltracking.source.jdom;

import java.util.List;

import org.jdom2.Element;

import se.simonsoft.xmltracking.source.XmlSourceAttribute;
import se.simonsoft.xmltracking.source.XmlSourceElement;
import se.simonsoft.xmltracking.source.XmlSourceNamespace;

public class XmlSourceElementJdom extends XmlSourceElement {

	private Element element;

	protected XmlSourceElementJdom(Element current,
			String name, List<XmlSourceNamespace> namespaces,
			List<XmlSourceAttribute> attributes, String elementsource) {
		super(name, namespaces, attributes, elementsource);
		this.element = current;
	}
	
	public Element getElement() {
		return this.element;
	}
	
}
