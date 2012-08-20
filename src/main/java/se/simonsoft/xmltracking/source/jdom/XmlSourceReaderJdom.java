package se.simonsoft.xmltracking.source.jdom;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import se.simonsoft.xmltracking.source.XmlSourceAttribute;
import se.simonsoft.xmltracking.source.XmlSourceElement;
import se.simonsoft.xmltracking.source.XmlSourceHandler;
import se.simonsoft.xmltracking.source.XmlSourceReader;

/**
 * Jdom allows an easy API for getting the exact source of an element, including newlines etc.
 * Currently normalized on newline: always LF only, removing any original CR and those inserted by JDOM.
 */
public class XmlSourceReaderJdom implements XmlSourceReader {

	@Override
	public void read(InputStream xml, XmlSourceHandler handler) {
		// http://jdom.org/docs/apidocs/org/jdom/output/XMLOutputter.html
		// says it can do "untouched" output

		SAXBuilder builder = new SAXBuilder();
		configureBuilder(builder);
		XMLOutputter outputter = new XMLOutputter();

		try {
			Document doc = builder.build(xml);
			Element root = doc.getRootElement();
			handler.startDocument();
			register(root, handler, outputter, null, 1, null, 1);
			handler.endDocument();
		}
		// indicates a well-formedness error
		catch (JDOMException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected void configureBuilder(SAXBuilder builder) {
		builder.setValidation(false);
	    builder.setFeature("http://xml.org/sax/features/validation", false);
	    builder.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
	    builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	}

	/**
	 * Depth first traversal of DOM, see {@link XmlSourceElement} for field defintions.
	 */
	public static XmlSourceElement register(Element current, XmlSourceHandler handler, XMLOutputter outputter,
			XmlSourceElement parent, int depth, XmlSourceElement siblingPreceding, int position) {
		String name = current.getName();
		
		List<XmlSourceAttribute> attributes = new LinkedList<XmlSourceAttribute>();
		@SuppressWarnings("unchecked")
		List<Attribute> attrs = current.getAttributes();
		for (Attribute a : attrs) {
			String ns = a.getNamespacePrefix();
			if (ns.length() > 0) ns += ':';
			attributes.add(new XmlSourceAttribute(ns + a.getName(), a.getValue()));
		}
		
		String elementsource = outputter.outputString(current);
		elementsource = elementsource.replace("\r", ""); // TODO avoid jdom insertion of carriage returns even if source is LF only
		XmlSourceElement element = new XmlSourceElement(name, attributes, elementsource);
		element.setDepth(depth, parent);
		element.setPosition(position, siblingPreceding);
		
		handler.begin(element);
		
		List<?> children = current.getChildren();
		Iterator<?> iterator = children.iterator();
		int pos = 1;
		XmlSourceElement prev = null;
		while (iterator.hasNext()) {
			Element child = (Element) iterator.next();
			prev = register(child, handler, outputter,
					element, depth + 1, prev, pos++);
		}
		
		return element;
	}

}
