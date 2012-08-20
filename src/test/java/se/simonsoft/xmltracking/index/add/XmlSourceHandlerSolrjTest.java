package se.simonsoft.xmltracking.index.add;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.junit.Test;

import se.simonsoft.xmltracking.source.XmlSourceAttribute;
import se.simonsoft.xmltracking.source.XmlSourceElement;
import se.simonsoft.xmltracking.source.XmlSourceReader;
import se.simonsoft.xmltracking.source.jdom.XmlSourceReaderJdom;

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
		
		XmlSourceHandlerSolrj handler = new XmlSourceHandlerSolrj(solrServer, idStrategy);
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
				assertTrue("source must be set", doc.containsKey("source"));
				if ("testdoc1_e1".equals(id)) {
					assertEquals("document", doc.getFieldValue("name"));
					assertTrue(doc.getFieldValue("source").toString().startsWith("<document"));
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
	
	//@Ignore // requires local test server
	@Test
	public void testIntegration() throws MalformedURLException {
		SolrServer server = new CommonsHttpSolrServer("http://localhost:8080/solr/reposxml/");
		
		// like the test above but with real server
		
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
		
		XmlSourceHandlerSolrj handler = new XmlSourceHandlerSolrj(server, idStrategy);
		
		handler.startDocument();
		verify(idStrategy).start();
		handler.begin(e1);
		handler.begin(e2);
		handler.begin(e3);
		handler.begin(e4);
		handler.endDocument();
		
		// TODO assertions, currently manual
	}
	
	//@Ignore // requires local test server
	@Test
	public void testIntegrationWithXmlSourceReader() throws MalformedURLException, SolrServerException {
		SolrServer server = new CommonsHttpSolrServer("http://localhost:8080/solr/reposxml/");
		
		XmlSourceReader reader = new XmlSourceReaderJdom();
		
		// actual documents
		final List<String> testfiles = Arrays.asList(
				"se/simonsoft/xmltracking/source/test1.xml",
				"se/simonsoft/xmltracking/reuse/900108_A.xml",
				"se/simonsoft/xmltracking/reuse/900108_B.xml",
				"se/simonsoft/xmltracking/reuse/900108_A_de-DE_Released.xml");
		
		final String testid = "test" + System.currentTimeMillis();
		
		IdStrategy testIdStrategy = new IdStrategy() {
			int filenum = 0;
			String file = null;
			int count = 0;
			@Override public void start() {
				count = 0;
				file = testfiles.get(filenum++);
			}
			@Override
			public String getElementId(XmlSourceElement element) {
				return testid + "_" + file + "_" + count++;
			}
		};
		
		XmlSourceHandlerSolrj handler = new XmlSourceHandlerSolrj(server, testIdStrategy);
			
		for (String testfile : testfiles) {
			InputStream f = this.getClass().getClassLoader().getResourceAsStream(testfile);
			reader.read(f, handler);
		}
		
		// assert cms:rid match
		SolrParams q1 = new SolrQuery("a_cms\\:rid:1wj1mp4dmcb0007")
			.addFilterQuery("id:" + testid + "_*")
			.addSortField("ia_xml:lang", ORDER.asc);
		QueryResponse r1 = server.query(q1);
		assertEquals(2, r1.getResults().size());
		SolrDocument r1en = r1.getResults().get(1);
		SolrDocument r1de = r1.getResults().get(0);
		assertEquals("en-GB", r1en.getFieldValue("ia_xml:lang"));
		assertEquals("de-DE", r1de.getFieldValue("ia_xml:lang"));
		
		// test aname
		SolrParams q2 = new SolrQuery("aname:frontm AND aname:note AND ra_cms\\:rid:1wj1mp4dmcb0000")
			.addFilterQuery("id:" + testid + "_*");
		assertEquals(2, server.query(q2).getResults().size());
		
		// test logical id match, with depth although not needed
		SolrParams q3 = new SolrQuery("a_cms\\:rlogicalid:x-svn\\:///svn/demo1\\^/vvab/xml/sections/INTRODUCTION.xml?p=129 AND depth:3")
			.addFilterQuery("id:" + testid + "_*")
			.addSortField("ia_xml:lang", ORDER.asc);
		SolrDocumentList r3 = server.query(q3).getResults();
		assertEquals(2, r3.size());
		assertEquals("en-GB", r1en.getFieldValue("ia_xml:lang"));
		assertEquals("de-DE", r1de.getFieldValue("ia_xml:lang"));
		
		// Test content match
		SolrParams q4c = new SolrQuery(
				"name:title AND depth:5" // narrow the search, for performance maybe
				+ " AND a_cms\\:rid:[* TO *]"// require existence of rid so we only get finalized releases
				// match directly on source until we have digest strategies (heavy escaping would be needed though)
				// we also need to handle the newline before the last word
				//source:<title*cms\:rid=*>Before\ the\ seed\ drill\ is\ put\ into*operation</title>
				+ " AND source:<title*>Before\\ the\\ seed\\ drill\\ is\\ put\\ into*operation</title>"
				// master language
				+ " AND ia_xml\\:lang:en-GB"
				)
				.addFilterQuery("id:" + testid + "_*")
				// sort newest first, revision field?
				.setSortField("ra_revision", ORDER.desc);
		System.out.println("Query: " + q4c.get("q"));
		SolrDocumentList r4c = server.query(q4c).getResults();
		assertTrue(r4c.size() >= 1);
		SolrDocument maybeTranslated1 = r4c.get(0);
		assertEquals("title", maybeTranslated1.getFieldValue("name"));
		String rid = maybeTranslated1.getFieldValue("a_cms:rid").toString();
		assertNotNull("Expecting a match in this test set that has a cms:rid", rid);
		assertEquals("The match should be in the previous release", "1wj1mp4dmcb000f", rid);
		
		// Now search on this matching content for translations
		SolrParams q4t = new SolrQuery("a_cms\\:rid:" + rid
				+ " AND -ia_xml\\:lang:en-GB"
				// word count: no need to filter on status
				// replace: get imported translations only
				)
				.addFilterQuery("id:" + testid + "_*")
				;
		
		SolrDocumentList r4t = server.query(q4t).getResults();
		assertEquals("Should have found one translation of the cms:rid in this test set", 1, r4t.size());
		SolrDocument r4tde = r4t.get(0);
		assertEquals("de-DE", r4tde.getFieldValue("ra_xml:lang"));
	}
	
}
