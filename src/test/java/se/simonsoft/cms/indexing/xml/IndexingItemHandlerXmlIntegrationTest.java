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
package se.simonsoft.cms.indexing.xml;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrDocumentList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import se.repos.testing.indexing.SvnTestIndexing;
import se.repos.testing.indexing.TestIndexOptions;
import se.simonsoft.cms.backend.filexml.CmsRepositoryFilexml;
import se.simonsoft.cms.backend.filexml.FilexmlRepositoryReadonly;
import se.simonsoft.cms.backend.filexml.FilexmlSourceClasspath;
import se.simonsoft.cms.backend.filexml.testing.ReposTestBackendFilexml;
import se.simonsoft.cms.indexing.xml.fields.IndexFieldDeletionsToSaveSpace;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldElement;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldExtractionChecksum;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexIdAppendTreeLocation;
import se.simonsoft.cms.indexing.xml.solr.XmlIndexWriterSolrj;
import se.simonsoft.xmltracking.source.XmlSourceReader;
import se.simonsoft.xmltracking.source.jdom.XmlSourceReaderJdom;
import se.simonsoft.xmltracking.source.saxon.IndexFieldExtractionCustomXsl;
import se.simonsoft.xmltracking.source.saxon.XmlMatchingFieldExtractionSourceDefault;

public class IndexingItemHandlerXmlIntegrationTest {

	/// test framework from cms-backend-svnkit
	
	private SvnTestIndexing indexing = null;

	/**
	 * Manual dependency injection. We should think twice before we copy this config to other tests,
	 * because as soon as we have >1 integration tests it will be a maintenance problem if we for example add handlers.
	 */
	@Before
	public void setUpIndexing() {
		TestIndexOptions indexOptions = new TestIndexOptions().itemDefaults();
		indexOptions.addCore("reposxml", "se/simonsoft/cms/indexing/xml/solr/reposxml/**");
		indexing = SvnTestIndexing.getInstance(indexOptions);
		SolrServer reposxml = indexing.getCore("reposxml");
		
		XmlSourceReader xmlReader = new XmlSourceReaderJdom();
		XmlIndexWriter indexWriter = new XmlIndexWriterSolrj(reposxml);
		Set<XmlIndexFieldExtraction> fe = new LinkedHashSet<XmlIndexFieldExtraction>();
		fe.add(new XmlIndexIdAppendTreeLocation());
		fe.add(new XmlIndexFieldElement());
		fe.add(new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSourceDefault()));
		fe.add(new XmlIndexFieldExtractionChecksum());
		fe.add(new IndexFieldDeletionsToSaveSpace());
		
		IndexingItemHandlerXml handlerXml = new IndexingItemHandlerXml();
		handlerXml.setDependenciesIndexing(indexWriter);
		handlerXml.setDependenciesXml(fe, xmlReader);
		
		indexOptions.addHandler(handlerXml);
	}
	
	@After
	public void tearDown() throws IOException {
		indexing.tearDown();
	}
	
	@Test
	public void testTinyInline() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-inline");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/testaut1", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
		
		SolrServer reposxml = indexing.getCore("reposxml");
		SolrDocumentList x1 = reposxml.query(new SolrQuery("*:*")).getResults();
		assertEquals(4, x1.getNumFound());
	
		// now produce more revisions
		
	}

