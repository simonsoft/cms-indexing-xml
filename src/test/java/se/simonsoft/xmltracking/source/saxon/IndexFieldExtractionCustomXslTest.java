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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;

import se.simonsoft.cms.indexing.IndexFields;
import se.simonsoft.xmltracking.index.add.IndexFieldExtraction;

public class IndexFieldExtractionCustomXslTest {

	@Test
	public void test() {
		IndexFieldExtraction x = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
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
	
	@Test
	public void testReuseDisqualifyOnRemovedRid() {
		IndexFieldExtraction x = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
				assertNotNull("Should find an xsl file to test with", xsl);
				return new StreamSource(xsl);
			}
		});		
		
		IndexFields root = mock(IndexFields.class);
		when(root.getFieldValue("source")).thenReturn(
				"<document xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rlogicalid=\"xy1\" cms:rid=\"r01\">\n" +
				"<section cms:rlogicalid=\"xy2\" >section</section>\n" +
				"<figure cms:rlogicalid=\"xy3\" cms:rid=\"r03\"><title>Title</title>Figure</figure>\n" +						
				"</document>");
		when(root.getFieldValue("prop_cms:status")).thenReturn("Released");
		
		x.extract(root, null);
		// a child is disqualified from reuse so this element has to be too
		verify(root).addField("reusevalue", "-1");
		
		IndexFields norid = mock(IndexFields.class);
		when(norid.getFieldValue("source")).thenReturn(
				"<section xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rlogicalid=\"xy2\" >section</section>");
		when(norid.getFieldValue("prop_cms:status")).thenReturn("Released");
		
		x.extract(norid, null);
		verify(norid).addField("reusevalue", "-1");
		
		IndexFields sibling = mock(IndexFields.class);
		when(sibling.getFieldValue("source")).thenReturn(
				"<figure xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rlogicalid=\"xy3\" cms:rid=\"r03\"><title>Title</title>Figure</figure>");
		when(sibling.getFieldValue("prop_cms:status")).thenReturn("Released");
		
		x.extract(sibling, null);
		verify(sibling).addField("reusevalue", "1");
		
		IndexFields doc2 = mock(IndexFields.class);
		when(doc2.getFieldValue("prop_cms:status")).thenReturn("In_Translation");
		when(doc2.getFieldValue("source")).thenReturn("<doc/>");
		x.extract(doc2, null);
		verify(doc2).addField("reusevalue", "0");
		
		IndexFields doc3 = mock(IndexFields.class);
		when(doc3.getFieldValue("prop_cms:status")).thenReturn(null);
		when(doc3.getFieldValue("source")).thenReturn("<doc/>");
		x.extract(doc3, null);
		verify(doc3).addField("reusevalue", "0");
	}

}
