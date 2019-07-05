/**
 * Copyright (C) 2009-2016 Simonsoft Nordic AB
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.Processor;

import org.junit.Before;
import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexRidDuplicateDetection;
import se.simonsoft.cms.indexing.xml.testconfig.IndexingConfigXmlBase;
import se.simonsoft.cms.indexing.xml.testconfig.IndexingConfigXmlStub;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class IndexFieldExtractionCustomXslTest {
	
	private Injector injector;
	Processor p;
	
	@Before
	public void setUp() {
		
		injector = Guice.createInjector(new IndexingConfigXmlBase(), new IndexingConfigXmlStub());
		p = injector.getInstance(Processor.class);
	}

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
		}, p);
		
		IndexingDoc fields = mock(IndexingDoc.class);
		when(fields.getFieldValue("source")).thenReturn(
				"<document xml:lang=\"en\">\n" +
				"<section>section &amp; stuff</section>\n" +
				"<figure><title>Title</title>Figure</figure>\n" +						
				"</document>");
		
		x.end(null, null, fields);
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
		}, p);
		
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
		
		x.end(null, null, fields);
		//verify(fields).addField("text", "section & stuff Even paragraphs will have new-lines right within them. TitleFigure");
		verify(fields).addField("text", "section & stuff Even paragraphs will have new-lines right within them. Title Figure");
		verify(fields).addField("words_text", "13");
		
		// source_reuse gets plain &, not &amp;. This must be caused by code, not the XSL.
		// New-lines are removed by normalize space, i.e. text nodes with only whitespace are completely removed. 
		// Can actually make space btw 2 inline elements disappear... still just for checksum.
		verify(fields).addField("source_reuse", "<document><section><title>section & stuff</title><p>Even paragraphs will have new-lines right within them.</p></section><figure><title>Title</title>Figure</figure></document>");
	}

	/**
	 * Testing normalization in the context of Assist.
	 * The (CMS 3.0) indexing is not designed for using source_reuse for insert.
	 * If using source_reuse for Assist or other inserts, the Normalization must be very exact.
	 */
	@Test
	public void testNormalizationAssistSpace() {
		XmlIndexFieldExtraction x = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
				assertNotNull("Should find an xsl file to test with", xsl);
				return new StreamSource(xsl);
			}
		}, p);

		IndexingDoc fields = mock(IndexingDoc.class);
		when(fields.getFieldValue("source")).thenReturn(
				"<p>Ett <code>två</code><code>tre</code> <code>fyra</code>  <code>fem</code>   <code> sex </code> sju.</p>");

		x.end(null, null, fields);
		verify(fields).addField("text", "Ett två tre fyra fem sex sju.");
		verify(fields).addField("words_text", "7");

		// source_reuse gets plain &, not &amp;. This must be caused by code, not the XSL.
		// New-lines are removed by normalize space, i.e. text nodes with only whitespace are completely removed. 
		// Can actually make space btw 2 inline elements disappear... important for reuse.
		verify(fields).addField("source_reuse", "<p>Ett <code>två</code><code>tre</code> <code>fyra</code> <code>fem</code> <code> sex </code> sju.</p>");
	}
	/**
	 * Testing normalization in the context of Assist.
	 * The (CMS 3.0) indexing is not designed for using source_reuse for insert.
	 * If using source_reuse for Assist or other inserts, the Normalization must be very exact.
	 */
	@Test
	public void testNormalizationAssistVVAB() {
		XmlIndexFieldExtraction x = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
				assertNotNull("Should find an xsl file to test with", xsl);
				return new StreamSource(xsl);
			}
		}, p);

		IndexingDoc fields = mock(IndexingDoc.class);
		when(fields.getFieldValue("source")).thenReturn(
				"<p>Väderstad <termref linkend=\"platform\"/> <termref linkend=\"type\"\n" +
						"/> are combination machines designed for direct seed drilling.</p\n" +
						">");

		x.end(null, null, fields);
		verify(fields).addField("text", "Väderstad are combination machines designed for direct seed drilling.");
		verify(fields).addField("words_text", "9");

		// source_reuse gets plain &, not &amp;. This must be caused by code, not the XSL.
		// New-lines are removed by normalize space, i.e. text nodes with only whitespace are completely removed. 
		// Can actually make space btw 2 inline elements disappear... important for reuse.
		verify(fields).addField("source_reuse", "<p>Väderstad <termref linkend=\"platform\"></termref> <termref linkend=\"type\"></termref> are combination machines designed for direct seed drilling.</p>");
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
		}, p);
		
		IndexingDoc fields = mock(IndexingDoc.class);
		when(fields.getFieldValue("source")).thenReturn(
				"<document xml:lang=\"en\">\n" +
				"<code xml:space=\"preserve\">\n" +
				"    Indented code\n" +
				"        Double  space\n" +
				"</code>\n" +
				"</document>");
		
		x.end(null, null, fields);
		verify(fields).addField("text", "Indented code Double space");
		//verify(fields).addField("text", "Indented code Double space");
		verify(fields).addField("source_reuse", "<document><code xml:space=\"preserve\">\n    Indented code\n        Double  space\n</code></document>");

		verify(fields).addField("words_text", "4");
	}
	
	/**
	 * Ensure that ph element content is excluded from source_reuse.
	 */
	@Test
	public void testPhElement() {
		XmlIndexFieldExtraction x = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
				assertNotNull("Should find an xsl file to test with", xsl);
				return new StreamSource(xsl);
			}
		}, p);
		
		IndexingDoc fields = mock(IndexingDoc.class);
		when(fields.getFieldValue("source")).thenReturn(
				"<document xml:lang=\"en\">\n" +
				"<section><title>GUI Strings</title>\n" +
				"<p>Press button <ph keyref=\"btn_success\" a=\"first\">SUCCESS</ph> and ...</p>\n" +
				"<p>Press button <ph keyref=\"btn_success\"><?Pub _previewtext text=\"SUCCESS PI\"?></ph> and ...</p>\n" +
				"<p>Press button <ph>This is not a keyref</ph> and ...</p>\n" +
				"</section>\n" +					
				"</document>");
		
		x.end(null, null, fields);
		verify(fields).addField("text", "GUI Strings Press button SUCCESS and ... Press button and ... Press button This is not a keyref and ...");
		verify(fields).addField("words_text", "20"); // Preferably excluding ph content also in text field, but that would be complex in the XSL.
		// A PI in ph will be completely disregarded but text as direct child of ph will currently be counted/searchable.
		verify(fields).addField("source_reuse", "<document><section><title>GUI Strings</title>" +
				//"<p>Press button <ph a=\"first\" keyref=\"btn_success\"></ph> and ...</p>" + 
				"<p>Press button <ph keyref=\"btn_success\" a=\"first\"></ph> and ...</p>" + // Currently demonstrates that attributes are NOT sorted!
				"<p>Press button <ph keyref=\"btn_success\"></ph> and ...</p>" + 
				"<p>Press button <ph>This is not a keyref</ph> and ...</p>" +
				"</section></document>");
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
		}, p);
		
		IndexingDoc fields = mock(IndexingDoc.class);
		when(fields.getFieldValue("source")).thenReturn(
				"<document xml:lang=\"en\">\n" +
				"<!-- A comment. -->" +
				"<section><title>No<?Pub _hardspace?>Break</title>\n" +
				"<p><?Pub _font FontColor=\"red\" SmallCap=\"yes\"\n" +
				"?>Specific text touchup.<?Pub /_font?></p>\n" +
				"</section>\n" +					
				"</document>");
		
		x.end(null, null, fields);
		verify(fields).addField("words_text", "5");
		verify(fields).addField("text", "No Break Specific text touchup.");
		verify(fields).addField("source_reuse", "<document><section><title>No<?Pub _hardspace?>Break</title><p><?Pub _font FontColor=\"red\" SmallCap=\"yes\"?>Specific text touchup.<?Pub /_font?></p></section></document>");
	}

	/**
	 * Copy of testProcessInstruction() but with complexity of RIDs (caused by old issue with Abx DOM)
	 */
	@Test
	public void testProcessInstructionRid() {
		XmlIndexFieldExtraction x = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
				assertNotNull("Should find an xsl file to test with", xsl);
				return new StreamSource(xsl);
			}
		}, p);
		
		IndexingDoc fields = mock(IndexingDoc.class);
		when(fields.getFieldValue("source")).thenReturn(
				"<document xml:lang=\"en\">\n" +
				"<!-- A comment. -->" +
				"<section><title>No<?Pub _hardspace cms:rid=\"2hf3j2tpqwv002a\"?>Break</title>\n" +
				"<p><?Pub _font FontColor=\"red\" SmallCap=\"yes\"\n" +
				"cms:rid=\"2hf3j2tpqwv002b\"?>Specific text touchup.<?Pub /_font?></p>\n" +
				"</section>\n" +					
				"</document>");
		
		x.end(null, null, fields);
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
		}, p);
		
		
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
		
		x.end(null, null, fields);
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
		}, p);
		
		
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
		
		x.end(null, null, fields);
		assertEquals("section & stuff Testing cms attributes including tvalidate. Title Figure", fields.getFieldValue("text"));
		assertEquals("<document><section><title>section & stuff</title><p>Testing cms attributes including tvalidate.</p></section><figure cms:tvalidate=\"no\"><title>Title</title>Figure</figure></document>", fields.getFieldValue("source_reuse"));
		assertEquals("10", fields.getFieldValue("words_text"));
	}
	
	/**
	 * Tests consequences of having a different prefix assigned to the CMS namespace.
	 * The output in source_reuse will normalize to 'cms', but there is no general support.
	 * The source field will contain the original.
	 * Interesting transform:
	 * http://lenzconsulting.com/namespace-normalizer/
	 */
	@Test
	public void testAttributesCmsPrefixNormalize() {
		XmlIndexFieldExtraction x = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
				assertNotNull("Should find an xsl file to test with", xsl);
				return new StreamSource(xsl);
			}
		}, p);
		
		
		IndexingDoc fields =  new IndexingDocIncrementalSolrj();
		fields.setField("source",
				"<document xmlns:odd=\"http://www.simonsoft.se/namespace/cms\" xml:lang=\"en\" odd:rwords=\"10\" odd:rlogicalid=\"xy1\" odd:rid=\"abc001\" odd:twords=\"15\">\n" +
				"<section odd:rlogicalid=\"xy2\" odd:rid=\"abc002\">\n" +
				"<title odd:rid=\"abc003\">section &amp; stuff</title>\n" +
				"<p odd:rid=\"abc004\" odd:tstatus=\"Released\" odd:tlogicalid=\"x-svn...\" odd:tmatch=\"element1\" odd:tpos=\"1.x.x\" odd:trid=\"xyz006\" odd:twords=\"5\">Testing cms attributes\n" +
				"including tvalidate.</p>\n" +
				"</section>\n" +
				"<figure odd:rid=\"abc005\" odd:tvalidate=\"no\"><title odd:rid=\"abc006\">Title</title>Figure</figure>\n" +						
				"</document>");
		
		x.end(null, null, fields);
		assertEquals("section & stuff Testing cms attributes including tvalidate. Title Figure", fields.getFieldValue("text"));
		assertEquals("<document><section><title>section & stuff</title><p>Testing cms attributes including tvalidate.</p></section><figure cms:tvalidate=\"no\"><title>Title</title>Figure</figure></document>", fields.getFieldValue("source_reuse"));
		assertEquals("10", fields.getFieldValue("words_text"));
		// Are we currently (Java extraction of a_*) normalizing the cms namespace?
		/*
		assertEquals("xy1", fields.getFieldValue("a_cms.rlogicalid"));
		*/
	}
	
	@Test (expected=Exception.class) 
	public void testAttributesCmsPrefixOccupied() {
		XmlIndexFieldExtraction x = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
				assertNotNull("Should find an xsl file to test with", xsl);
				return new StreamSource(xsl);
			}
		}, p);
		
		
		IndexingDoc fields =  new IndexingDocIncrementalSolrj();
		fields.setField("source",
				"<document xmlns:odd=\"http://www.simonsoft.se/namespace/cms\" xmlns:cms=\"http://www.simonsoft.se/namespace/crap\" xml:lang=\"en\" odd:rwords=\"10\" odd:rlogicalid=\"xy1\" odd:rid=\"abc001\" odd:twords=\"15\">\n" +
				"<section odd:rlogicalid=\"xy2\" odd:rid=\"abc002\">\n" +
				"<title odd:rid=\"abc003\" cms:foo=\"unknown\">section &amp; stuff</title>\n" +
				"<p odd:rid=\"abc004\" odd:tstatus=\"Released\" odd:tlogicalid=\"x-svn...\" odd:tmatch=\"element1\" odd:tpos=\"1.x.x\" odd:trid=\"xyz006\" odd:twords=\"5\">Testing cms attributes\n" +
				"including tvalidate.</p>\n" +
				"</section>\n" +
				"<figure odd:rid=\"abc005\" odd:tvalidate=\"no\"><title odd:rid=\"abc006\">Title</title>Figure</figure>\n" +						
				"</document>");
		
		x.end(null, null, fields);

	}
	
	
	@Test
	public void testPretranslateDisqualifyOnDuplicateRid() {
		XmlIndexFieldExtraction x = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
				assertNotNull("Should find an xsl file to test with", xsl);
				return new StreamSource(xsl);
			}
		}, p);
		
		
		IndexingDoc fields =  new IndexingDocIncrementalSolrj();
		fields.setField("source",
				"<document xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" xml:lang=\"en\" cms:rwords=\"10\" cms:rlogicalid=\"xy1\" cms:rid=\"abc001\">\n" +
				"<section cms:rlogicalid=\"xy2\" cms:rid=\"abc002\">\n" +
				"<title cms:rid=\"abc003\">section title</title>\n" +
				"<p cms:rid=\"abc004\">Testing cms attributes\n" +
				"including tvalidate.</p>\n" +
				"<p cms:rid=\"abc004\">Duplicate RID.</p>\n" +
				"</section>\n" +
				"<figure cms:rid=\"abc005\"><title cms:rid=\"abc006\">Title</title>Figure</figure>\n" +						
				"</document>");
		
		fields.addField("flag", "hasridduplicate"); // Must flag the duplicate rid, done by repositem extraction.
		x.end(null, null, fields);
		
		XmlIndexFieldExtraction r = new XmlIndexRidDuplicateDetection();
		r.end(null, null, fields);
		
		assertEquals("section title Testing cms attributes including tvalidate. Duplicate RID. Title Figure", fields.getFieldValue("text"));
		assertEquals("<document><section><title>section title</title><p>Testing cms attributes including tvalidate.</p><p>Duplicate RID.</p></section><figure><title>Title</title>Figure</figure></document>", fields.getFieldValue("source_reuse"));
		assertEquals("11", fields.getFieldValue("words_text"));
		assertEquals("abc004 abc004", fields.getFieldValue("reuseridduplicate"));
		assertEquals(-5, fields.getFieldValue("reusevalue")); // Requires the flag to be set by repositem extract.
	}
	
	
	/**
	 * There is a dangerous situation illustrated by the p r02b which has RID but below a disqualified element.
	 * That p would likely be available for pretranslate. 
	 */
	@Test
	public void testPretranslateDisqualifyOnRemovedRid() {
		XmlIndexFieldExtraction x = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
				assertNotNull("Should find an xsl file to test with", xsl);
				return new StreamSource(xsl);
			}
		}, p);		
		
		IndexingDoc root = new IndexingDocIncrementalSolrj();
		root.setField("source",
				"<document xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rlogicalid=\"xy1\" cms:rid=\"r01\">\n" +
				"<section cms:rlogicalid=\"xy2\" ><p cms:rid=\"r02b\">section</p></section>\n" +
				"<figure cms:rlogicalid=\"xy3\" cms:rid=\"r03\"><title>Title</title>Figure</figure>\n" +						
				"</document>");
		root.setField("prop_cms.status", "Released");
		
		x.end(null, null, root);
		assertEquals("a child is disqualified from reuse so this element has to be too", "-3", root.getFieldValue("reusevalue"));
		assertEquals("the element is status=Released so reuseready is not affected", "1", root.getFieldValue("reuseready"));
		
		IndexingDoc norid = new IndexingDocIncrementalSolrj();
		norid.setField("source",
				"<section xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rlogicalid=\"xy2\" >section</section>");
		norid.setField("prop_cms.status", "Released");
		
		x.end(null, null, norid);
		assertEquals("-2", norid.getFieldValue("reusevalue"));
		
		IndexingDoc sibling = new IndexingDocIncrementalSolrj();
		sibling.setField("source",
				"<figure xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rlogicalid=\"xy3\" cms:rid=\"r03\"><title>Title</title>Figure</figure>");
		sibling.setField("prop_cms.status", "Released");
		
		x.end(null, null, sibling);
		assertEquals("1", sibling.getFieldValue("reusevalue"));
		assertEquals("1", sibling.getFieldValue("reuseready"));
		
	}
	
	/**
	 * More modern variant of removing RID (on node with rlogicalid).
	 * Can be done on any node and should disqualify both parents and children.
	 */
	@Test
	public void testPretranslateDisqualifyOnTsuppress() {
		XmlIndexFieldExtraction x = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
				assertNotNull("Should find an xsl file to test with", xsl);
				return new StreamSource(xsl);
			}
		}, p);		
		
		IndexingDoc root = new IndexingDocIncrementalSolrj();
		root.setField("source",
				"<document xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rlogicalid=\"xy1\" cms:rid=\"r01\">\n" +
				"<section cms:rlogicalid=\"xy2\" cms:rid=\"r02\" cms:tsuppress=\"yes\"><p cms:rid=\"r02b\">section</p></section>\n" +
				"<figure cms:rlogicalid=\"xy3\" cms:rid=\"r03\"><title>Title</title>Figure</figure>\n" +						
				"</document>");
		root.setField("prop_cms.status", "Released");
		
		x.end(null, null, root);
		assertEquals("a child is disqualified from reuse so this element has to be too", "-4", root.getFieldValue("reusevalue"));
		assertEquals("the element is status=Released so reuseready is not affected", "1", root.getFieldValue("reuseready"));
		
		IndexingDoc syes = new IndexingDocIncrementalSolrj();
		syes.setField("source",
				"<section xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rlogicalid=\"xy2\" cms:rid=\"r02\" cms:tsuppress=\"yes\"><p cms:rid=\"r02b\">section</p></section>");
		syes.setField("prop_cms.status", "Released");
		
		x.end(null, null, syes);
		assertEquals("the suppressed element itself is disqualified" ,"-4", syes.getFieldValue("reusevalue"));
		
		IndexingDoc sno = new IndexingDocIncrementalSolrj();
		sno.setField("source",
				"<section xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rlogicalid=\"xy2\" cms:rid=\"r02\" cms:tsuppress=\"no\"><p cms:rid=\"r02b\">section</p></section>");
		sno.setField("prop_cms.status", "Released");
		
		x.end(null, null, sno);
		assertEquals("tsuppress attr can be set to 'no', no disqualification" ,"1", sno.getFieldValue("reusevalue"));
		
		//Verify that all children of tsuppress:ed element is disqualified.
		IndexingDoc sya = new IndexingDocIncrementalSolrj();
		sya.setField("source",
				"<p xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rid=\"r02b\">anything</p>");
		sya.setField("prop_cms.status", "Released");
		sya.setField("ins_cms", "http://www.simonsoft.se/namespace/cms");
		sya.setField("aa_cms.tsuppress", "whatever");
		
		x.end(null, null, sya);
		assertEquals("the children of suppressed element is disqualified" ,"-5", sya.getFieldValue("reusevalue"));
		
	}
	
	/**
	 * tvalidate='no' can be set on a node to suppress validation.
	 * Pretranslate should be done on that node (with limited validation) but not below.
	 * Can be done on any node and should disqualify children only (neither the element nor parents).
	 */
	@Test
	public void testPretranslateDisqualifyOnTvalidate() {
		XmlIndexFieldExtraction x = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
				assertNotNull("Should find an xsl file to test with", xsl);
				return new StreamSource(xsl);
			}
		}, p);		
		
		IndexingDoc root = new IndexingDocIncrementalSolrj();
		root.setField("source",
				"<document xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rlogicalid=\"xy1\" cms:rid=\"r01\">\n" +
				"<section cms:rlogicalid=\"xy2\" cms:rid=\"r02\" cms:tvalidate=\"yes\"><p cms:rid=\"r02b\">section</p></section>\n" +
				"<figure cms:rlogicalid=\"xy3\" cms:rid=\"r03\"><title>Title</title>Figure</figure>\n" +						
				"</document>");
		root.setField("prop_cms.status", "Released");
		
		x.end(null, null, root);
		assertEquals("a child is tvalidate=no, parent unaffected", "1", root.getFieldValue("reusevalue"));
		
				
		IndexingDoc sno = new IndexingDocIncrementalSolrj();
		sno.setField("source",
				"<section xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rlogicalid=\"xy2\" cms:rid=\"r02\" cms:tvalidate=\"no\"><p cms:rid=\"r02b\">section</p></section>");
		sno.setField("prop_cms.status", "Released");
		
		x.end(null, null, sno);
		assertEquals("tvalidate=no of element itself, no disqualification" ,"1", sno.getFieldValue("reusevalue"));
		
		//Verify that all children of tvalidate=no element is disqualified.
		IndexingDoc sya = new IndexingDocIncrementalSolrj();
		sya.setField("source",
				"<p xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rid=\"r02b\">anything</p>");
		sya.setField("prop_cms.status", "Released");
		sya.setField("ins_cms", "http://www.simonsoft.se/namespace/cms");
		sya.setField("aa_cms.tvalidate", "no");
		
		x.end(null, null, sya);
		assertEquals("the children of tvalidate=no element is disqualified" ,"-7", sya.getFieldValue("reusevalue"));
		
	}
	
	/**
	 * Similar to tsuppress but driven by DTD-specific attributes and not propagating upwards.
	 */
	@Test
	public void testPretranslateDisqualifyOnMarkfortranslateNo() {
		XmlIndexFieldExtraction x = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
				assertNotNull("Should find an xsl file to test with", xsl);
				return new StreamSource(xsl);
			}
		}, p);		
		
		IndexingDoc root = new IndexingDocIncrementalSolrj();
		root.setField("source",
				"<document xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rlogicalid=\"xy1\" cms:rid=\"r01\">\n" +
				"<section cms:rlogicalid=\"xy2\" cms:rid=\"r02\" markfortrans=\"no\"><p cms:rid=\"r02b\">section</p></section>\n" +
				"<figure cms:rlogicalid=\"xy3\" cms:rid=\"r03\"><title>Title</title>Figure</figure>\n" +						
				"</document>");
		root.setField("prop_cms.status", "Released");
		
		x.end(null, null, root);
		assertEquals("a child is markfortrans but that does NOT disqualify parent (because markfortrans attribute is included in checksum", "1", root.getFieldValue("reusevalue"));
		assertEquals("the element is status=Released so reuseready is of course ok", "1", root.getFieldValue("reuseready"));
		
		IndexingDoc tno = new IndexingDocIncrementalSolrj();
		tno.setField("source",
				"<section xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rlogicalid=\"xy2\" cms:rid=\"r02\" markfortrans=\"no\"><p cms:rid=\"r02b\">section</p></section>");
		tno.setField("prop_cms.status", "Released");
		
		x.end(null, null, tno);
		assertEquals("the markfortrans element has markfortrans attr in checksum/source_reuse" ,"<section markfortrans=\"no\"><p>section</p></section>", tno.getFieldValue("source_reuse"));
		assertEquals("the markfortrans element is not disqualified since including markfortrans/translate attr in checksum" ,"1", tno.getFieldValue("reusevalue"));
		//assertEquals("the markfortrans element itself is disqualified (can be discussed, alternative would be to include markfortrans/translate attr in checksum)" ,"-20", syes.getFieldValue("reusevalue"));
		
		// Verify markfortrans=yes. Not really core now with the approach of inheriting attribute into source_reuse.
		// We could consider removing markfortrans=yes, but not sure about interaction with defaulting of markfortrans attribute.
		IndexingDoc tyes = new IndexingDocIncrementalSolrj();
		tyes.setField("source",
				"<section xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rlogicalid=\"xy2\" cms:rid=\"r02\" markfortrans=\"yes\"><p cms:rid=\"r02b\">section</p></section>");
		tyes.setField("prop_cms.status", "Released");
		
		x.end(null, null, tyes);
		assertEquals("the markfortrans element has markfortrans attr in checksum/source_reuse" ,"<section markfortrans=\"yes\"><p>section</p></section>", tyes.getFieldValue("source_reuse"));
		assertEquals("markfortrans attr can be set to 'yes', no disqualification" ,"1", tyes.getFieldValue("reusevalue"));
		
		//Verify that all children of markfortrans:ed element is disqualified.
		IndexingDoc tna = new IndexingDocIncrementalSolrj();
		tna.setField("source",
				"<p xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:rid=\"r02b\">anything</p>");
		tna.setField("prop_cms.status", "Released");
		tna.setField("aa_markfortrans", "no");
		
		x.end(null, null, tna);
		assertEquals("the child of markfortrans:ed element has inherited markfortrans in checksum/source_reuse" ,"<p markfortrans=\"no\">anything</p>", tna.getFieldValue("source_reuse"));
		assertEquals("the child of markfortrans:ed element is not disqualified, inherited attr instead" ,"1", tna.getFieldValue("reusevalue"));
		
	}
	
	@Test
	public void testPretranslateDisqualifyOnStatus() {
		XmlIndexFieldExtraction x = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
				assertNotNull("Should find an xsl file to test with", xsl);
				return new StreamSource(xsl);
			}
		}, p);	
		
		IndexingDoc doc2 = new IndexingDocIncrementalSolrj();
		doc2.addField("prop_cms.status", "In_Translation");
		doc2.addField("source", "<doc/>");
		
		x.end(null, null, doc2);
		assertEquals("-2", doc2.getFieldValue("reusevalue"));
		assertEquals("status is not released", "0", doc2.getFieldValue("reuseready"));
		
		IndexingDoc doc3 = new IndexingDocIncrementalSolrj();
		doc3.setField("prop_cms.status", null);
		doc3.setField("source", "<doc/>");
		x.end(null, null, doc3);
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
		
		XmlIndexFieldExtraction xsl = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSourceDefault(), p);
		
		IndexingDoc fields = mock(IndexingDoc.class);
		when(fields.getFieldValue("source")).thenReturn(element);
		
		try {
			xsl.end(null, null, fields);
			fail("Should throw declared exception for xml error");
		} catch (XmlNotWellFormedException e) { // any other exception would abort indexing
			// expected
		}
	}

}