//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	@Test
//	public void test() throws SolrServerException, IOException {
//		
//		XmlSourceElement e1 = new XmlSourceElement("document",
//				Arrays.asList(new XmlSourceAttribute("cms:status", "In_Work"),
//						new XmlSourceAttribute("xml:lang", "en")), 
//				"<document cms:status=\"In_Work\" xml:lang=\"en\">\n" +
//				"<section cms:component=\"xyz\" cms:status=\"Released\">section</section>\n" +
//				"<figure cms:component=\"xz0\"><title>Title</title>Figure</figure>\n" +						
//				"</document>")
//				.setDepth(1, null).setPosition(1, null);
//		
//		XmlSourceElement e2 = new XmlSourceElement("section",
//				Arrays.asList(new XmlSourceAttribute("cms:component", "xyz"),
//						new XmlSourceAttribute("cms:status", "Released")),
//				"<section cms:component=\"xyz\" cms:status=\"Released\">section</section>")
//				.setDepth(2, e1).setPosition(1, null);
//
//		XmlSourceElement e3 = new XmlSourceElement("figure",
//				Arrays.asList(new XmlSourceAttribute("cms:component", "xz0")),
//				"<figure cms:component=\"xz0\"><title>Title</title>Figure</figure>")
//				.setDepth(2, e1).setPosition(2, e2);
//		
//		XmlSourceElement e4 = new XmlSourceElement("title",
//				new LinkedList<XmlSourceAttribute>(),
//				"<title>Title</title>")
//				.setDepth(3, e3).setPosition(1, null);
//		
//		IdStrategy idStrategy = mock(IdStrategy.class);
//		when(idStrategy.getElementId(e1)).thenReturn("testdoc1_e1");
//		when(idStrategy.getElementId(e2)).thenReturn("testdoc1_e2");
//		when(idStrategy.getElementId(e3)).thenReturn("testdoc1_e3");
//		when(idStrategy.getElementId(e4)).thenReturn("testdoc1_e4");
//		
//		XmlIndexFieldExtraction extractor1 = mock(XmlIndexFieldExtraction.class);
//		XmlIndexFieldExtraction extractor2 = mock(XmlIndexFieldExtraction.class);
//		LinkedHashSet<XmlIndexFieldExtraction> extractors = new LinkedHashSet<XmlIndexFieldExtraction>();
//		extractors.add(extractor1);
//		extractors.add(extractor2);		
//		
//		SolrServer solrServer = mock(SolrServer.class, withSettings().verboseLogging());
//		
//		XmlSourceHandlerSolrj handler = new XmlSourceHandlerSolrj(solrServer, idStrategy) {
//			@Override protected void fieldCleanupTemporary(IndexingDoc doc) {}
//		};
//		handler.setFieldExtraction(extractors);
//		
//		handler.startDocument(new XmlSourceDoctype("document", "pubID", "sysID"));
//		verify(idStrategy).start();
//		handler.begin(e1);
//		verify(extractor1).extract((XmlSourceElement) isNull(), any(IndexingDoc.class)); // still not sure we want to pass the xml indexing specific data
//		verify(extractor2).extract((XmlSourceElement) isNull(), any(IndexingDoc.class));
//		handler.begin(e2);
//		handler.begin(e3);
//		handler.begin(e4);
//		
//		handler.endDocument();
//		// commit not expected to be done by handler anymore //verify(solrServer, times(1)).commit();
//
//		ArgumentCaptor<List> addcapture = ArgumentCaptor.forClass(List.class);
//		verify(solrServer).add(addcapture.capture());
//		verifyNoMoreInteractions(solrServer);
//		
//		List<SolrInputDocument> added = addcapture.getValue();
//		assertEquals("Should have added all elements", 4, added.size());
//		
//		// first element
//		SolrInputDocument doc = added.get(0);
//		assertEquals("testdoc1_e1", doc.getFieldValue("id"));
//		assertEquals("document", doc.getFieldValue("name"));
//		assertEquals("should index doctype name", "document", doc.getFieldValue("typename"));
//		assertEquals("pubID", doc.getFieldValue("typepublic"));
//		assertEquals("sysID", doc.getFieldValue("typesystem"));
//		// TODO after we use actual XML file//assertEquals("We shouln't index (or store) source of root elements", null, doc.getFieldValue("source"));
//		// assumption made about SchemaFieldName impl
//		assertTrue("Should contain the attribute name prefixed with a_ as field",
//				doc.containsKey("a_cms:status"));
//		assertEquals("In_Work", doc.getFieldValue("a_cms:status").toString());
//		assertEquals("en", doc.getFieldValue("a_xml:lang").toString());
//		// additional names
//		assertEquals("document", doc.getFieldValue("rname"));
//		assertEquals("parent name should be null for root", null, doc.getFieldValue("pname"));
//		assertEquals("ancestor names should exclude self", null, doc.getFieldValues("aname")); // todo empty list in response?
//		assertEquals(null, doc.getFieldValues("aname"));
//		// additional attributes
//		assertEquals("In_Work", doc.getFieldValue("ra_cms:status").toString());
//		assertEquals("In_Work", doc.getFieldValue("ia_cms:status").toString());
//		assertEquals("en", doc.getFieldValue("ra_xml:lang").toString());
//		assertEquals("en", doc.getFieldValue("ia_xml:lang").toString());
//		assertNull(doc.getFieldValue("cms:component"));
//		assertEquals(1, doc.getFieldValue("depth"));
//		assertEquals(1, doc.getFieldValue("position"));
//		assertEquals(null, doc.getFieldValue("sname"));
//		//assertEquals("should not add source for root", null, doc.getFieldValue("source"));
//		assertNotNull("we want to be able to pretranslate on root hits and we didn't have time to fully implemente source retrieval from original document",
//				doc.getFieldValue("source"));
//		
//		// second element
//		doc = added.get(1);
//		assertEquals("testdoc1_e2", doc.getFieldValue("id"));
//		assertEquals("should index doctype for all elements", "document", doc.getFieldValue("typename"));
//		assertEquals("pubID", doc.getFieldValue("typepublic"));
//		assertEquals("sysID", doc.getFieldValue("typesystem"));
//		assertEquals("section", doc.getFieldValue("name"));		
//		assertEquals("document", doc.getFieldValue("rname"));
//		assertEquals("document", doc.getFieldValue("pname"));
//		assertEquals(1, doc.getFieldValues("aname").size());
//		assertEquals("document", doc.getFieldValues("aname").iterator().next());
//		assertTrue(doc.getFieldValue("source").toString().startsWith("<section"));
//		assertEquals("xyz", doc.getFieldValue("a_cms:component").toString());
//		assertEquals("Released", doc.getFieldValue("a_cms:status").toString());
//		assertEquals("Released", doc.getFieldValue("ia_cms:status").toString());
//		assertEquals("In_Work", doc.getFieldValue("ra_cms:status").toString());
//		assertEquals("en", doc.getFieldValue("ia_xml:lang").toString());
//		assertEquals("en", doc.getFieldValue("ra_xml:lang").toString());
//		assertEquals(null, doc.getFieldValue("a_xml:lang"));
//		assertEquals("xyz", doc.getFieldValue("ia_cms:component"));
//		assertEquals(null, doc.getFieldValue("ra_cms:component"));
//		assertEquals(2, doc.getFieldValue("depth"));
//		assertEquals(1, doc.getFieldValue("position"));
//		assertEquals(null, doc.getFieldValue("sname"));
//		
//		// third element
//		doc = added.get(2);
//		assertEquals("testdoc1_e3", doc.getFieldValue("id"));
//		assertEquals("figure", doc.getFieldValue("name"));
//		assertTrue(doc.getFieldValue("source").toString().startsWith("<figure"));
//		assertEquals("xz0", doc.getFieldValue("a_cms:component"));
//		assertEquals(null, doc.getFieldValue("a_cms:status"));
//		assertEquals("In_Work", doc.getFieldValue("ia_cms:status"));
//		assertEquals("In_Work", doc.getFieldValue("ra_cms:status"));
//		assertEquals("en", doc.getFieldValue("ia_xml:lang"));
//		assertEquals("en", doc.getFieldValue("ra_xml:lang"));
//		assertEquals(null, doc.getFieldValue("a_xml:lang"));
//		assertEquals("xz0", doc.getFieldValue("ia_cms:component"));
//		assertEquals(null, doc.getFieldValue("ra_cms:component"));
//		assertEquals(2, doc.getFieldValue("depth"));
//		assertEquals(2, doc.getFieldValue("position"));
//		assertEquals("section", doc.getFieldValue("sname"));
//		assertEquals("xyz", doc.getFieldValue("sa_cms:component"));
//		
//		// fourth element
//		doc = added.get(3);
//		assertEquals("testdoc1_e4", doc.getFieldValue("id"));
//		assertEquals("title", doc.getFieldValue("name"));
//		assertTrue("source must be set, at least for elements like title", doc.containsKey("source"));
//		assertTrue(doc.getFieldValue("source").toString().startsWith("<title"));
//		assertEquals("figure", doc.getFieldValue("pname"));
//		assertEquals("document", doc.getFieldValue("rname"));
//		Iterator<Object> a = doc.getFieldValues("aname").iterator();
//		assertEquals("ancestor names should be ordered from top", "document", a.next());
//		assertEquals("all ancestors should be there", "figure", a.next());
//		assertFalse("ancestors should not include self", a.hasNext());
//		assertEquals(1, doc.getFieldValue("position"));
//		assertEquals(3, doc.getFieldValue("depth"));
//		assertEquals(null, doc.getFieldValues("sname"));
//		assertEquals("xz0", doc.getFieldValue("ia_cms:component"));
//		assertEquals(null, doc.getFieldValue("sa_cms:component"));
//	}
//	
//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	@Test
//	public void testNoreuseOnDisqualifiedChild() throws SolrServerException, IOException {
//		String ns = " xmlns:cms=\"http://www.simonsoft.se/namespace/cms\"";
//		
//		XmlSourceElement e1 = new XmlSourceElement("document",
//				Arrays.asList(new XmlSourceAttribute("cms:rlogicalid", "xy1"),
//						new XmlSourceAttribute("cms:rid", "r01")), 
//				"<document" + ns + " cms:rlogicalid=\"xy1\" cms:rid=\"r01\">\n" +
//				"<section cms:rlogicalid=\"xy2\" >section</section>\n" +
//				"<figure cms:rlogicalid=\"xy3\" cms:rid=\"r03\"><title>Title</title>Figure</figure>\n" +						
//				"</document>")
//				.setDepth(1, null).setPosition(1, null);
//		
//		XmlSourceElement e2 = new XmlSourceElement("section",
//				Arrays.asList(new XmlSourceAttribute("cms:rlogicalid", "xy2")),
//				"<section" + ns + " cms:rlogicalid=\"xy2\">section</section>")
//				.setDepth(2, e1).setPosition(1, null);
//
//		XmlSourceElement e3 = new XmlSourceElement("figure",
//				Arrays.asList(new XmlSourceAttribute("cms:component", "xz0"),
//						new XmlSourceAttribute("cms:rid", "r03")),
//				"<figure" + ns + " cms:rlogicalid=\"xy3\" cms:rid=\"r03\"><title>Title</title>Figure</figure>")
//				.setDepth(2, e1).setPosition(2, e2);
//		
//		XmlSourceElement e4 = new XmlSourceElement("title",
//				new LinkedList<XmlSourceAttribute>(),
//				"<title>Title</title>")
//				.setDepth(3, e3).setPosition(1, null);
//		
//		IdStrategy idStrategy = mock(IdStrategy.class);
//		when(idStrategy.getElementId(e1)).thenReturn("testdoc1_e1");
//		when(idStrategy.getElementId(e2)).thenReturn("testdoc1_e2");
//		when(idStrategy.getElementId(e3)).thenReturn("testdoc1_e3");
//		when(idStrategy.getElementId(e4)).thenReturn("testdoc1_e4");
//	
//		SolrServer solrServer = mock(SolrServer.class);	
//		
//		XmlSourceHandlerSolrj handler = new XmlSourceHandlerSolrj(solrServer, idStrategy) {
//			@Override protected void fieldCleanupTemporary(IndexingDoc doc) {}
//		};
//		
//		// we currently rely on the "custom xsl" extractor for this feature
//		LinkedHashSet<XmlIndexFieldExtraction> extractors = new LinkedHashSet<XmlIndexFieldExtraction>();
//		XmlIndexFieldExtraction x = new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSource() {
//			@Override
//			public Source getXslt() {
//				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
//						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
//				assertNotNull("Should find an xsl file to test with", xsl);
//				return new StreamSource(xsl);
//			}
//		});		
//		extractors.add(x);
//		handler.setFieldExtraction(extractors);
//		
//		handler.startDocument(null);
//		handler.begin(e1);
//		handler.begin(e2);
//		handler.begin(e3);
//		handler.begin(e4);
//		handler.endDocument();
//
//		ArgumentCaptor<List> addcapture = ArgumentCaptor.forClass(List.class);
//		verify(solrServer).add(addcapture.capture());
//		
//		List<SolrInputDocument> added = addcapture.getValue();
//		assertEquals("Should have added all elements", 4, added.size());
//		
//		SolrInputDocument a1 = added.get(0);
//		assertEquals("xy1", a1.getFieldValue("a_cms:rlogicalid"));
//		assertEquals("should flag that a part of the element has ben banned from reuse so that we can keep the architectural promise of assuming all reuse search matches are valid",
//		//		new Integer(-1), (Integer) a1.getFieldValue("reusevalue"));
//				"-1", a1.getFieldValue("reusevalue").toString());
//		SolrInputDocument a3 = added.get(2);
//		assertEquals("the sibling to a banned element should still be ok, but we have no status value here so we must expect 0 instaed of 1",
//		//		new Integer(1), (Integer) a3.getFieldValue("reusevalue"));
//				"0", a3.getFieldValue("reusevalue").toString());
//		
//	}	
	
