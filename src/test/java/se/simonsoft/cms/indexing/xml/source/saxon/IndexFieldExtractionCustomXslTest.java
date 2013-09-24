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
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
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
		
		IndexingDoc root = new IndexingDocIncrementalSolrj();
		root.setField("source",
				"<document xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rlogicalid=\"xy1\" cms:rid=\"r01\">\n" +
				"<section cms:rlogicalid=\"xy2\" >section</section>\n" +
				"<figure cms:rlogicalid=\"xy3\" cms:rid=\"r03\"><title>Title</title>Figure</figure>\n" +						
				"</document>");
		root.setField("prop_cms.status", "Released");
		
		x.extract(null, root);
		assertEquals("a child is disqualified from reuse so this element has to be too", "-3", root.getFieldValue("reusevalue"));
		assertEquals("the element is status=Released so reuseready is not affected", "1", root.getFieldValue("reuseready"));
		
		IndexingDoc norid = new IndexingDocIncrementalSolrj();
		norid.setField("source",
				"<section xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rlogicalid=\"xy2\" >section</section>");
		norid.setField("prop_cms.status", "Released");
		
		x.extract(null, norid);
		assertEquals("-2", norid.getFieldValue("reusevalue"));
		
		IndexingDoc sibling = new IndexingDocIncrementalSolrj();
		sibling.setField("source",
				"<figure xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rlogicalid=\"xy3\" cms:rid=\"r03\"><title>Title</title>Figure</figure>");
		sibling.setField("prop_cms.status", "Released");
		
		x.extract(null, sibling);
		assertEquals("1", sibling.getFieldValue("reusevalue"));
		
		IndexingDoc doc2 = new IndexingDocIncrementalSolrj();
		doc2.addField("prop_cms.status", "In_Translation");
		doc2.addField("source", "<doc/>");
		
		x.extract(null, doc2);
		assertEquals("-2", doc2.getFieldValue("reusevalue"));
		assertEquals("status is not released", "0", doc2.getFieldValue("reuseready"));
		
		IndexingDoc doc3 = new IndexingDocIncrementalSolrj();
		doc3.setField("prop_cms.status", null);
		doc3.setField("source", "<doc/>");
		x.extract(null, doc3);
		assertEquals("-2", doc3.getFieldValue("reusevalue"));
		// TODO assert reuseready
	}

}
