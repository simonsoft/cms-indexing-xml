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
package se.simonsoft.xmltracking.source.saxon;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import se.simonsoft.xmltracking.source.XmlSourceAttribute;
import se.simonsoft.xmltracking.source.XmlSourceElement;
import se.simonsoft.xmltracking.source.XmlSourceHandler;

/**
 * Design evaluation, reads an intermediate Solr doc and converts to java for processing before sending to index.
 */
public class ContentHandlerXmltrackingFields implements ContentHandler {

	private static final Logger logger = LoggerFactory.getLogger(ContentHandlerXmltrackingFields.class);
	
	// the element that corresponds to an indexing document
	private static final String DOC_NAME = "doc";
	
	// after all fields this element is expected, representing structure
	private static final String RECURSE_NAME = "children";
	
	// where to send elements for indexing
	private XmlSourceHandler handler;
	
	// state, depth first from root, required to set parent in element data instance
	private Stack<XmlSourceElement> current = new Stack<XmlSourceElement>();
	
	// state, fields, string only at this level (analysis performed when adding to index)
	private Map<String, String> fields = new HashMap<String, String>();
	
	// state, current field name
	private String field = null;
	
	// state, characters concatenation
	private StringBuffer content = new StringBuffer();
	
	public ContentHandlerXmltrackingFields(XmlSourceHandler handler) {
		this.handler = handler;
	}
	
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		logger.debug("characters {}+{}: " + new String(ch), start, length);
	}

	@Override
	public void endDocument() throws SAXException {
		logger.info("endDocument");
	}
	
	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		logger.debug("endPrefixMapping {}", prefix);
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
		logger.debug("ignorableWhitespace {}+{}", start, length);
	}

	@Override
	public void processingInstruction(String target, String data)
			throws SAXException {
		logger.debug("processingInstruction {} {}", target, data);
	}
	
	@Override
	public void setDocumentLocator(Locator locator) {
		logger.debug("setDocumentLocator {}", locator);
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		logger.debug("skippedEntity {}", name);
	}

	@Override
	public void startDocument() throws SAXException {
		logger.info("startDocument");
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		LinkedList<XmlSourceAttribute> a = new LinkedList<XmlSourceAttribute>();
		for (int i = 0; i < attributes.getLength(); i++) {
			a.add(new XmlSourceAttribute(attributes.getLocalName(i), attributes.getValue(i)));
		}
		logger.debug("startElement {} {} ({}), {}: {}", new Object[]{uri, localName, qName, attributes.getLength(), a});
		
		if (DOC_NAME.equals(localName)) {
			current.add(new XmlSourceElementFromEvents());
			current.lastElement().setDepth(current.size(), current.size() < 2 ? null : current.get(current.size() - 2));
		}
		
		if (RECURSE_NAME.equals(localName)) {
			handler.begin(current.lastElement());
		}
		
	}
	
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		logger.debug("endElement {} {} ({})", new Object[]{uri, localName, qName});
		
		if (DOC_NAME.equals(localName)) {
			current.pop();
		}
	}

	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		logger.debug("startPrefixMapping {} {}", prefix, uri);
	}

}
