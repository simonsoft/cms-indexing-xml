/**
 * Copyright (C) 2009-2012 Simonsoft Nordic AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.simonsoft.xmltracking.source.jdom;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

import se.simonsoft.xmltracking.source.XmlSourceAttribute;
import se.simonsoft.xmltracking.source.XmlSourceDoctype;
import se.simonsoft.xmltracking.source.XmlSourceElement;
import se.simonsoft.xmltracking.source.XmlSourceHandler;
import se.simonsoft.xmltracking.source.XmlSourceNamespace;
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
			XmlSourceDoctype doctype = null;
			DocType t = doc.getDocType();
			if (t != null) {
				doctype = new XmlSourceDoctype(t.getElementName(), t.getPublicID(), t.getSystemID());
			}
			Element root = doc.getRootElement();
			handler.startDocument(doctype);
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
		
		List<XmlSourceNamespace> namespaces = new LinkedList<XmlSourceNamespace>();
		List<Namespace> namespacesIntroduced = current.getNamespacesIntroduced();
		for (Namespace n : namespacesIntroduced) {
			namespaces.add(new XmlSourceNamespace(n.getPrefix(), n.getURI()));
		}
		
		List<XmlSourceAttribute> attributes = new LinkedList<XmlSourceAttribute>();
		List<Attribute> attrs = current.getAttributes();
		for (Attribute a : attrs) {
			String ns = a.getNamespacePrefix();
			if (ns.length() > 0) ns += ':';
			attributes.add(new XmlSourceAttribute(ns + a.getName(), a.getValue()));
		}
		
		String elementsource = outputter.outputString(current);
		elementsource = elementsource.replace("\r", ""); // TODO avoid jdom insertion of carriage returns even if source is LF only
		XmlSourceElement element = new XmlSourceElement(name, namespaces, attributes, elementsource);
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
