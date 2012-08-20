package se.simonsoft.xmltracking.source.saxon;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;

import se.simonsoft.xmltracking.source.XmlSourceElement;
import se.simonsoft.xmltracking.source.XmlSourceHandler;

public class XmlSourceReaderXslTest {

	@SuppressWarnings("deprecation")
	@Test
	public void testRead() {
		InputStream test1 = this.getClass().getClassLoader().getResourceAsStream(
				"se/simonsoft/xmltracking/source/test1.xml");
		assertNotNull("Should find the test source", test1);
		InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
				"se/simonsoft/xmltracking/source/xml-indexing-recursive.xsl");
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
			System.out.println("adding " + element);
			elements.add(element);
		}
		
	}	
	
}
