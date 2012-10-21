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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import se.simonsoft.cms.indexing.IndexFields;

/**
 * Passes solr doc style field=value elements directly to {@link IndexFields}.
 */
class ContentHandlerToIndexFields implements ContentHandler {

	private static final Logger logger = LoggerFactory.getLogger(ContentHandlerToIndexFields.class);
	
	private IndexFields fields;
	
	private transient StringBuffer buf = new StringBuffer();
	private transient String curfield = null;
	
	/**
	 * @param fields to add extracted fields to
	 */
	public ContentHandlerToIndexFields(IndexFields fields) {
		this.fields = fields;
	}

	/**
	 * Detects type and adds to index fields.
	 * @param field Field name
	 * @param value String value from transform output, decoded
	 */
	private void add(String field, String value) {
		// TODO type handling, or does solr convert to schema's field type?
		fields.addField(field, value);
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		logger.trace("startElement {} {} ({}), {}: {}", new Object[]{uri, localName, qName, attributes.getLength()});
		if ("field".equals(localName)) {
			curfield = attributes.getValue("name");
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		logger.trace("endElement {} {} ({})", new Object[]{uri, localName, qName});
		if ("field".equals(localName)) {
			logger.debug("Adding field {}={}", curfield, buf.length() > 40 ? buf.substring(0, 40) + "..." : buf);
			add(curfield, buf.toString());
			buf.setLength(0);
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		//logger.trace("characters {}+{}: " + new String(ch), start, length);
		buf.append(ch);
	}

	@Override
	public void endDocument() throws SAXException {
		logger.info("endDocument");
	}
	
	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		logger.trace("endPrefixMapping {}", prefix);
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
		logger.trace("ignorableWhitespace {}+{}", start, length);
	}

	@Override
	public void processingInstruction(String target, String data)
			throws SAXException {
		logger.trace("processingInstruction {} {}", target, data);
	}
	
	@Override
	public void setDocumentLocator(Locator locator) {
		logger.trace("setDocumentLocator {}", locator);
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		logger.trace("skippedEntity {}", name);
	}

	@Override
	public void startDocument() throws SAXException {
		logger.info("startDocument");
	}
	
	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		logger.trace("startPrefixMapping {} {}", prefix, uri);
	}
	
}
