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
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import se.repos.indexing.IndexAdmin;
import se.repos.testing.indexing.ReposTestIndexing;
import se.repos.testing.indexing.TestIndexOptions;
import se.simonsoft.cms.backend.filexml.CmsRepositoryFilexml;
import se.simonsoft.cms.backend.filexml.FilexmlRepositoryReadonly;
import se.simonsoft.cms.backend.filexml.FilexmlSourceClasspath;
import se.simonsoft.cms.backend.filexml.testing.ReposTestBackendFilexml;
import se.simonsoft.cms.indexing.xml.custom.IndexFieldExtractionCustomXsl;
import se.simonsoft.cms.indexing.xml.custom.XmlMatchingFieldExtractionSourceDefault;
import se.simonsoft.cms.indexing.xml.fields.IndexFieldDeletionsToSaveSpace;
import se.simonsoft.cms.indexing.xml.fields.IndexReuseJoinFields;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldElement;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldExtractionChecksum;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexIdAppendTreeLocation;
import se.simonsoft.cms.indexing.xml.solr.XmlIndexWriterSolrj;
import se.simonsoft.cms.indexing.xml.testconfig.IndexingConfigXml;
import se.simonsoft.cms.xmlsource.handler.XmlSourceReader;
import se.simonsoft.cms.xmlsource.handler.jdom.XmlSourceReaderJdom;

public class HandlerXmlIntegrationTest {

	private ReposTestIndexing indexing = null;

	/**
	 * Manual dependency injection.
	 */
	@Before
	public void setUpIndexing() {
		TestIndexOptions indexOptions = new TestIndexOptions().itemDefaultServices()
				.addCore("reposxml", "se/simonsoft/cms/indexing/xml/solr/reposxml/**")
				.addModule(new IndexingConfigXml());
		indexing = ReposTestIndexing.getInstance(indexOptions);
	}
	
	@After
	public void tearDown() throws IOException {
		indexing.tearDown();
	}
	
	@Test
	public void testTinyInline() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-inline");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-inline", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
		
		SolrServer reposxml = indexing.getCore("reposxml");
		
		SolrDocumentList x1 = reposxml.query(new SolrQuery("*:*")).getResults();
		assertEquals(4, x1.getNumFound());
		assertEquals("should get 'repoid' from repositem", "localtesthost/svn/tiny-inline", x1.get(0).getFieldValue("repoid"));
	
		SolrServer repositem = indexing.getCore("reposxml");
		SolrDocumentList flagged = reposxml.query(new SolrQuery("flag:hasxml")).getResults();
		assertEquals("Documents that got added to reposxml should be flagged 'hasxml' in repositem", 1, flagged.getNumFound());
		
		

