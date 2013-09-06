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
import static org.mockito.Mockito.*;

import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;

import se.simonsoft.xmltracking.index.add.XmlIndexFieldExtraction;
import se.simonsoft.xmltracking.source.saxon.IndexFieldExtractionCustomXsl;
import se.simonsoft.xmltracking.source.saxon.XmlMatchingFieldExtractionSource;

public class IndexFieldExtractionCustomXslTest {

	@Test
	public void test() {
		XmlIndexFieldExtraction x = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
				assertNotNull("Should find an xsl file to test with", xsl);
				return new StreamSource(xsl);
			}
		});
		
		IndexingDoc fields = mock(IndexingDoc.class);
		when(fields.getFieldValue("source")).thenReturn(
				"<document xml:lang=\"en\">\n" +
				"<section>section &amp; stuff</section>\n" +
				"<figure><title>Title</title>Figure</figure>\n" +						
				"</document>");
		
		x.extract(null, fields);
		verify(fields).addField("text", "section & stuff TitleFigure");
		verify(fields).addField(eq("source_reuse"), anyString());
		//verify(fields).addField("words_text", 4); ?
		//verify(fields).addField("words_text", 3);
		// string is ok if solr converts it
		verify(fields).addField("words_text", "3");
	}
	
	@Test
	public void testReuseDisqualifyOnRemovedRid() {
		XmlIndexFieldExtraction x = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
				assertNotNull("Should find an xsl file to test with", xsl);
				return new StreamSource(xsl);
			}
		});		
		
		IndexingDoc root = mock(IndexingDoc.class);
		when(root.getFieldValue("source")).thenReturn(
				"<document xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rlogicalid=\"xy1\" cms:rid=\"r01\">\n" +
				"<section cms:rlogicalid=\"xy2\" >section</section>\n" +
				"<figure cms:rlogicalid=\"xy3\" cms:rid=\"r03\"><title>Title</title>Figure</figure>\n" +						
				"</document>");
		when(root.getFieldValue("prop_cms:status")).thenReturn("Released");
		
		x.extract(null, root);
		// a child is disqualified from reuse so this element has to be too
		verify(root).addField("reusevalue", "-1");
		
		IndexingDoc norid = mock(IndexingDoc.class);
		when(norid.getFieldValue("source")).thenReturn(
				"<section xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rlogicalid=\"xy2\" >section</section>");
		when(norid.getFieldValue("prop_cms:status")).thenReturn("Released");
		
		x.extract(null, norid);
		verify(norid).addField("reusevalue", "-1");
		
		IndexingDoc sibling = mock(IndexingDoc.class);
		when(sibling.getFieldValue("source")).thenReturn(
				"<figure xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rlogicalid=\"xy3\" cms:rid=\"r03\"><title>Title</title>Figure</figure>");
		when(sibling.getFieldValue("prop_cms:status")).thenReturn("Released");
		
		x.extract(null, sibling);
		verify(sibling).addField("reusevalue", "1");
		
		IndexingDoc doc2 = mock(IndexingDoc.class);
		when(doc2.getFieldValue("prop_cms:status")).thenReturn("In_Translation");
		when(doc2.getFieldValue("source")).thenReturn("<doc/>");
		x.extract(null, doc2);
		verify(doc2).addField("reusevalue", "0");
		
		IndexingDoc doc3 = mock(IndexingDoc.class);
		when(doc3.getFieldValue("prop_cms:status")).thenReturn(null);
		when(doc3.getFieldValue("source")).thenReturn("<doc/>");
		x.extract(null, doc3);
		verify(doc3).addField("reusevalue", "0");
	}

}
