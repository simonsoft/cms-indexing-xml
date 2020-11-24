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
package se.simonsoft.cms.indexing.xml.custom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrDocumentList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import se.repos.testing.indexing.ReposTestIndexing;
import se.repos.testing.indexing.TestIndexOptions;
import se.simonsoft.cms.backend.filexml.CmsRepositoryFilexml;
import se.simonsoft.cms.backend.filexml.FilexmlRepositoryReadonly;
import se.simonsoft.cms.backend.filexml.FilexmlSourceClasspath;
import se.simonsoft.cms.backend.filexml.testing.ReposTestBackendFilexml;
import se.simonsoft.cms.indexing.xml.testconfig.IndexingConfigXmlBase;
import se.simonsoft.cms.indexing.xml.testconfig.IndexingConfigXmlDefault;

public class HandlerXmlRepositemTest {
	
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
	public void testReleaseTranslationTitleText() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/releasetranslation");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/releasetranslation", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
	
		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList doc = repositem.query(new SolrQuery("patharea:release AND flag:hasxml AND head:true")).getResults();
		assertEquals("Document should exist", 1, doc.getNumFound());
		assertEquals("NOTE: This filexml repo was manually created, file name does not match.", "My First Novel.xml", doc.get(0).getFieldValue("pathname"));
		
