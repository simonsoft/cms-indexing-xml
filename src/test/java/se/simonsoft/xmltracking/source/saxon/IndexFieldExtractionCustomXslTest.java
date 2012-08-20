package se.simonsoft.xmltracking.source.saxon;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;

import se.simonsoft.xmltracking.index.add.IndexFieldExtraction;
import se.simonsoft.xmltracking.index.add.IndexFields;

public class IndexFieldExtractionCustomXslTest {

	@Test
	public void test() {
		IndexFieldExtraction x = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/xmltracking/source/xml-indexing-fields.xsl");
				assertNotNull("Should find an xsl file to test with", xsl);
				return new StreamSource(xsl);
			}
		});
		
		IndexFields fields = mock(IndexFields.class);
		when(fields.getFieldValue("source")).thenReturn(
				"<document xml:lang=\"en\">\n" +
				"<section>section &amp; stuff</section>\n" +
				"<figure><title>Title</title>Figure</figure>\n" +						
				"</document>");
		
		x.extract(fields, null);
		verify(fields).addField("text", "section & stuff TitleFigure");
		verify(fields).addField(eq("source_reuse"), anyString());
		//verify(fields).addField("words_text", 4); ?
		//verify(fields).addField("words_text", 3);
		// string is ok if solr converts it
		verify(fields).addField("words_text", "3");
	}

}
