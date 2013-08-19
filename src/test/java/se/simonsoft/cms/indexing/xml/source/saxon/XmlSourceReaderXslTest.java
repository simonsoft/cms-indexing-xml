/**
 * Copyright (C) 2009-2013 Simonsoft Nordic AB
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
package se.simonsoft.cms.indexing.xml.source.saxon;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;

import se.simonsoft.xmltracking.source.XmlSourceDoctype;
import se.simonsoft.xmltracking.source.XmlSourceElement;
import se.simonsoft.xmltracking.source.XmlSourceHandler;
import se.simonsoft.xmltracking.source.saxon.XmlSourceReaderXsl;

public class XmlSourceReaderXslTest {

	@SuppressWarnings("deprecation")
	@Test
	public void testRead() {
		InputStream test1 = this.getClass().getClassLoader().getResourceAsStream(
				"se/simonsoft/cms/indexing/xml/source/test1.xml");
		assertNotNull("Should find the test source", test1);
		InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
				"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
		// (test basic fields to solr doc)
		//		"se/simonsoft/xmltracking/source/xml-indexing-fields.xsl");		
		assertNotNull("Should find an xsl file to test with", xsl);
		Source xslSource = new StreamSource(xsl);
		
		TestHandler handler = new TestHandler();
		
		XmlSourceReaderXsl reader = new XmlSourceReaderXsl();
		reader.setExtractionStylesheet(xslSource);
		reader.read(test1, handler);
	}

	class TestHandler implements XmlSourceHandler {

		boolean started = false;
		boolean ended = false;
		List<XmlSourceElement> elements = new LinkedList<XmlSourceElement>();
		
		@Override
		public void startDocument(XmlSourceDoctype doctype) {
			assertFalse(started);
			started = true;
		}

		@Override
		public void endDocument() {
			assertFalse(ended);
			ended = true;
		}

		@Override
		public void begin(XmlSourceElement element) {
			System.out.println("adding " + element);
			elements.add(element);
		}
		
	}	
	
}
