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
package se.simonsoft.xmltracking.index.add;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;

import se.simonsoft.cms.indexing.IndexFields;
import se.simonsoft.xmltracking.source.XmlSourceAttribute;
import se.simonsoft.xmltracking.source.XmlSourceElement;

public class XmlSourceHandlerSolrjTest {

	@SuppressWarnings("serial")
	@Test
	public void test() throws SolrServerException, IOException {
		
		XmlSourceElement e1 = new XmlSourceElement("document",
				Arrays.asList(new XmlSourceAttribute("cms:status", "In_Work"),
						new XmlSourceAttribute("xml:lang", "en")), 
				"<document cms:status=\"In_Work\" xml:lang=\"en\">\n" +
				"<section cms:component=\"xyz\" cms:status=\"Released\">section</section>\n" +
				"<figure cms:component=\"xz0\"><title>Title</title>Figure</figure>\n" +						
				"</document>")
				.setDepth(1, null).setPosition(1, null);
		
		XmlSourceElement e2 = new XmlSourceElement("section",
				Arrays.asList(new XmlSourceAttribute("cms:component", "xyz"),
						new XmlSourceAttribute("cms:status", "Released")),
				"<section cms:component=\"xyz\" cms:status=\"Released\">section</section>")
				.setDepth(2, e1).setPosition(1, null);

		XmlSourceElement e3 = new XmlSourceElement("figure",
				Arrays.asList(new XmlSourceAttribute("cms:component", "xz0")),
				"<figure cms:component=\"xz0\"><title>Title</title>Figure</figure>")
				.setDepth(2, e1).setPosition(2, e2);
		
		XmlSourceElement e4 = new XmlSourceElement("title",
				new LinkedList<XmlSourceAttribute>(),
				"<title>Title</title>")
				.setDepth(3, e3).setPosition(1, null);
		
		IdStrategy idStrategy = mock(IdStrategy.class);
		when(idStrategy.getElementId(e1)).thenReturn("testdoc1_e1");
		when(idStrategy.getElementId(e2)).thenReturn("testdoc1_e2");
		when(idStrategy.getElementId(e3)).thenReturn("testdoc1_e3");
		when(idStrategy.getElementId(e4)).thenReturn("testdoc1_e4");
		
		IndexFieldExtraction extractor1 = mock(IndexFieldExtraction.class);
		IndexFieldExtraction extractor2 = mock(IndexFieldExtraction.class);
		LinkedHashSet<IndexFieldExtraction> extractors = new LinkedHashSet<IndexFieldExtraction>();
		extractors.add(extractor1);
		extractors.add(extractor2);		
		
		SolrServer solrServer = mock(SolrServer.class);
		
		XmlSourceHandlerSolrj handler = new XmlSourceHandlerSolrj(solrServer, idStrategy) {
			@Override protected void fieldCleanupTemporary(IndexFieldsSolrj doc) {}
		};
		handler.setFieldExtraction(extractors);
		
		handler.startDocument();
		verify(idStrategy).start();
		handler.begin(e1);
		verify(extractor1).extract(any(IndexFields.class), (XmlSourceElement) isNull()); // still not sure we want to pass the xml indexing specific data
		verify(extractor2).extract(any(IndexFields.class), (XmlSourceElement) isNull());
		handler.begin(e2);
		handler.begin(e3);
		handler.begin(e4);
		// note: update times() below if elements are added to the test
		handler.endDocument();
		verify(solrServer, times(1)).commit();
		
		verify(solrServer, times(4)).add(new SolrInputDocument() {
			@Override
			public boolean equals(Object obj) {
				System.out.println("Got " + obj);
				SolrInputDocument doc = (SolrInputDocument) obj;
				assertTrue("id must be set", doc.containsKey("id"));
				String id = doc.getFieldValue("id").toString();
				assertTrue("name must be set", doc.containsKey("name"));
				if ("testdoc1_e1".equals(id)) {
					assertEquals("document", doc.getFieldValue("name"));
					// TODO after we use actual XML file//assertEquals("We shouln't index (or store) source of root elements", null, doc.getFieldValue("source"));
					// assumption made about SchemaFieldName impl
					assertTrue("Should contain the attribute name prefixed with a_ as field",
							doc.containsKey("a_cms:status"));
					assertEquals("In_Work", doc.getFieldValue("a_cms:status").toString());
					assertEquals("en", doc.getFieldValue("a_xml:lang").toString());
					// additional names
					assertEquals("document", doc.getFieldValue("rname"));
					assertEquals("parent name should be null for root", null, doc.getFieldValue("pname"));
					assertEquals("ancestor names should exclude self", null, doc.getFieldValues("aname")); // todo empty list in response?
					assertEquals(null, doc.getFieldValues("aname"));
					// additional attributes
					assertEquals("In_Work", doc.getFieldValue("ra_cms:status").toString());
					assertEquals("In_Work", doc.getFieldValue("ia_cms:status").toString());
					assertEquals("en", doc.getFieldValue("ra_xml:lang").toString());
					assertEquals("en", doc.getFieldValue("ia_xml:lang").toString());
					assertNull(doc.getFieldValue("cms:component"));
					assertEquals(1, doc.getFieldValue("depth"));
					assertEquals(1, doc.getFieldValue("position"));
					assertEquals(null, doc.getFieldValue("sname"));
				} else if ("testdoc1_e2".equals(id)) {
					assertEquals("section", doc.getFieldValue("name"));
					assertEquals("document", doc.getFieldValue("rname"));
					assertEquals("document", doc.getFieldValue("pname"));
					assertEquals(1, doc.getFieldValues("aname").size());
					assertEquals("document", doc.getFieldValues("aname").iterator().next());
					assertTrue(doc.getFieldValue("source").toString().startsWith("<section"));
					assertEquals("xyz", doc.getFieldValue("a_cms:component").toString());
					assertEquals("Released", doc.getFieldValue("a_cms:status").toString());
					assertEquals("Released", doc.getFieldValue("ia_cms:status").toString());
					assertEquals("In_Work", doc.getFieldValue("ra_cms:status").toString());
					assertEquals("en", doc.getFieldValue("ia_xml:lang").toString());
					assertEquals("en", doc.getFieldValue("ra_xml:lang").toString());
					assertEquals(null, doc.getFieldValue("a_xml:lang"));
					assertEquals("xyz", doc.getFieldValue("ia_cms:component"));
					assertEquals(null, doc.getFieldValue("ra_cms:component"));
					assertEquals(2, doc.getFieldValue("depth"));
					assertEquals(1, doc.getFieldValue("position"));
					assertEquals(null, doc.getFieldValue("sname"));
				} else if ("testdoc1_e3".equals(id)) {
					assertEquals("figure", doc.getFieldValue("name"));
					assertTrue(doc.getFieldValue("source").toString().startsWith("<figure"));
					assertEquals("xz0", doc.getFieldValue("a_cms:component"));
					assertEquals(null, doc.getFieldValue("a_cms:status"));
					assertEquals("In_Work", doc.getFieldValue("ia_cms:status"));
					assertEquals("In_Work", doc.getFieldValue("ra_cms:status"));
					assertEquals("en", doc.getFieldValue("ia_xml:lang"));
					assertEquals("en", doc.getFieldValue("ra_xml:lang"));
					assertEquals(null, doc.getFieldValue("a_xml:lang"));
					assertEquals("xz0", doc.getFieldValue("ia_cms:component"));
					assertEquals(null, doc.getFieldValue("ra_cms:component"));
					assertEquals(2, doc.getFieldValue("depth"));
					assertEquals(2, doc.getFieldValue("position"));
					assertEquals("section", doc.getFieldValue("sname"));
					assertEquals("xyz", doc.getFieldValue("sa_cms:component"));
				} else if ("testdoc1_e4".equals(id)) {
					assertEquals("title", doc.getFieldValue("name"));
					assertTrue("source must be set, at least for elements like title", doc.containsKey("source"));
					assertTrue(doc.getFieldValue("source").toString().startsWith("<title"));
					assertEquals("figure", doc.getFieldValue("pname"));
					assertEquals("document", doc.getFieldValue("rname"));
					Iterator<Object> a = doc.getFieldValues("aname").iterator();
					assertEquals("ancestor names should be ordered from top", "document", a.next());
					assertEquals("all ancestors should be there", "figure", a.next());
					assertFalse("ancestors should not include self", a.hasNext());
					assertEquals(1, doc.getFieldValue("position"));
					assertEquals(3, doc.getFieldValue("depth"));
					assertEquals(null, doc.getFieldValues("sname"));
					assertEquals("xz0", doc.getFieldValue("ia_cms:component"));
					assertEquals(null, doc.getFieldValue("sa_cms:component"));
				} else {
					fail("Unexpected id " + id);
				}
				return true; // we should have failed assertions on unexpected docs
			}
		});
	}

	public void testExtractors() {
		IndexFieldExtraction x1 = mock(IndexFieldExtraction.class);
		
	}
	
}
