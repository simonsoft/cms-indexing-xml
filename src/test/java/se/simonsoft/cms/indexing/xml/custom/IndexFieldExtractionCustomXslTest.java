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
package se.simonsoft.cms.indexing.xml.custom;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.indexing.xml.custom.IndexFieldExtractionCustomXsl;
import se.simonsoft.cms.indexing.xml.custom.XmlMatchingFieldExtractionSource;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;

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
		//verify(fields).addField("text", "section & stuff TitleFigure");
		verify(fields).addField("text", "section & stuff Title Figure");
		verify(fields).addField(eq("source_reuse"), anyString());

		verify(fields).addField("words_text", "5");
	}
	
	
	
	/**
	 * Testing normalization of space (specifically ensure space btw elements and newline becoming space).
	 * Also tests that comments are ignored in source_reuse.
	 */
	@Test
	public void testNormalization() {
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
				"<!-- A comment. -->" +
				"<section><title>section &amp; stuff</title>\n" +
				"<p>Even paragraphs will have new-lines\n" +
				"right within them.</p>\n" +
				"</section>\n" +
				"<figure><title>Title</title>Figure</figure>\n" +						
				"</document>");
		
		x.extract(null, fields);
		//verify(fields).addField("text", "section & stuff Even paragraphs will have new-lines right within them. TitleFigure");
		verify(fields).addField("text", "section & stuff Even paragraphs will have new-lines right within them. Title Figure");
		verify(fields).addField("words_text", "13");
		
		// source_reuse gets plain &, not &amp;. This must be caused by code, not the XSL.
		// New-lines are removed by normalize space, i.e. text nodes with only whitespace are completely removed. 
		// Can actually make space btw 2 inline elements disappear... still just for checksum.
		verify(fields).addField("source_reuse", "<document><section><title>section & stuff</title><p>Even paragraphs will have new-lines right within them.</p></section><figure><title>Title</title>Figure</figure></document>");

		
	}
	
	@Test
	public void testNormalizationPreserve() {
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
				"<code xml:space=\"preserve\">\n" +
				"    Indented code\n" +
				"        Double  space\n" +
				"</code>\n" +
				"</document>");
		
		x.extract(null, fields);
		verify(fields).addField("text", "Indented code Double space");
		//verify(fields).addField("text", "Indented code Double space");
		verify(fields).addField("source_reuse", "<document><code xml:space=\"preserve\">\n    Indented code\n        Double  space\n</code></document>");

		verify(fields).addField("words_text", "4");
	}
	
	@Test
	public void testProcessInstruction() {
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
				"<!-- A comment. -->" +
				"<section><title>No<?Pub _hardspace?>Break</title>\n" +
				"<p><?Pub _font FontColor=\"red\" SmallCap=\"yes\"\n" +
				"?>Specific text touchup.<?Pub /_font?></p>\n" +
				"</section>\n" +					
				"</document>");
		
		x.extract(null, fields);
		verify(fields).addField("words_text", "5");
		verify(fields).addField("text", "No Break Specific text touchup.");
		verify(fields).addField("source_reuse", "<document><section><title>No<?Pub _hardspace?>Break</title><p><?Pub _font FontColor=\"red\" SmallCap=\"yes\"?>Specific text touchup.<?Pub /_font?></p></section></document>");

	}
	
	@Test
	public void testAttributesBursting() {
		XmlIndexFieldExtraction x = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
				assertNotNull("Should find an xsl file to test with", xsl);
				return new StreamSource(xsl);
			}
		});
		
		
		IndexingDoc fields =  new IndexingDocIncrementalSolrj();
		fields.setField("source",
				"<document xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" xml:lang=\"en\" status=\"Released\" revision=\"123\" revision-baseline=\"123\" revision-commit=\"123\" modifieddate=\"2013-01-01\" modifiedby=\"bill\">\n" +
				"<section xml:id=\"must-be\" cms:rlogicalid=\"xy2\" cms:rid=\"abc002\">\n" +
				"<title cms:rid=\"abc003\">section &amp; stuff</title>\n" +
				"<p cms:rid=\"abc004\" cms:trid=\"xyz006\" status=\"Released\" revision=\"123\" revision-baseline=\"123\" revision-commit=\"123\" modifieddate=\"2013-01-01\" modifiedby=\"bill\">Testing bursted attributes,\n" +
				"twoway or toxml.</p>\n" +
				"</section>\n" +
				"<figure cms:rid=\"abc005\"><title cms:rid=\"abc006\">Title</title>Figure</figure>\n" +						
				"</document>");
		
		x.extract(null, fields);
		assertEquals("section & stuff Testing bursted attributes, twoway or toxml. Title Figure", fields.getFieldValue("text"));
		// Bursted attributes should definitely be excluded from root. Potentially all attributes excluded on root.
		assertEquals("<document><section xml:id=\"must-be\"><title>section & stuff</title><p status=\"Released\" revision=\"123\" revision-baseline=\"123\" revision-commit=\"123\" modifieddate=\"2013-01-01\" modifiedby=\"bill\">Testing bursted attributes, twoway or toxml.</p></section><figure><title>Title</title>Figure</figure></document>", fields.getFieldValue("source_reuse"));
		// Potentially excluding bursted attributes on all elements, but requires configuration.
		//assertEquals("<document><section xml:id=\"must-be\"><title>section & stuff</title><p>Testing bursted attributes, twoway or toxml.</p></section><figure><title>Title</title>Figure</figure></document>", fields.getFieldValue("source_reuse"));
		assertEquals("11", fields.getFieldValue("words_text"));
	}
	
	@Test
	public void testAttributesCms() {
		XmlIndexFieldExtraction x = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
				assertNotNull("Should find an xsl file to test with", xsl);
				return new StreamSource(xsl);
			}
		});
		
		
		IndexingDoc fields =  new IndexingDocIncrementalSolrj();
		fields.setField("source",
				"<document xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" xml:lang=\"en\" cms:rwords=\"10\" cms:rlogicalid=\"xy1\" cms:rid=\"abc001\" cms:twords=\"15\">\n" +
				"<section cms:rlogicalid=\"xy2\" cms:rid=\"abc002\">\n" +
				"<title cms:rid=\"abc003\">section &amp; stuff</title>\n" +
				"<p cms:rid=\"abc004\" cms:tstatus=\"Released\" cms:tlogicalid=\"x-svn...\" cms:tmatch=\"element1\" cms:tpos=\"1.x.x\" cms:trid=\"xyz006\" cms:twords=\"5\">Testing cms attributes\n" +
				"including tvalidate.</p>\n" +
				"</section>\n" +
				"<figure cms:rid=\"abc005\" cms:tvalidate=\"no\"><title cms:rid=\"abc006\">Title</title>Figure</figure>\n" +						
				"</document>");
		
		x.extract(null, fields);
		assertEquals("section & stuff Testing cms attributes including tvalidate. Title Figure", fields.getFieldValue("text"));
		assertEquals("<document><section><title>section & stuff</title><p>Testing cms attributes including tvalidate.</p></section><figure cms:tvalidate=\"no\"><title>Title</title>Figure</figure></document>", fields.getFieldValue("source_reuse"));
		assertEquals("10", fields.getFieldValue("words_text"));
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
		assertEquals("1", sibling.getFieldValue("reuseready"));
		
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
		assertEquals("0", doc3.getFieldValue("reuseready"));
	}
	
	/**
	 * Transformation may have higher validity requirements than Jdom parsing.
	 */
	@Test
	public void testInvalidXml() {
		// found in Flir data x-svn:///svn/flir^/itc-swe/lang/fr-FR/xml/T404000-T405000/T404014.xml?p=6739
		// but does the current element "source" concept handle entities that are actually declared?
		String element = "<p>Conference R&D;</p>";
		
		XmlIndexFieldExtraction xsl = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSourceDefault());
		
		IndexingDoc fields = mock(IndexingDoc.class);
		when(fields.getFieldValue("source")).thenReturn(element);
		
		try {
			xsl.extract(null, fields);
			fail("Should throw declared exception for xml error");
		} catch (XmlNotWellFormedException e) { // any other exception would abort indexing
			// expected
		}
	}

}