		assertEquals("Should index all elements", 5, reposxml.query(new SolrQuery("*:*")).getResults().size());
	}

	@Test
	public void testNextRevisionDeletesElement() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-inline");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-inline", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
		
		SolrServer reposxml = indexing.getCore("reposxml");
		SolrDocumentList x1 = reposxml.query(new SolrQuery("*:*")).getResults();
		assertEquals(4, x1.getNumFound());
		assertEquals("should get 'repoid' from repositem", "localtesthost", x1.get(0).getFieldValue("repoid"));
	
		SolrServer repositem = indexing.getCore("reposxml");
		SolrDocumentList flagged = reposxml.query(new SolrQuery("flag:hasxml")).getResults();
		assertEquals("Documents that got added to reposxml should be flagged 'hasxml' in repositem", 1, flagged.getNumFound());
		
		// TODO delete one of the elements and make sure it is not there after indexing next revision, would indicate reliance on id overwrite
		
	}	
	
	@Test
	public void testInvalidXml() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-invalid");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-invalid", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
		
		SolrServer reposxml = indexing.getCore("reposxml");
		SolrDocumentList x1 = reposxml.query(new SolrQuery("*:*")).getResults();		
		assertEquals("Should skip the document because it is not parseable as XML. Thus we can try formats that may be XML, such as html, without breaking indexing.",
				0, x1.getNumFound());
		
		SolrServer repositem = indexing.getCore("repositem");
		SolrDocumentList flagged = reposxml.query(new SolrQuery("flag:hasxmlerror")).getResults();
		assertEquals("Should be flagged as error in repositem", 1, flagged.getNumFound());		
	}
	
	@Test
	public void testClear() {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-inline");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-inline", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
		
		SolrServer reposxml = indexing.getCore("reposxml");
		
		// IndexAdminXml is not bound in text context, we should probably switch to a real config module in this test
		//IndexAdminXml indexAdminXml = new IndexAdminXml(repo...
		
		//context.getInstance(IndexAdmin.class).addPostAction(indexAdminXml);
	}
	
	@Test
	public void testJoin() throws SolrServerException {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-inline");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-inline", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		SolrServer reposxml = indexing.enable(new ReposTestBackendFilexml(filexml)).getCore("reposxml");
		
		SolrDocumentList j1 = reposxml.query(new SolrQuery("{!join from=id to=id_p}*:*")).getResults();
		assertEquals("all elements that have a parent, got " + j1, 3, j1.getNumFound());
		for (SolrDocument e : j1) {
			assertNotEquals("root does not have a parent", "doc", e.getFieldValue("name"));
		}
	
		SolrDocumentList j2 = reposxml.query(new SolrQuery("{!join from=id_p to=id}*:*")).getResults();
		assertEquals("all elements that have a child, got " + j2, 2, j2.getNumFound());
		
		SolrDocumentList j3 = reposxml.query(new SolrQuery("{!join from=id_p to=id}name:inline")).getResults();
		assertEquals("all elements that have a child which is an <inline/>, got " + j3, 1, j3.getNumFound());
		assertEquals("elem", j3.get(0).getFieldValue("name"));
		assertEquals("localtesthost/svn/tiny-inline/test1.xml@2|1.2", j3.get(0).getFieldValue("id"));
		
		SolrDocumentList j4 = reposxml.query(new SolrQuery("name:elem AND {!join from=id_p to=id}*:*")).getResults();
		assertEquals("all elements that are an elem and have a child, got " + j4, 1, j4.getNumFound());
		assertEquals("localtesthost/svn/tiny-inline/test1.xml@2|1.2", j4.get(0).getFieldValue("id"));
		
		SolrDocumentList j5 = reposxml.query(new SolrQuery("{!join from=id_p to=id}(name:elem OR name:inline)")).getResults();
		assertEquals("all elements that have a child which is either <elem/> or <inline/>" + j5, 2, j5.getNumFound());
		
		// why doesn't this run? instead use Parameter dereferencing?
		//SolrDocumentList j6 = reposxml.query(new SolrQuery("repo:tiny-inline AND {!join from=id_p to=id}(name:elem OR name:inline)")).getResults();
		//assertEquals("all elements that have a child which is either <elem/> or <inline/>, in the test repo" + j6, 2, j6.getNumFound());

		SolrDocumentList j7 = reposxml.query(new SolrQuery("{!join from=id_p to=id}(text:\"elem text\" AND name:elem)")).getResults();
		assertEquals("elements that have a child which matches two criterias" + j7, 1, j7.getNumFound());

		SolrDocumentList j8 = reposxml.query(new SolrQuery("{!join from=id_a to=id}name:inline")).getResults();
		assertEquals("elements with a descendat which is an <inline/>, got " + j8, 2, j8.getNumFound());		
		
		// "Parameter dereferencing", http://wiki.apache.org/solr/LocalParams#parameter_dereferencing, but how to do "qq" in solrj?
//		// find all figures with a bylinew with value "me"
//		assertJQ(req("q", "{!join from=id_p to=id v=$qq}",
//					"qq", "name:byline AND pos:1.2.2", // we don't have text indexed in this test so we use pos instead
//					//"qf", "name",
//					"fl", "id",
//					"debugQuery", "true"),
//				"/response=={'numFound':1,'start':0,'docs':[{'id':'testdoc1_e3'}]}");
		
	}
	
	@Test
	public void testJoinReleasetranslationNoExtraFields() throws SolrServerException {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/releasetranslation");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/testaut1", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		SolrServer reposxml = indexing.enable(new ReposTestBackendFilexml(filexml)).getCore("reposxml");
		
		// search for the first title
		SolrDocumentList findUsingRid = reposxml.query(new SolrQuery("a_cms.rid:2gyvymn15kv0001 AND -prop_abx.TranslationLocale:*")).getResults();
		assertEquals("Should find the first title in the release (though actually a future one)", 1, findUsingRid.getNumFound());
		String wantedReleaseSha1 = (String) findUsingRid.get(0).getFieldValue("c_sha1_source_reuse");
		
		SolrDocumentList findAllMatchesWithoutJoin = reposxml.query(new SolrQuery("c_sha1_source_reuse:" + wantedReleaseSha1)).getResults();
		assertEquals("Could search for the checksum in all xml", 1, findAllMatchesWithoutJoin.getNumFound());
		
		SolrQuery q = new SolrQuery("c_sha1_source_reuse:" + wantedReleaseSha1
				// Because we join on the same filed name we must explicitly state that the hit should be a release, or In_Translation items would join with themselves and match
				// Do we have a release specific field that is not copied to translations? For now just exclude translations.
				+ " AND -prop_abx.TranslationLocale:*"
				// Haven't found how to combine two criterias on the join into a single join, when there's also criteria on the actual match (see join test above)
				+ " AND {!join from=prop_abx.AuthorMaster to=prop_abx.AuthorMaster}prop_abx.TranslationLocale:sv-SE"
				+ " AND {!join from=prop_abx.AuthorMaster to=prop_abx.AuthorMaster}reusevalue:1");
		SolrDocumentList findReusevalue = reposxml.query(q).getResults();
		assertEquals(1, findReusevalue.getNumFound());
		// TODO with the current data set it is impossible to assert that we don't get false positives with the above query
		// Would need another release with an Obsolete sv-SE translation and a reusevalue=1 de-DE one, which probably would match falsely
	}

	@Test
	public void testJoinReleasetranslation() throws SolrServerException {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/releasetranslation");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/testaut1", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		SolrServer reposxml = indexing.enable(new ReposTestBackendFilexml(filexml)).getCore("reposxml");
		
		// search for the first title
		SolrDocumentList findUsingRid = reposxml.query(new SolrQuery("a_cms.rid:2gyvymn15kv0001 AND -prop_abx.TranslationLocale:*")).getResults();
		assertEquals("Should find the first title in the release (though actually a future one)", 1, findUsingRid.getNumFound());
		String wantedReleaseSha1 = (String) findUsingRid.get(0).getFieldValue("c_sha1_source_reuse");
		
		SolrQuery q = new SolrQuery("c_sha1_source_reuse:" + wantedReleaseSha1
				+ " AND {!join to=pathfull from=reuserelease}reusevaluelocale:1sv-SE");
		SolrDocumentList findReusevalue = reposxml.query(q).getResults();
		assertEquals(1, findReusevalue.getNumFound());
		String ridForSourceAndReusereadyLookup = (String) findReusevalue.get(0).getFieldValue("a_cms.rid");
		assertEquals("2gyvymn15kv0001", ridForSourceAndReusereadyLookup);
	}	
	
	@Test
	public void testJoinReleasetranslationRid() throws SolrServerException {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/releasetranslation");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/testaut1", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		SolrServer reposxml = indexing.enable(new ReposTestBackendFilexml(filexml)).getCore("reposxml");
		
		// search for the first title
		SolrDocumentList findUsingRid = reposxml.query(new SolrQuery("a_cms.rid:2gyvymn15kv0001 AND -prop_abx.TranslationLocale:*")).getResults();
		assertEquals("Should find the first title in the release (though actually a future one)", 1, findUsingRid.getNumFound());
		String wantedReleaseSha1 = (String) findUsingRid.get(0).getFieldValue("c_sha1_source_reuse");
		
		SolrQuery q = new SolrQuery("c_sha1_source_reuse:" + wantedReleaseSha1
				+ " AND -prop_abx.TranslationLocale:*" // probably as fq for performance, needed because we join on same field so translations would match themselves				
				+ " AND {!join to=a_cms.rid from=a_cms.rid}reusevaluelocale:1sv-SE");
		SolrDocumentList findReusevalue = reposxml.query(q).getResults();
		assertEquals(1, findReusevalue.getNumFound());
		String ridForSourceAndReusereadyLookup = (String) findReusevalue.get(0).getFieldValue("a_cms.rid");
		assertEquals("2gyvymn15kv0001", ridForSourceAndReusereadyLookup);
		
		// TODO we dont't get a cartesian product, so can we sort on released first (as we do with xincludes)
		// otherwise we would risk getting lots of reuseready=0 hits first, and the benefit of joining would be gone
		// The following query syntax fails
		//q.addSort(new SortClause("{!join to=pathfull from=reuserelease}reuseready", ORDER.desc));
		//SolrDocumentList findReusevalueReleasedFirst = reposxml.query(q).getResults();
		//assertEquals(1, findReusevalueReleasedFirst.getNumFound());
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
	
}
