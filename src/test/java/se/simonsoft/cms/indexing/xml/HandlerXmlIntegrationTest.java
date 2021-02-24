/**
 * Copyright (C) 2009-2017 Simonsoft Nordic AB
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import se.repos.indexing.IndexAdmin;
import se.repos.testing.indexing.ReposTestIndexing;
import se.repos.testing.indexing.TestIndexOptions;
import se.simonsoft.cms.backend.filexml.CmsRepositoryFilexml;
import se.simonsoft.cms.backend.filexml.FilexmlRepositoryReadonly;
import se.simonsoft.cms.backend.filexml.FilexmlSourceClasspath;
import se.simonsoft.cms.backend.filexml.testing.ReposTestBackendFilexml;
import se.simonsoft.cms.indexing.xml.testconfig.IndexingConfigXmlBase;
import se.simonsoft.cms.indexing.xml.testconfig.IndexingConfigXmlDefault;

public class HandlerXmlIntegrationTest {

	private ReposTestIndexing indexing = null;

	/**
	 * Manual dependency injection.
	 */
	@Before
	public void setUpIndexing() {
		TestIndexOptions indexOptions = new TestIndexOptions().itemDefaultServices()
				.addCore("reposxml", "se/simonsoft/cms/indexing/xml/solr/reposxml/**")
				.addModule(new IndexingConfigXmlBase())
				.addModule(new IndexingConfigXmlDefault());
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
		
		SolrClient reposxml = indexing.getCore("reposxml");
		
		SolrDocumentList x1 = reposxml.query(new SolrQuery("*:*").setSort("pos", ORDER.asc)).getResults();
		assertEquals(4, x1.getNumFound());
		assertEquals("should get 'repoid' from repositem", "localtesthost/svn/tiny-inline", x1.get(0).getFieldValue("repoid"));
	
		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList flagged = repositem.query(new SolrQuery("flag:hasxml AND head:true")).getResults();
		assertEquals("Documents that got added to reposxml should be flagged 'hasxml' in repositem", 1, flagged.getNumFound());
		Collection<Object> flags = flagged.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", flagged.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasxml'", flagged.get(0).getFieldValues("flag").contains("hasxml"));
		assertTrue("Flag 'hasxmlrepositem'", flagged.get(0).getFieldValues("flag").contains("hasxmlrepositem"));
		assertFalse("Flag 'hasridduplicate'", flagged.get(0).getFieldValues("flag").contains("hasridduplicate"));
		assertEquals("", 2, flags.size());
		
		//Statistics in repositem schema
		assertEquals("Should count elements", 4L, flagged.get(0).getFieldValue("count_elements"));
		assertEquals("Should count words", 3L, flagged.get(0).getFieldValue("count_words_text"));
		assertNull("not calculated, no RID", flagged.get(0).getFieldValue("count_words_translate"));
		
		// Depth for reposxml
		assertEquals("null since item is not a translation", null, flagged.get(0).getFieldValue("count_reposxml_depth"));

		// Reposxml
		assertEquals("Should index all elements", 4, x1.size());
		
		assertEquals("document/root element name", "doc", x1.get(0).getFieldValue("name"));
		assertEquals("element pos", "1", x1.get(0).getFieldValue("pos"));
		assertEquals("word count identical to repositem (document element)", 3L, x1.get(0).getFieldValue("count_words_text"));
		assertEquals("word count translate", 3L, x1.get(0).getFieldValue("count_words_translate"));
		assertEquals("word count child (immediate text)", 0L, x1.get(0).getFieldValue("count_words_child"));
		
		assertEquals("document/root element name", "elem", x1.get(2).getFieldValue("name"));
		assertEquals("element pos", "1.2", x1.get(2).getFieldValue("pos"));
		assertEquals("word count", 2L, x1.get(2).getFieldValue("count_words_text"));
		assertEquals("word count child (immediate text)", 1L, x1.get(2).getFieldValue("count_words_child"));
		
		// The "typename" is quite debatable because the test document has an incorrect DOCTYPE declaration (root element is "doc" not "document").
		assertEquals("should set root element name", "document", x1.get(0).getFieldValue("typename"));
		assertEquals("should set systemid", "techdoc.dtd", x1.get(0).getFieldValue("typesystem"));
		assertEquals("should set publicid", "-//Simonsoft//DTD TechDoc Base V1.0 Techdoc//EN", x1.get(0).getFieldValue("typepublic"));
		
		assertEquals("should extract source", "<elem>text</elem>", x1.get(1).getFieldValue("source"));
	}

	@Test
	public void testTinyRidDuplicate() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-ridduplicate");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-ridduplicate", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
		
		SolrClient reposxml = indexing.getCore("reposxml");
		
		SolrDocumentList x1 = reposxml.query(new SolrQuery("pathname:test1.xml").addSort("pos", ORDER.asc)).getResults();
		assertEquals("Should index all elements", 5, x1.getNumFound());
		assertEquals("should get 'repoid' from repositem", "localtesthost/svn/tiny-ridduplicate", x1.get(0).getFieldValue("repoid"));
	
		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList flagged = repositem.query(new SolrQuery("pathname:test1.xml AND flag:hasxml AND head:true")).getResults();
		assertEquals("Documents that got added to reposxml should be flagged 'hasxml' in repositem", 1, flagged.getNumFound());
		Collection<Object> flags = flagged.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", flagged.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasxml'", flagged.get(0).getFieldValues("flag").contains("hasxml"));
		assertTrue("Flag 'hasridduplicate'", flagged.get(0).getFieldValues("flag").contains("hasridduplicate"));
		assertEquals("3 flag(s)", 3, flags.size());
		
		Collection<Object> duplicates = flagged.get(0).getFieldValues("embd_xml_ridduplicate");
		assertEquals("one duplicate, mentioned once", 1, duplicates.size());
		assertEquals("List the duplicate RIDs in repositem core", "2gyvymn15kv0002", duplicates.iterator().next());
		

		// Back to asserting on reposxml.
		assertEquals("second element", "section", x1.get(1).getFieldValue("name"));
		assertEquals("third element", "elem", x1.get(2).getFieldValue("name"));
		assertEquals("should extract source", "<elem xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" name=\"ch1\" cms:rid=\"2gyvymn15kv0002\">text</elem>", x1.get(2).getFieldValue("source"));
	}
	
	@Test
	public void testTinyRidDuplicateTsuppress() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-ridduplicate");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-ridduplicate", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
		
		SolrClient reposxml = indexing.getCore("reposxml");
		
		SolrDocumentList x1 = reposxml.query(new SolrQuery("pathname:test1-tsuppress.xml").addSort("pos", ORDER.asc)).getResults();
		assertEquals("Should index all elements", 5, x1.getNumFound());
		assertEquals("should get 'repoid' from repositem", "localtesthost/svn/tiny-ridduplicate", x1.get(0).getFieldValue("repoid"));
	
		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList flagged = repositem.query(new SolrQuery("pathname:test1-tsuppress.xml AND flag:hasxml AND head:true")).getResults();
		assertEquals("Documents that got added to reposxml should be flagged 'hasxml' in repositem", 1, flagged.getNumFound());
		Collection<Object> flags = flagged.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", flagged.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasxml'", flagged.get(0).getFieldValues("flag").contains("hasxml"));
		assertFalse("Flag 'hasridduplicate'", flagged.get(0).getFieldValues("flag").contains("hasridduplicate"));
		assertTrue("Flag 'hastsuppress'", flagged.get(0).getFieldValues("flag").contains("hastsuppress"));
		assertEquals("only hasxml, hasxmlrepositem, hastsuppress flag", 3, flags.size());

		// Back to asserting on reposxml.
		assertEquals("second element", "section", x1.get(1).getFieldValue("name"));
		assertEquals("third element", "elem", x1.get(2).getFieldValue("name"));
		assertEquals("should extract source", "<elem xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" name=\"ch1\" cms:rid=\"2gyvymn15kv0002\">text</elem>", x1.get(2).getFieldValue("source"));
	}
	
	@Test
	public void testNextRevisionDeletesElement() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-inline");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-inline", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
		
		SolrClient reposxml = indexing.getCore("reposxml");
		SolrDocumentList x1 = reposxml.query(new SolrQuery("*:*")).getResults();
		assertEquals(4, x1.getNumFound());
		assertEquals("should get 'repoid' from repositem", "localtesthost/svn/tiny-inline", x1.get(0).getFieldValue("repoid"));
	
		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList flagged = repositem.query(new SolrQuery("flag:hasxml AND head:true")).getResults();
		assertEquals("Documents that got added to reposxml should be flagged 'hasxml' in repositem", 1, flagged.getNumFound());
		
		// TODO delete one of the elements and make sure it is not there after indexing next revision, would indicate reliance on id overwrite
		
	}	
	
	@Test
	public void testInvalidXml() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-invalid");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-invalid", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
		
		SolrClient reposxml = indexing.getCore("reposxml");
		SolrDocumentList x1 = reposxml.query(new SolrQuery("*:*")).getResults();		
		assertEquals("Should skip the document because it is not parseable as XML. Thus we can try formats that may be XML, such as html, without breaking indexing.",
				0, x1.getNumFound());
		
		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList flagged = repositem.query(new SolrQuery("flag:hasxmlerror AND head:true")).getResults();
		assertEquals("Should be flagged as error in repositem", 1, flagged.getNumFound());		
	}
	
	@Test
	public void testClear() throws SolrServerException, IOException {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-inline");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-inline", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
		
		SolrClient reposxml = indexing.getCore("reposxml");
		assertTrue("Should have indexed something", reposxml.query(new SolrQuery("*:*")).getResults().size() > 0);
		
		// IndexAdminXml is not bound in text context, we should probably switch to a real config module in this test
		IndexAdmin indexAdmin = indexing.getContext().getInstance(IndexAdmin.class);
		indexAdmin.clear();
		
		assertEquals("Should have removed all xml", 0, reposxml.query(new SolrQuery("*:*")).getResults().size());
		
		SolrInputDocument differentRepo = new SolrInputDocument();
		differentRepo.setField("id", "something completely different");
		differentRepo.setField("pos", "2");
		reposxml.add(differentRepo);
		reposxml.commit();
		
		assertEquals(1, reposxml.query(new SolrQuery("*:*")).getResults().size());
		indexAdmin.clear();
		assertEquals("Should not have cleared other repositories", 1, reposxml.query(new SolrQuery("*:*")).getResults().size());
	}
	
	@Test
	public void testJoin() throws SolrServerException, IOException {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-inline");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-inline", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		SolrClient reposxml = indexing.enable(new ReposTestBackendFilexml(filexml)).getCore("reposxml");
		
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
		assertEquals("localtesthost/svn/tiny-inline/test1.xml@0000000002|00000003", j3.get(0).getFieldValue("id"));
		
		SolrDocumentList j4 = reposxml.query(new SolrQuery("name:elem AND {!join from=id_p to=id}*:*")).getResults();
		assertEquals("all elements that are an elem and have a child, got " + j4, 1, j4.getNumFound());
		assertEquals("localtesthost/svn/tiny-inline/test1.xml@0000000002|00000003", j4.get(0).getFieldValue("id"));
		
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
	public void testTinyAttributes() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-attributes");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-inline", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
		
		SolrClient reposxml = indexing.getCore("reposxml");
		
		SolrQuery q1 = new SolrQuery("*:*").addSort("pos", SolrQuery.ORDER.asc);
		SolrDocumentList x1 = reposxml.query(q1).getResults();
		assertEquals(4, x1.getNumFound());
		
		assertEquals("get name of root", "root", x1.get(0).getFieldValue("a_name"));
		assertEquals("get depth of root", 1, x1.get(0).getFieldValue("depth"));
		assertEquals("get pos/treeloc of root", "1", x1.get(0).getFieldValue("pos"));
		assertEquals("get name of e1", "ch1", x1.get(1).getFieldValue("a_name"));
		
		assertNull("get name of e2", x1.get(2).getFieldValue("a_name"));
		assertEquals("get id of e2", "e2", x1.get(2).getFieldValue("a_id"));
		
		assertEquals("get ancestor name of e1 - tests that inherited attr is not overridden by local attr", "root", x1.get(1).getFieldValue("aa_name"));
		assertEquals("get inherited name of e1 - overridden by local attr", "ch1", x1.get(1).getFieldValue("ia_name"));
		
		assertEquals("get ancestor name of e2", "root", x1.get(2).getFieldValue("aa_name"));
		assertEquals("get inherited name of e2", "root", x1.get(2).getFieldValue("ia_name"));
		
		assertEquals("get p-sibling name of e2", "ch1", x1.get(2).getFieldValue("sa_name"));
		
		assertEquals("get element name of inline", "inline", x1.get(3).getFieldValue("name"));
		assertEquals("get inherited name of inline", "root", x1.get(3).getFieldValue("ia_name"));
		assertEquals("get depth of inline", 3, x1.get(3).getFieldValue("depth"));
		assertEquals("get pos/treeloc of inline", "1.2.1", x1.get(3).getFieldValue("pos"));
		assertNull("get p-sibling name of inline", x1.get(3).getFieldValue("sa_name"));
		
	}
	
	@Test
	public void testTinyAttributesNs() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-attributes-ns");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-inline", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
		
		SolrClient reposxml = indexing.getCore("reposxml");
		
		SolrQuery q1 = new SolrQuery("*:*").addSort("pos", SolrQuery.ORDER.asc);
		SolrDocumentList x1 = reposxml.query(q1).getResults();
		assertEquals(4, x1.getNumFound());
		
		assertEquals("get name of root", "root", x1.get(0).getFieldValue("a_name"));
		assertEquals("get depth of root", 1, x1.get(0).getFieldValue("depth"));
		assertEquals("get pos/treeloc of root", "1", x1.get(0).getFieldValue("pos"));
		assertEquals("get RID of root", "2gyvymn15kv0000", x1.get(0).getFieldValue("a_cms.rid"));
		assertEquals("get doc.code of root", "period", x1.get(0).getFieldValue("a_doc,code"));
		
		
		assertEquals("get name of e1", "ch1", x1.get(1).getFieldValue("a_name"));
		assertEquals("get RID of e1", "2gyvymn15kv0001", x1.get(1).getFieldValue("a_cms.rid"));
		assertEquals("get doc.code of e1", "period-child", x1.get(1).getFieldValue("a_doc,code"));
		
		assertNull("get name of e2", x1.get(2).getFieldValue("a_name"));
		assertEquals("get id of e2", "e2", x1.get(2).getFieldValue("a_id"));
		assertEquals("get RID of e2", "2gyvymn15kv0002", x1.get(2).getFieldValue("a_cms.rid"));
		assertEquals("get doc.code of e2, empty", "", x1.get(2).getFieldValue("a_doc,code"));
		assertNull("Non-existant attributes are null in schema", x1.get(2).getFieldValue("a_nonexist"));
		
		// Also test ancestor attributes
		assertEquals("get ancestor RID of e1", "2gyvymn15kv0000", x1.get(1).getFieldValue("aa_cms.rid"));
		assertEquals("get inherited RID of e1", "2gyvymn15kv0001", x1.get(1).getFieldValue("ia_cms.rid"));
		assertEquals("get ancestor doc.code of e1", "period", x1.get(1).getFieldValue("aa_doc,code"));
		assertEquals("get inherited doc.code of e1", "period-child", x1.get(1).getFieldValue("ia_doc,code"));
		
		assertEquals("get ancestor RID of e2", "2gyvymn15kv0000", x1.get(2).getFieldValue("aa_cms.rid"));
		assertEquals("get inherited RID of e2", "2gyvymn15kv0002", x1.get(2).getFieldValue("ia_cms.rid"));
		assertEquals("get ancestor doc.code of e2", "period", x1.get(2).getFieldValue("aa_doc,code"));
		assertEquals("get inherited doc.code of e2", "", x1.get(2).getFieldValue("ia_doc,code"));
	}
	
	
	@Test
	public void testAttributesReleasetranslationRelease() throws SolrServerException, IOException {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/releasetranslation");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/testaut1", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		SolrClient reposxml = indexing.enable(new ReposTestBackendFilexml(filexml)).getCore("reposxml");
		
		SolrDocument elem;
		// search for the first title
		SolrDocumentList findUsingRid = reposxml.query(new SolrQuery("a_cms.rid:2gyvymn15kv0001 AND -prop_abx.TranslationLocale:*")).getResults();
		assertEquals("Should find the first title in the release (though actually a future one)", 1, findUsingRid.getNumFound());
		elem = findUsingRid.get(0);
		assertEquals("get the rid attribute", "2gyvymn15kv0001", elem.getFieldValue("a_cms.rid"));
		assertEquals("get the parent rlogicalid", "x-svn:///svn/testaut1^/tms/xml/Docs/My%20First%20Novel.xml?p=5", elem.getFieldValue("ia_cms.rlogicalid"));

		
		findUsingRid = reposxml.query(new SolrQuery("a_cms.rid:2gyvymn15kv0006 AND -prop_abx.TranslationLocale:*")).getResults();
		assertEquals("Should find a para", 1, findUsingRid.getNumFound());
		elem = findUsingRid.get(0);
		assertEquals("verify it is a para", "p", elem.getFieldValue("name")); 
		assertEquals("get the rid attribute", "2gyvymn15kv0006", elem.getFieldValue("a_cms.rid")); 
		assertEquals("get the ancestor rid attribute (in this case parent rid)", "2gyvymn15kv0004", elem.getFieldValue("aa_cms.rid"));
		assertEquals("get the inherited rid attribute (in this case context element rid)", "2gyvymn15kv0006", elem.getFieldValue("ia_cms.rid"));
		assertEquals("get the root rid attribute", "2gyvymn15kv0000", elem.getFieldValue("ra_cms.rid"));
		assertEquals("get the preceding sibling rid attribute", "2gyvymn15kv0005", elem.getFieldValue("sa_cms.rid"));
		assertNull("get the project id attribute", elem.getFieldValue("a_cms.translation-project"));
		
		assertEquals("get the inherited rlogicalid attribute", "x-svn:///svn/testaut1^/tms/xml/Secs/First%20chapter.xml?p=4", elem.getFieldValue("ia_cms.rlogicalid"));
		
		assertEquals("assist depends on patharea", Arrays.asList(new String[] {"release"}), elem.getFieldValue("patharea"));
		assertEquals("assist depends on reusevalue even for a Release", 1, elem.getFieldValue("reusevalue"));		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testAttributesReleasetranslationTranslation() throws SolrServerException, IOException {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/releasetranslation");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/testaut1", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
		SolrClient reposxml = indexing.getCore("reposxml");
		
		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList flagged = repositem.query(new SolrQuery("flag:hasxml AND head:true")).getResults();
		assertEquals("Documents that got added to reposxml should be flagged 'hasxml' in repositem", 2, flagged.getNumFound());
		assertNull("Should NOT limit depth of Release", flagged.get(0).getFieldValue("count_reposxml_depth"));
		assertEquals("Should limit depth of Translation", 1L, flagged.get(1).getFieldValue("count_reposxml_depth"));
		
		SolrDocumentList findAll = reposxml.query(new SolrQuery("prop_abx.TranslationLocale:*")).getResults();
		//assertEquals("Should find all elements in the single translation", 1, findAll.getNumFound());
		assertEquals("Should ...", 1L, findAll.get(0).getFieldValue("count_reposxml_depth"));
		
		SolrDocumentList findUsingRid0 = reposxml.query(new SolrQuery("a_cms.rid:2gyvymn15kv0000 AND prop_abx.TranslationLocale:*")).getResults();
		assertEquals("Should find root element in the Translation", 1, findUsingRid0.getNumFound());
		SolrDocument elem0 = findUsingRid0.get(0);

		String ridStr = (String) elem0.getFieldValue("reuseridreusevalue");
		assertEquals("number of elements is 13, verified",  13, ridStr.split(" ").length);
		assertEquals("RIDs with reusevalue > 0", "2gyvymn15kv0000 2gyvymn15kv0001 2gyvymn15kv0002 2gyvymn15kv0003 2gyvymn15kv0004 2gyvymn15kv0005 2gyvymn15kv0006 2gyvymn15kv0007 2gyvymn15kv0008 2gyvymn15kv0009 2gyvymn15kv000a 2gyvymn15kv000b 2gyvymn15kv000c ", ridStr);
		
		List<String> cList = (List<String>) elem0.getFieldValue("reuse_c_sha1_release_descendants");
		//assertEquals("debug contents", "...", cList);
		assertTrue("should contain Release checksum", cList.contains("c5fed03ed1304cecce75d63aee2ada2b0f2326af"));
		assertEquals("get RID by checksum", "2gyvymn15kv0006", elem0.getFieldValue("reuse_rid_c5fed03ed1304cecce75d63aee2ada2b0f2326af"));
	}

	
	@Test
	public void testJoinReleasetranslationNoExtraFields() throws SolrServerException, IOException {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/releasetranslation");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/testaut1", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		SolrClient reposxml = indexing.enable(new ReposTestBackendFilexml(filexml)).getCore("reposxml");
		
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

	/** 
	 * This kind of join is not used, just work in progress.
	 * @throws SolrServerException
	 * @throws IOException 
	 */
	@Test
	@Ignore
	public void testJoinReleasetranslation() throws SolrServerException, IOException {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/releasetranslation");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/testaut1", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		SolrClient reposxml = indexing.enable(new ReposTestBackendFilexml(filexml)).getCore("reposxml");
		
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
	
	/** 
	 * This kind of join is not used, just work in progress.
	 * @throws SolrServerException
	 * @throws IOException 
	 */
	@Test
	@Ignore
	public void testJoinReleasetranslationRid() throws SolrServerException, IOException {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/releasetranslation");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/testaut1", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		SolrClient reposxml = indexing.enable(new ReposTestBackendFilexml(filexml)).getCore("reposxml");
		
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
	
	/**
	 * Test covering the search algorithm actually implemented in CMS 3.0.
	 * The joins is performed on RID to match the Sha1 on the Release side while the Translation is the "primary" side of the join.
	 * @throws SolrServerException
	 * @throws IOException 
	 */
	@Test 
	@Ignore // No longer possible, avoiding indexing the full depth of Translations.
	public void testJoinReleasetranslationRidSha1() throws SolrServerException, IOException {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/releasetranslation");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/testaut1", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		SolrClient reposxml = indexing.enable(new ReposTestBackendFilexml(filexml)).getCore("reposxml");
		
		// search for the first title
		SolrDocumentList findUsingRid = reposxml.query(new SolrQuery("a_cms.rid:2gyvymn15kv0001 AND -prop_abx.TranslationLocale:*")).getResults();
		assertEquals("Should find the first title in the release (though actually a future one)", 1, findUsingRid.getNumFound());
		String wantedReleaseSha1 = (String) findUsingRid.get(0).getFieldValue("c_sha1_source_reuse");
		
		String locale = "sv-SE";
		// this join does not know that the remote element is actually in a Release
		// it could be another not-yet-translated translation, but that would typically not be an issue.
		SolrQuery query = new SolrQuery("prop_abx.TranslationLocale:" + locale
				+ " AND {!join from=a_cms.rid to=a_cms.rid}c_sha1_source_reuse:" + wantedReleaseSha1);
				
		query.addFilterQuery("reusevalue:[1 TO *]");
		query.addFilterQuery("patharea:translation");
		// Filter on repository and parent path.
		query.addFilterQuery("repo:" + repo.getName());
		query.addFilterQuery("repoparent:" + "\\/svn"); 
		
		// Prefer higher reuseready integer, prefers Released over other status values.
		query.addSort(SolrQuery.SortClause.desc("reuseready"));
		// Prefer the highest RID, i.e. latest finalized.
		query.addSort(SolrQuery.SortClause.desc("a_cms.rid"));
		
		SolrDocumentList findReusevalue = reposxml.query(query).getResults();
		assertEquals(1, findReusevalue.getNumFound());
		String ridForSourceAndReusereadyLookup = (String) findReusevalue.get(0).getFieldValue("a_cms.rid");
		assertEquals("2gyvymn15kv0001", ridForSourceAndReusereadyLookup);
		assertEquals(1, findReusevalue.get(0).getFieldValue("reuseready"));
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
//		SolrClient SolrClient = mock(SolrClient.class, withSettings().verboseLogging());
//		
//		XmlSourceHandlerSolrj handler = new XmlSourceHandlerSolrj(SolrClient, idStrategy) {
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
//		// commit not expected to be done by handler anymore //verify(SolrClient, times(1)).commit();
//
//		ArgumentCaptor<List> addcapture = ArgumentCaptor.forClass(List.class);
//		verify(SolrClient).add(addcapture.capture());
//		verifyNoMoreInteractions(SolrClient);
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
//		SolrClient SolrClient = mock(SolrClient.class);	
//		
//		XmlSourceHandlerSolrj handler = new XmlSourceHandlerSolrj(SolrClient, idStrategy) {
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
//		verify(SolrClient).add(addcapture.capture());
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
