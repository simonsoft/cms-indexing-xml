package se.simonsoft.xmltracking.source.saxon;

import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

public class XmlMatchingFieldExtractionSourceDefault implements
		XmlMatchingFieldExtractionSource {

	@Override
	public Source getXslt() {
		InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
				"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
		return new StreamSource(xsl);
	}

}