//	@Test
//	public void testIntegration() throws Exception {
//		
//		XmlSourceElement e1 = new XmlSourceElement("document",
//				Arrays.asList(new XmlSourceNamespace("cms", "http://www.simonsoft.se/namespace/cms")),
//				Arrays.asList(new XmlSourceAttribute("cms:status", "In_Work"),
//						new XmlSourceAttribute("xml:lang", "en")), 
//				"<document xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:status=\"In_Work\" xml:lang=\"en\">\n" +
//				"<section cms:component=\"xyz\" cms:status=\"Released\">section</section>\n" +
//				"<figure cms:component=\"xz0\"><title>Title</title><byline>me</byline></figure>\n" +						
//				"</document>")
//				.setDepth(1, null).setPosition(1, null);
//		
//		XmlSourceElement e2 = new XmlSourceElement("section",
//				Arrays.asList(new XmlSourceAttribute("cms:component", "xyz"),
//						new XmlSourceAttribute("cms:status", "Released")),
//				"<section cms:component=\"xyz\" cms:status=\"Released\">section</section>")
//				.setDepth(2, e1).setPosition(1, null);
//
//		XmlSourceElement e3 = new XmlSourceElement("figure",
//				Arrays.asList(new XmlSourceAttribute("cms:component", "xz0")),
//				"<figure cms:component=\"xz0\"><title>Title</title><byline>me</byline></figure>")
//				.setDepth(2, e1).setPosition(2, e2);
//		
//		XmlSourceElement e4 = new XmlSourceElement("title",
//				new LinkedList<XmlSourceAttribute>(),
//				"<title>Title</title>")
//				.setDepth(3, e3).setPosition(1, null);
//
//		XmlSourceElement e5 = new XmlSourceElement("byline",
//				new LinkedList<XmlSourceAttribute>(),
//				"<byline>me</byline>")
//				.setDepth(3, e3).setPosition(2, e4);		
//		
//		IdStrategy idStrategy = mock(IdStrategy.class);
//		when(idStrategy.getElementId(e1)).thenReturn("testdoc1_e1");
//		when(idStrategy.getElementId(e2)).thenReturn("testdoc1_e2");
//		when(idStrategy.getElementId(e3)).thenReturn("testdoc1_e3");
//		when(idStrategy.getElementId(e4)).thenReturn("testdoc1_e4");
//		when(idStrategy.getElementId(e5)).thenReturn("testdoc1_e5");
//		
//		XmlSourceHandlerSolrj handler = new XmlSourceHandlerSolrj(server, idStrategy);
//
//		// Note that this test currently does not run any extractors so only basic fields will be extracted
//		Set<XmlIndexFieldExtraction> extraction = new HashSet<XmlIndexFieldExtraction>();
//		handler.setFieldExtraction(extraction);
//		
//		handler.startDocument(null);
//		verify(idStrategy).start();
//		handler.begin(e1);
//		handler.begin(e2);
//		handler.begin(e3);
//		handler.begin(e4);
//		handler.begin(e5);
//		handler.endDocument();
//		server.commit();
//		
//		// We could probably do these assertions by mocking solr server, but it wouldn't be easier
//		QueryResponse all = server.query(new SolrQuery("*:*").addSortField("id", ORDER.asc));
//		assertEquals(5, all.getResults().getNumFound());
//		
//		SolrDocument d1 = all.getResults().get(0);
//		assertEquals("should get id from IdStrategy", "testdoc1_e1", d1.get("id"));
//		assertEquals("document", d1.get("name"));
//		assertEquals(1, d1.get("position"));
//		assertEquals(1, d1.get("depth"));
//		assertEquals(null, d1.get("id_p"));
//		assertEquals(null, d1.get("id_s"));
//		assertEquals(d1.get("id"), d1.get("id_r"));
//		assertEquals(null, d1.getFieldValues("id_a"));
//		assertEquals("In_Work", d1.get("a_cms:status"));
//		assertEquals("en", d1.get("a_xml:lang"));
//		assertEquals("should index namespaces", "http://www.simonsoft.se/namespace/cms", d1.get("ns_cms"));
//		assertEquals("inherited namespaces should contains self", "http://www.simonsoft.se/namespace/cms", d1.get("ins_cms"));
//		assertEquals("root", "1", d1.get("pos"));
//		
//		SolrDocument d2 = all.getResults().get(1);
//		assertEquals("section", d2.get("name"));
//		assertEquals(2, d2.get("depth"));
//		assertEquals(d1.get("id"), d2.get("id_p"));
//		assertEquals(null, d2.get("id_s"));
//		assertEquals(d1.get("id"), d2.get("id_r"));
//		assertEquals("document", d2.get("pname"));
//		assertEquals("ns is only those defined on the actual element", null, d2.get("ns_cms"));
//		assertEquals("inherited namespaces", "http://www.simonsoft.se/namespace/cms", d2.get("ins_cms"));
//		assertEquals("1.1", d2.get("pos"));
//		
//		assertEquals(1, d2.getFieldValues("aname").size());
//		assertTrue(d2.getFieldValues("aname").contains("document"));
//		assertFalse(d2.getFieldValues("aname").contains("section"));
//		assertEquals(1, d2.getFieldValues("id_a").size());
//		
//		assertEquals(null, d2.get("a_xml:lang"));
//		assertEquals("en", d2.get("ia_xml:lang"));
//		
//		SolrDocument d3 = all.getResults().get(2);
//		assertEquals(2, d3.get("position"));
//		assertEquals("1.2", d3.get("pos"));
//		assertEquals(d1.get("id"), d3.get("id_p"));
//		assertEquals(d2.get("id"), d3.get("id_s"));
//		assertEquals(d1.get("id"), d3.get("id_r"));
//		assertEquals(1, d3.getFieldValues("id_a").size());
//		assertTrue(d3.getFieldValues("id_a").contains(d1.get("id")));
//		assertEquals("xz0", d3.get("a_cms:component"));
//		
//		SolrDocument d4 = all.getResults().get(3);
//		assertEquals("1.2.1", d4.get("pos"));
//		assertEquals(d3.get("id"), d4.get("id_p"));
//		assertEquals(null, d4.get("id_s"));
//		assertEquals(d1.get("id"), d4.get("id_r"));
//		assertEquals(2, d4.getFieldValues("id_a").size());
//		assertTrue(d4.getFieldValues("id_a").contains(d1.get("id")));
//		assertTrue(d4.getFieldValues("id_a").contains(d3.get("id")));
//		
//		SolrDocument d5 = all.getResults().get(4);
//		assertEquals("1.2.2", d5.get("pos"));
//		
//		// now that we have the data in a test index, test some other queries
//		reuseDataTestJoin();
//	}
//	
//	void reuseDataTestJoin() throws Exception {
//		
//		// find all elements that can be joined with a parent
//		assertJQ(req("q", "{!join from=id to=id_p}*:*", "fl", "id"),
//				"/response=={'numFound':4,'start':0,'docs':[{'id':'testdoc1_e2'},{'id':'testdoc1_e3'},{'id':'testdoc1_e4'},{'id':'testdoc1_e5'}]}");
//
//		// find all elements that have a child
//		assertJQ(req("q", "{!join from=id_p to=id}*:*", "fl", "id"),
//				"/response=={'numFound':2,'start':0,'docs':[{'id':'testdoc1_e1'},{'id':'testdoc1_e3'}]}");
//
//		// find all elements that have a child that is a <title/>
//		assertJQ(req("q", "{!join from=id_p to=id}name:title", "fl", "id"),
//				"/response=={'numFound':1,'start':0,'docs':[{'id':'testdoc1_e3'}]}");		
//		
//		// find all children of xz0 component
//		assertJQ(req("q", "{!join from=id to=id_p}a_cms\\:component:xz0", "fl", "name"),
//				"/response=={'numFound':2,'start':0,'docs':[{'name':'title'}, {'name':'byline'}]}");
//		
//		// find all figures with a bylinew with value "me"
//		assertJQ(req("q", "{!join from=id_p to=id v=$qq}",
//					"qq", "name:byline AND pos:1.2.2", // we don't have text indexed in this test so we use pos instead
//					//"qf", "name",
//					"fl", "id",
//					"debugQuery", "true"),
//				"/response=={'numFound':1,'start':0,'docs':[{'id':'testdoc1_e3'}]}");
//		
//		// find all elements that contain a title (recursively)
//		assertJQ(req("q", "{!join from=id_a to=id}name:title", "fl", "id"),
//				"/response=={'numFound':2,'start':0,'docs':[{'id':'testdoc1_e1'},{'id':'testdoc1_e3'}]}");
//		
//	}	
	
}