		Collection<Object> flags = doc.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", doc.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasxml'", doc.get(0).getFieldValues("flag").contains("hasxml"));
		//assertTrue("Flag 'hasridduplicate'", doc.get(0).getFieldValues("flag").contains("hasridduplicate"));
		assertEquals("1 flag(s)", 1, flags.size());
		
		assertEquals("word count excl keyref",  24L, doc.get(0).getFieldValue("count_words_text"));

		assertEquals("", "My First Novel", doc.get(0).getFieldValue("embd_xml_title"));
		assertEquals("", "Once upon a time...\nSubchapters are quite rare in novels.", doc.get(0).getFieldValue("embd_xml_intro"));
	}
	

	@Test
	public void testTinyPretranslate() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-pretranslate");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-pretranslate", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
	
		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList doc = repositem.query(new SolrQuery("pathname:test1.xml AND flag:hasxml AND head:true")).getResults();
		assertEquals("Document should exist", 1, doc.getNumFound());
		Collection<Object> flags = doc.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", doc.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasxml'", doc.get(0).getFieldValues("flag").contains("hasxml"));
		//assertTrue("Flag 'hasridduplicate'", doc.get(0).getFieldValues("flag").contains("hasridduplicate"));
		assertEquals("1 flag(s)", 1, flags.size());
		
		assertEquals("word count excl keyref",  3L, doc.get(0).getFieldValue("count_words_text"));
		
		assertEquals("words to translate", 3L, doc.get(0).getFieldValue("count_words_translate"));
		
		Collection<Object> mixedUnsafe = doc.get(0).getFieldValues("embd_xml_ridmixedunsafe");
		assertNull("no unsafe mixed content elements", mixedUnsafe);
	}
	
	
	@Test
	public void testTinyPretranslateComplete() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-pretranslate");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-pretranslate", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
	
		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList doc = repositem.query(new SolrQuery("pathname:test1-complete.xml AND flag:hasxml AND head:true")).getResults();
		assertEquals("Document should exist", 1, doc.getNumFound());
		Collection<Object> flags = doc.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", doc.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasxml'", doc.get(0).getFieldValues("flag").contains("hasxml"));
		//assertTrue("Flag 'hasridduplicate'", doc.get(0).getFieldValues("flag").contains("hasridduplicate"));
		assertEquals("1 flag(s)", 1, flags.size());
		
		assertEquals("word count excl keyref",  3L, doc.get(0).getFieldValue("count_words_text"));
		
		assertEquals("elements to translate (includes the one with a keyref)", 1L, doc.get(0).getFieldValue("count_elements_translate_all"));
		assertEquals("elements to translate (with text)", 0L, doc.get(0).getFieldValue("count_elements_translate"));
		assertEquals("words to translate", 0L, doc.get(0).getFieldValue("count_words_translate"));
		
		Collection<Object> mixedUnsafe = doc.get(0).getFieldValues("embd_xml_ridmixedunsafe");
		assertNull("no unsafe mixed content elements", mixedUnsafe);
	}
	
	@Test
	public void testTinyPretranslateCompleteSection() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-pretranslate");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-pretranslate", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
	
		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList doc = repositem.query(new SolrQuery("pathname:test1-complete-section.xml AND flag:hasxml AND head:true")).getResults();
		assertEquals("Document should exist", 1, doc.getNumFound());
		Collection<Object> flags = doc.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", doc.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasxml'", doc.get(0).getFieldValues("flag").contains("hasxml"));
		//assertTrue("Flag 'hasridduplicate'", doc.get(0).getFieldValues("flag").contains("hasridduplicate"));
		assertEquals("1 flag(s)", 1, flags.size());
		
		assertEquals("word count excl keyref",  3L, doc.get(0).getFieldValue("count_words_text"));
		
		assertEquals("elements to translate (does not include the one with a keyref since it was part of a replacement)", 0L, doc.get(0).getFieldValue("count_elements_translate"));
		assertEquals("words to translate", 0L, doc.get(0).getFieldValue("count_words_translate"));
		
		Collection<Object> mixedUnsafe = doc.get(0).getFieldValues("embd_xml_ridmixedunsafe");
		assertNull("no unsafe mixed content elements", mixedUnsafe);
	}
	
	@Test
	public void testTinyPretranslateCompleteSectionTranslateNo() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-pretranslate");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-pretranslate", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
		
		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList doc = repositem.query(new SolrQuery("pathname:test1-complete-section-translate-no.xml AND flag:hasxml AND head:true")).getResults();
		assertEquals("Document should exist", 1, doc.getNumFound());
		Collection<Object> flags = doc.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", doc.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasxml'", doc.get(0).getFieldValues("flag").contains("hasxml"));
		//assertTrue("Flag 'hasridduplicate'", doc.get(0).getFieldValues("flag").contains("hasridduplicate"));
		assertEquals("1 flag(s)", 1, flags.size());
		
		assertEquals("word count excl keyref",  3L, doc.get(0).getFieldValue("count_words_text"));
		
		assertEquals("elements to translate (does not include the one with a keyref since it was part of a replacement)", 0L, doc.get(0).getFieldValue("count_elements_translate"));
		assertEquals("words total", 3L, doc.get(0).getFieldValue("count_words_text"));
		assertEquals("words to translate", 0L, doc.get(0).getFieldValue("count_words_translate"));
		assertEquals("words translate=no, now _including_ text in Pretranslated elements", 2L, doc.get(0).getFieldValue("count_words_translate_no"));
		//assertEquals("words translate=no, must exclude text in Pretranslated elements", 0L, doc.get(0).getFieldValue("count_words_translate_no"));
		
		Collection<Object> mixedUnsafe = doc.get(0).getFieldValues("embd_xml_ridmixedunsafe");
		assertNull("no unsafe mixed content elements", mixedUnsafe);
	}
	
	@Test
	public void testTinyPretranslateCompleteSectionTranslateNoTerm() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-pretranslate");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-pretranslate", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
		
		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList doc = repositem.query(new SolrQuery("pathname:test1-translate-no-term.xml AND flag:hasxml AND head:true")).getResults();
		assertEquals("Document should exist", 1, doc.getNumFound());
		Collection<Object> flags = doc.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", doc.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasxml'", doc.get(0).getFieldValues("flag").contains("hasxml"));
		//assertTrue("Flag 'hasridduplicate'", doc.get(0).getFieldValues("flag").contains("hasridduplicate"));
		assertEquals("1 flag(s)", 1, flags.size());
		
		assertEquals("word count excl keyref",  3L, doc.get(0).getFieldValue("count_words_text"));
		
		assertEquals("elements to translate", 0L, doc.get(0).getFieldValue("count_elements_translate"));
		assertEquals("elements to translate RIDs", "", doc.get(0).getFieldValue("embd_xml_ridtranslate"));
		assertEquals("words to translate", 0L, doc.get(0).getFieldValue("count_words_translate"));
		assertEquals("words to translate, must exclude text in Pretranslated elements", 2L, doc.get(0).getFieldValue("count_words_translate_no"));
		
		Collection<Object> mixedUnsafe = doc.get(0).getFieldValues("embd_xml_ridmixedunsafe");
		assertNull("no unsafe mixed content elements", mixedUnsafe);
	}
	
	
	
	@Test
	public void testTinyPretranslatePartial() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-pretranslate");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-pretranslate", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
	
		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList doc = repositem.query(new SolrQuery("pathname:test1-partial.xml AND flag:hasxml AND head:true")).getResults();
		assertEquals("Document should exist", 1, doc.getNumFound());
		Collection<Object> flags = doc.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", doc.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasxml'", doc.get(0).getFieldValues("flag").contains("hasxml"));
		//assertTrue("Flag 'hasridduplicate'", doc.get(0).getFieldValues("flag").contains("hasridduplicate"));
		assertEquals("1 flag(s)", 1, flags.size());
		
		assertEquals("word count excl keyref",  3L, doc.get(0).getFieldValue("count_words_text"));
		
		assertEquals("elements to translate (includes the one with a keyref)", 2L, doc.get(0).getFieldValue("count_elements_translate_all"));
		assertEquals("elements to translate (with text)", 1L, doc.get(0).getFieldValue("count_elements_translate"));
		assertEquals("words to translate", 2L, doc.get(0).getFieldValue("count_words_translate"));
		
		Collection<Object> mixedUnsafe = doc.get(0).getFieldValues("embd_xml_ridmixedunsafe");
		assertNull("no unsafe mixed content elements", mixedUnsafe);
	}
	
	@Test
	public void testTinyPretranslateTranslateNo() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-pretranslate");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-pretranslate", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
	
		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList doc = repositem.query(new SolrQuery("pathname:test1-translate-no.xml AND flag:hasxml AND head:true")).getResults();
		assertEquals("Document should exist", 1, doc.getNumFound());
		Collection<Object> flags = doc.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", doc.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasxml'", doc.get(0).getFieldValues("flag").contains("hasxml"));
		//assertTrue("Flag 'hasridduplicate'", doc.get(0).getFieldValues("flag").contains("hasridduplicate"));
		assertEquals("1 flag(s)", 1, flags.size());
		
		assertEquals("word count excl keyref",  3L, doc.get(0).getFieldValue("count_words_text"));
		
		assertEquals("elements to translate (includes the one with a keyref)", 1L, doc.get(0).getFieldValue("count_elements_translate_all"));
		assertEquals("elements to translate (with text)", 0L, doc.get(0).getFieldValue("count_elements_translate"));
		assertEquals("words to translate", 0L, doc.get(0).getFieldValue("count_words_translate"));
		assertEquals("words to translate", 2L, doc.get(0).getFieldValue("count_words_translate_no"));
		
		Collection<Object> mixedUnsafe = doc.get(0).getFieldValues("embd_xml_ridmixedunsafe");
		assertNull("no unsafe mixed content elements", mixedUnsafe);
	}
	
	@Test
	public void testTinyPretranslateTranslateNoSection() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-pretranslate");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-pretranslate", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
	
		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList doc = repositem.query(new SolrQuery("pathname:test1-translate-no-section.xml AND flag:hasxml AND head:true")).getResults();
		assertEquals("Document should exist", 1, doc.getNumFound());
		Collection<Object> flags = doc.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", doc.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasxml'", doc.get(0).getFieldValues("flag").contains("hasxml"));
		//assertTrue("Flag 'hasridduplicate'", doc.get(0).getFieldValues("flag").contains("hasridduplicate"));
		assertFalse("No flag 'hastsuppress'", doc.get(0).getFieldValues("flag").contains("hastsuppress"));
		assertEquals("1 flag(s)", 1, flags.size());
		
		assertEquals("word count excl keyref",  3L, doc.get(0).getFieldValue("count_words_text"));
		
		assertEquals("elements to translate (includes the one with a keyref but now it is under translate=no)", 0L, doc.get(0).getFieldValue("count_elements_translate"));
		assertEquals("words to translate", 0L, doc.get(0).getFieldValue("count_words_translate"));
		assertEquals("words to translate", 3L, doc.get(0).getFieldValue("count_words_translate_no"));
		
		Collection<Object> mixedUnsafe = doc.get(0).getFieldValues("embd_xml_ridmixedunsafe");
		assertNull("no unsafe mixed content elements", mixedUnsafe);
	}
	
	@Test
	public void testTinyPretranslateTranslateNoTsuppress() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-pretranslate");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-pretranslate", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
	
		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList doc = repositem.query(new SolrQuery("pathname:test1-translate-no-tsuppress.xml AND flag:hasxml AND head:true")).getResults();
		assertEquals("Document should exist", 1, doc.getNumFound());
		Collection<Object> flags = doc.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", doc.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasxml'", doc.get(0).getFieldValues("flag").contains("hasxml"));
		assertFalse("No flag 'hasridduplicate'", doc.get(0).getFieldValues("flag").contains("hasridduplicate"));
		assertTrue("Flag 'hastsuppress'", doc.get(0).getFieldValues("flag").contains("hastsuppress"));
		assertEquals("2 flag(s)", 2, flags.size());
		
		// Total word count, from a translation perspective.
		assertEquals("word count excl keyref",  3L, doc.get(0).getFieldValue("count_words_text"));
		
		// Debatable how to count within tsuppress areas.
		// Exclude is the reasonable way in order to present the values without overlap.
		assertEquals("elements to translate, none", 0L, doc.get(0).getFieldValue("count_elements_translate_all"));
		assertEquals("elements to translate (with text), none", 0L, doc.get(0).getFieldValue("count_elements_translate"));
		assertEquals("elements to translate, none", "", doc.get(0).getFieldValue("embd_xml_ridtranslate_all"));
		
		assertEquals("words suppressed", 3L, doc.get(0).getFieldValue("count_words_tsuppress"));
		
		assertEquals("words to translate", 0L, doc.get(0).getFieldValue("count_words_translate"));
		assertEquals("words to translate", 0L, doc.get(0).getFieldValue("count_words_translate_no"));
		
		Collection<Object> mixedUnsafe = doc.get(0).getFieldValues("embd_xml_ridmixedunsafe");
		assertNull("no unsafe mixed content elements", mixedUnsafe);
	}

	
	@Test
	public void testTinyPretranslateMixedUnsafe() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-pretranslate");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-pretranslate", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
	
		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList doc = repositem.query(new SolrQuery("pathname:test1-mixed-unsafe.xml AND head:true")).getResults();
		assertEquals("Document should exist", 1, doc.getNumFound());
		Collection<Object> flags = doc.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", doc.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasridmixedunsafe'", doc.get(0).getFieldValues("flag").contains("hasridmixedunsafe"));
		assertEquals("2 flag(s)", 2, flags.size());
		
		Collection<Object> mixedUnsafe = doc.get(0).getFieldValues("embd_xml_ridmixedunsafe");
		assertEquals("List the unsafe mixed RIDs in repositem core", "2gyvymn15kv0001", mixedUnsafe.iterator().next());
	}
	
	
	@Test
	public void testTinyPretranslateRidMissingParent() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-pretranslate");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-pretranslate", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
	
		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList doc = repositem.query(new SolrQuery("pathname:test1-rid-missing-parent.xml AND head:true")).getResults();
		assertEquals("Document should exist", 1, doc.getNumFound());
		Collection<Object> flags = doc.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", doc.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasridmissing'", doc.get(0).getFieldValues("flag").contains("hasridmissing"));
		assertEquals("2 flag(s)", 2, flags.size());
		
		Collection<Object> ridMissing = doc.get(0).getFieldValues("embd_xml_ridmissing");
		assertEquals("List the missing RIDs in repositem core", "2gyvymn15kv0002 2gyvymn15kv0003", ridMissing.iterator().next());
	}
	
	@Test
	public void testTinyPretranslateRidMissingSibling() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-pretranslate");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-pretranslate", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
	
		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList doc = repositem.query(new SolrQuery("pathname:test1-rid-missing-sibling.xml AND head:true")).getResults();
		assertEquals("Document should exist", 1, doc.getNumFound());
		Collection<Object> flags = doc.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", doc.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasridmissing'", doc.get(0).getFieldValues("flag").contains("hasridmissing"));
		assertEquals("2 flag(s)", 2, flags.size());
		
		Collection<Object> ridMissing = doc.get(0).getFieldValues("embd_xml_ridmissing");
		assertEquals("List the missing RIDs in repositem core", "2gyvymn15kv0001", ridMissing.iterator().next());
	}
	
	@Test
	public void testTinyPretranslateRidMissingEmpty() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-pretranslate");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-pretranslate", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
	
		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList doc = repositem.query(new SolrQuery("pathname:test1-rid-missing-empty.xml AND head:true")).getResults();
		assertEquals("Document should exist", 1, doc.getNumFound());
		Collection<Object> flags = doc.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", doc.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasridmissing'", doc.get(0).getFieldValues("flag").contains("hasridmissing"));
		assertEquals("2 flag(s)", 2, flags.size());
		
		Collection<Object> ridMissing = doc.get(0).getFieldValues("embd_xml_ridmissing");
		assertEquals("List the missing RIDs in repositem core", "2gyvymn15kv0000", ridMissing.iterator().next());
	}
	
}
