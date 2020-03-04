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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.Processor;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.common.SolrDocumentList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.repos.testing.indexing.ReposTestIndexing;
import se.repos.testing.indexing.TestIndexOptions;
import se.simonsoft.cms.backend.filexml.CmsRepositoryFilexml;
import se.simonsoft.cms.backend.filexml.FilexmlRepositoryReadonly;
import se.simonsoft.cms.backend.filexml.FilexmlSourceClasspath;
import se.simonsoft.cms.backend.filexml.testing.ReposTestBackendFilexml;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexRidDuplicateDetection;
import se.simonsoft.cms.indexing.xml.testconfig.IndexingConfigXmlBase;
import se.simonsoft.cms.indexing.xml.testconfig.IndexingConfigXmlDefault;
import se.simonsoft.cms.indexing.xml.testconfig.IndexingConfigXmlStub;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;

import com.google.inject.Guice;
import com.google.inject.Injector;

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
	public void testTinyPretranslate() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-pretranslate");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-pretranslate", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
	
		SolrServer repositem = indexing.getCore("repositem");
		SolrDocumentList doc = repositem.query(new SolrQuery("pathname:test1.xml AND flag:hasxml")).getResults();
		assertEquals("Document should exist", 1, doc.getNumFound());
		Collection<Object> flags = doc.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", doc.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasxml'", doc.get(0).getFieldValues("flag").contains("hasxml"));
		//assertTrue("Flag 'hasridduplicate'", doc.get(0).getFieldValues("flag").contains("hasridduplicate"));
		assertEquals("1 flag(s)", 1, flags.size());
		
		Collection<Object> mixedUnsafe = doc.get(0).getFieldValues("embd_xml_ridmixedunsafe");
		assertNull("no unsafe mixed content elements", mixedUnsafe);
	}

	
	@Test
	public void testTinyPretranslateMixedUnsafe() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/tiny-pretranslate");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/tiny-pretranslate", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
	
		SolrServer repositem = indexing.getCore("repositem");
		SolrDocumentList doc = repositem.query(new SolrQuery("pathname:test1-mixed-unsafe.xml")).getResults();
		assertEquals("Document should exist", 1, doc.getNumFound());
		Collection<Object> flags = doc.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", doc.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasridmixedunsafe'", doc.get(0).getFieldValues("flag").contains("hasridmixedunsafe"));
		assertEquals("2 flag(s)", 2, flags.size());
		
		Collection<Object> mixedUnsafe = doc.get(0).getFieldValues("embd_xml_ridmixedunsafe");
		assertEquals("one unsafe mixed RID", 1, mixedUnsafe.size());
		assertEquals("List the unsafe mixed RIDs in repositem core", "2gyvymn15kv0001", mixedUnsafe.iterator().next());
	}
	
}
