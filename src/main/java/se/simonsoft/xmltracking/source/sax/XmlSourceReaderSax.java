package se.simonsoft.xmltracking.source.sax;

import java.io.InputStream;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import se.simonsoft.xmltracking.source.XmlSourceHandler;
import se.simonsoft.xmltracking.source.XmlSourceReader;

public class XmlSourceReaderSax implements XmlSourceReader {

	@Override
	public void read(InputStream xml, XmlSourceHandler handler) {
		XMLReader xr;
		try {
			xr = XMLReaderFactory.createXMLReader();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException("Error not handled", e);
		}
		throw new UnsupportedOperationException("not implemented");
	}

}
