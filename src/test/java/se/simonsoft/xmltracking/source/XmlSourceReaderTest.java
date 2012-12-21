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
package se.simonsoft.xmltracking.source;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import se.simonsoft.xmltracking.source.XmlSourceAttribute;
import se.simonsoft.xmltracking.source.XmlSourceElement;
import se.simonsoft.xmltracking.source.XmlSourceHandler;
import se.simonsoft.xmltracking.source.XmlSourceReader;
import se.simonsoft.xmltracking.source.jdom.XmlSourceReaderJdom;

/**
 * Compliance tests for all impls
 */
public class XmlSourceReaderTest {

	XmlSourceReader reader = null;
	
	@Before
	public void setUp() {
		//reader = new XmlSourceReaderSax();
		reader = new XmlSourceReaderJdom();
	}
	
	@Test
	public void testRead() throws IOException {
		InputStream test1 = this.getClass().getClassLoader().getResourceAsStream(
				"se/simonsoft/cms/indexing/xml/source/test1.xml");
		TestHandler handler = new TestHandler();
		
		reader.read(test1, handler);
		assertTrue(handler.started);
		assertTrue(handler.ended);
		assertNull("file does not have doctype", handler.doctype);
		
		XmlSourceElement root = verifyCommon(handler.elements.get(0));
		assertEquals("document", root.getName());
		assertEquals(0, root.getAttributes().size());
		assertEquals(null, root.getParent());
		assertEquals(1, root.getDepth());
		assertEquals(null, root.getSiblingPreceding());
		assertEquals("position should start from 1, same as in XSL", 1, root.getPosition());
		
		XmlSourceElement s1 = verifyCommon(handler.elements.get(1));
		assertEquals("section", s1.getName());
		assertEquals(root, s1.getParent());
		assertEquals(0, s1.getAttributes().size());
		verifyCommon(s1);
		assertEquals("<section><p>This is the para\n" + 
				"text &amp; some &lt;escaped&gt; stuff</p>\n" +
				"</section>", getSource(s1));
		assertEquals(2, s1.getDepth());
		assertEquals(1, s1.getPosition());
		assertEquals(null, s1.getSiblingPreceding());
		
		XmlSourceElement s1p = verifyCommon(handler.elements.get(2));
		assertEquals("expecting depth-first", "p", s1p.getName());
		assertEquals(s1, s1p.getParent());
		assertEquals("<p>This is the para\n" + 
				"text &amp; some &lt;escaped&gt; stuff</p>", getSource(s1p));
		assertEquals(3, s1p.getDepth());
		assertEquals(1, s1p.getPosition());
		assertEquals(s1, s1p.getParent());
		
		XmlSourceElement s2 = verifyCommon(handler.elements.get(3));
		assertEquals("section", s2.getName());
		assertEquals("<section meaningful=\"no\">\n" +
				"	<!-- empty -->\n" +
				"	</section>", getSource(s2));
		assertEquals(root, s2.getParent());
		assertEquals(1, s2.getAttributes().size());
		XmlSourceAttribute a1 = s2.getAttributes().get(0);
		assertEquals("meaningful", a1.getName());
		assertEquals("no", a1.getValue());
		assertEquals(2, s2.getPosition());
		assertEquals(s1, s2.getSiblingPreceding());
	}
	
	@Test
	public void testNamespaced() throws IOException {
		InputStream testns = this.getClass().getClassLoader().getResourceAsStream(
				"se/simonsoft/cms/indexing/xml/source/testnamespaces.xml");
		TestHandler handler = new TestHandler();
		
		reader.read(testns, handler);
		XmlSourceElement d = verifyCommon(handler.elements.get(0));
		assertEquals("document", d.getName());
		assertEquals(3, d.getAttributes().size());
		
		assertNotNull("Should read doctype when there is one", handler.doctype);
		assertEquals("Should read doctype", "document", handler.doctype.getElementName());
		assertEquals("-//Simonsoft//DTD xxx//EN", handler.doctype.getPublicID());
		assertEquals("techdoc.dtd", handler.doctype.getSystemID());
		
		assertEquals("Should produce attributes in order",
				"notnamespaced", d.getAttributes().get(0).getName());
		assertEquals("Attribute names should include namespace", 
				"cms:rid", d.getAttributes().get(1).getName());
		assertEquals("extra:id", d.getAttributes().get(2).getName());
		
		assertEquals("Should have namespaces", 2, d.getNamespaces().size());
		assertEquals("Should produce declared attributes in order",
				"cms", d.getNamespaces().get(0).getName());
		assertEquals("http://www.simonsoft.se/namespace/cms", d.getNamespaces().get(0).getUri());
		assertEquals("extra", d.getNamespaces().get(1).getName());
		assertEquals("http://www.simonsoft.se/namespace/extra", d.getNamespaces().get(1).getUri());
		
		XmlSourceElement c1 = verifyCommon(handler.elements.get(1));
		assertEquals(1, c1.getAttributes().size());
		assertEquals("no new namespaces here", 0, c1.getNamespaces().size());
		
		// Namespace may be needed for subsequent analysis to run, for example xsl transform
//		assertEquals("source should be original, without namespace declaration",
//				"<child cms:rid=\"X01\">\n"
//					+ "		text\n"
//					+ "	</child>", c1.getSource());
		
		XmlSourceElement c2 = verifyCommon(handler.elements.get(2));
		assertEquals(1, c2.getAttributes().size());
		assertEquals("source should be original, without only original namespace declaration",
				//"<child xmlns:new=\"http://www.simonsoft.se/namespace/new\" new:a=\"v\"/>", getSource(c2));
				"<child xmlns:new=\"http://www.simonsoft.se/namespace/new\" new:a=\"v\" />", getSource(c2)); // whitespace added, allowed?
		assertEquals("should have the new namespace but not the inherited",
				1, c2.getNamespaces().size());
	}
	
	private XmlSourceElement verifyCommon(XmlSourceElement element) {
		String source = getSource(element);
		assertNotNull(source);
		assertEquals("Source should always start with element, no padding", 
				'<', source.charAt(0));
		assertEquals("Source should always end with element, no padding",
				'>', source.charAt(source.length() - 1));
		return element;
	}

	private String getSource(XmlSourceElement element) {
		Reader sourceReader = element.getSource();
		StringBuffer b = new StringBuffer();
		int c;
		try {
			while ((c = sourceReader.read()) > -1) {
				b.append((char) c);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return b.toString();
	}
	
	class TestHandler implements XmlSourceHandler {

		boolean started = false;
		XmlSourceDoctype doctype = null;
		boolean ended = false;
		List<XmlSourceElement> elements = new LinkedList<XmlSourceElement>();
		
		@Override
		public void startDocument(XmlSourceDoctype doctype) {
			assertFalse(started);
			started = true;
			this.doctype = doctype;
		}

		@Override
		public void endDocument() {
			assertFalse(ended);
			ended = true;
		}

		@Override
		public void begin(XmlSourceElement element) {
			elements.add(element);
		}
		
	}

}
