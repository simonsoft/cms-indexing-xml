package se.simonsoft.xmltracking.source;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
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
				"se/simonsoft/xmltracking/source/test1.xml");
		TestHandler handler = new TestHandler();
		
		reader.read(test1, handler);
		assertTrue(handler.started);
		assertTrue(handler.ended);
		
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
				"</section>", s1.getSource());
		assertEquals(2, s1.getDepth());
		assertEquals(1, s1.getPosition());
		assertEquals(null, s1.getSiblingPreceding());
		
		XmlSourceElement s1p = verifyCommon(handler.elements.get(2));
		assertEquals("expecting depth-first", "p", s1p.getName());
		assertEquals(s1, s1p.getParent());
		assertEquals("<p>This is the para\n" + 
				"text &amp; some &lt;escaped&gt; stuff</p>", s1p.getSource());
		assertEquals(3, s1p.getDepth());
		assertEquals(1, s1p.getPosition());
		assertEquals(s1, s1p.getParent());
		
		XmlSourceElement s2 = verifyCommon(handler.elements.get(3));
		assertEquals("section", s2.getName());
		assertEquals("<section meaningful=\"no\">\n" +
				"	<!-- empty -->\n" +
				"	</section>", s2.getSource());
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
				"se/simonsoft/xmltracking/source/testnamespaces.xml");
		TestHandler handler = new TestHandler();
		
		reader.read(testns, handler);
		XmlSourceElement d = verifyCommon(handler.elements.get(0));
		assertEquals("document", d.getName());
		assertEquals(3, d.getAttributes().size());
		System.out.println(d.getAttributes());
		assertEquals("Should produce attributes in order",
				"notnamespaced", d.getAttributes().get(0).getName());
		assertEquals("Attribute names should include namespace", 
				"cms:rid", d.getAttributes().get(1).getName());
		assertEquals("extra:id", d.getAttributes().get(2).getName());
		
		XmlSourceElement c1 = verifyCommon(handler.elements.get(1));
		assertEquals(1, c1.getAttributes().size());
		// Namespace may be needed for subsequent analysis to run, for example xsl transform
//		assertEquals("source should be original, without namespace declaration",
//				"<child cms:rid=\"X01\">\n"
//					+ "		text\n"
//					+ "	</child>", c1.getSource());
		
		XmlSourceElement c2 = verifyCommon(handler.elements.get(2));
		assertEquals(1, c2.getAttributes().size());
		assertEquals("source should be original, without only original namespace declaration",
				//"<child xmlns:new=\"http://www.simonsoft.se/namespace/new\" new:a=\"v\"/>", c2.getSource());
				"<child xmlns:new=\"http://www.simonsoft.se/namespace/new\" new:a=\"v\" />", c2.getSource()); // whitespace added, allowed?
	}
	
	private XmlSourceElement verifyCommon(XmlSourceElement element) {
		String source = element.getSource();
		assertNotNull(source);
		assertEquals("Source should always start with element, no padding", 
				'<', source.charAt(0));
		assertEquals("Source should always end with element, no padding",
				'>', source.charAt(source.length() - 1));
		return element;
	}
	
	class TestHandler implements XmlSourceHandler {

		boolean started = false;
		boolean ended = false;
		List<XmlSourceElement> elements = new LinkedList<XmlSourceElement>();
		
		@Override
		public void startDocument() {
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
			elements.add(element);
		}
		
	}

}
