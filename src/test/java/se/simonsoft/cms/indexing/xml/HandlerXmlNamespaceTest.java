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
package se.simonsoft.cms.indexing.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeNotNull;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import se.repos.testing.indexing.ReposTestIndexing;
import se.repos.testing.indexing.TestIndexOptions;
import se.simonsoft.cms.backend.filexml.CmsRepositoryFilexml;
import se.simonsoft.cms.backend.filexml.FilexmlRepositoryReadonly;
import se.simonsoft.cms.backend.filexml.FilexmlSource;
import se.simonsoft.cms.backend.filexml.FilexmlSourceClasspath;
import se.simonsoft.cms.backend.filexml.testing.ReposTestBackendFilexml;
import se.simonsoft.cms.indexing.xml.testconfig.IndexingConfigXmlBase;
import se.simonsoft.cms.indexing.xml.testconfig.IndexingConfigXmlDefault;
import se.simonsoft.cms.item.CmsItemPath;

public class HandlerXmlNamespaceTest {

	private ReposTestIndexing indexing;
	
	private long startTime = 0;
	
	/**
	 * Manual dependency injection.
	 */
	@Before
	public void setUpIndexing() {
		startTime = System.currentTimeMillis();
		
		TestIndexOptions indexOptions = new TestIndexOptions().itemDefaultServices()
				.addCore("reposxml", "se/simonsoft/cms/indexing/xml/solr/reposxml/**")
				.addModule(new IndexingConfigXmlBase())
				.addModule(new IndexingConfigXmlDefault());
		indexing = ReposTestIndexing.getInstance(indexOptions);
	}
	
	@After
	public void tearDown() throws IOException {
		long time = System.currentTimeMillis() - startTime;
		System.out.println("Test took " + time + " millisecondss");
		
		ReposTestIndexing.getInstance().tearDown();
	}
	
	// filexml backend could expose a https://github.com/hamcrest/JavaHamcrest matcher
	protected void assumeResourceExists(FilexmlSource source, String cmsItemPath) {
		assumeNotNull("Test skipped until large file " + cmsItemPath + " is exported",
				source.getFile(new CmsItemPath(cmsItemPath)));
	}
	
	@Test
	public void testNamespaceXhtml() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/namespace-xhtml");
		assumeResourceExists(repoSource, "/test1.xml");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/namespace", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));

		SolrServer reposxml = indexing.getCore("reposxml");
		SolrDocumentList all = reposxml.query(new SolrQuery("*:*").setRows(2).setSort("pos", ORDER.asc)).getResults();
		assertEquals(13, all.getNumFound()); 
		
		SolrDocument e1 = all.get(0);
		assertEquals("html", e1.getFieldValue("name"));
		// Solr allows the wildcard part of dynamic fields to be empty.
		// TODO: Is ns_ what we want or would we like to define as "ns"? 
		assertEquals("declared ns", "http://www.w3.org/1999/xhtml", e1.getFieldValue("ns_"));
		assertEquals("inherited and declared ns", "http://www.w3.org/1999/xhtml", e1.getFieldValue("ins_"));
		
		SolrDocument e2 = all.get(1);
		assertEquals("head", e2.getFieldValue("name"));
		assertNull("not declared here", e2.getFieldValue("ns_"));
		assertEquals("inherited ns", "http://www.w3.org/1999/xhtml", e2.getFieldValue("ins_"));
		
	}
	
	
	@Test
	public void testNamespaceXml() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/namespace-xml");
		assumeResourceExists(repoSource, "/test1.xml");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/namespace", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));

		SolrServer reposxml = indexing.getCore("reposxml");
		SolrDocumentList all = reposxml.query(new SolrQuery("*:*").setRows(5).setSort("pos", ORDER.asc)).getResults();
		assertEquals(5, all.getNumFound()); 
		
		SolrDocument e1 = all.get(0);
		assertEquals("doc", e1.getFieldValue("name"));
		
		assertEquals("declared ns", "http://www.simonsoft.se/namespace/cms", e1.getFieldValue("ns_cms"));
		assertEquals("inherited and declared ns", "http://www.simonsoft.se/namespace/cms", e1.getFieldValue("ins_cms"));
		
		assertEquals("declared ns", "http://www.simonsoft.se/namespace/test1", e1.getFieldValue("ns_cms1"));
		assertEquals("declared ns", "http://www.simonsoft.se/namespace/test2", e1.getFieldValue("ns_cms2"));
		assertEquals("declared ns", "http://www.simonsoft.se/namespace/test3", e1.getFieldValue("ns_cms3"));
		e1 = null;
		
		SolrDocument e2 = all.get(1);
		assertEquals("elem", e2.getFieldValue("name"));
		assertNull("not declared here", e2.getFieldValue("ns_cms"));
		assertNull("not declared here", e2.getFieldValue("ns_cms1"));
		assertNull("not declared here", e2.getFieldValue("ns_cms2"));
		assertNull("not declared here", e2.getFieldValue("ns_cms3"));
		
		assertNotNull("inherited", e2.getFieldValue("ins_cms"));
		assertNotNull("inherited", e2.getFieldValue("ins_cms1"));
		assertNotNull("inherited", e2.getFieldValue("ins_cms2"));
		assertNotNull("inherited", e2.getFieldValue("ins_cms3"));
		
		//System.out.println(e2.getFieldValue("ns_unused"));
		assertEquals("unused namespaces", "[http://www.simonsoft.se/namespace/cms\nhttp://www.simonsoft.se/namespace/test2\nhttp://www.simonsoft.se/namespace/test3\n]", 
				e2.getFieldValue("ns_unused").toString());
		e2 = null;
		
		SolrDocument e3 = all.get(2);
		assertEquals("cms1:elem", e3.getFieldValue("name"));
		assertNull("not declared here", e3.getFieldValue("ns_cms"));
		assertNull("not declared here", e3.getFieldValue("ns_cms1"));
		assertNull("not declared here", e3.getFieldValue("ns_cms2"));
		assertNull("not declared here", e3.getFieldValue("ns_cms3"));
		
		assertNotNull("inherited", e3.getFieldValue("ins_cms"));
		assertNotNull("inherited", e3.getFieldValue("ins_cms1"));
		assertNotNull("inherited", e3.getFieldValue("ins_cms2"));
		assertNotNull("inherited", e3.getFieldValue("ins_cms3"));
		
		//System.out.println(e3.getFieldValue("ns_unused"));
		assertEquals("unused namespaces", "[http://www.simonsoft.se/namespace/cms\nhttp://www.simonsoft.se/namespace/test2\nhttp://www.simonsoft.se/namespace/test3\n]", 
				e3.getFieldValue("ns_unused").toString());		e3 = null;
		
		SolrDocument e4 = all.get(3);
		assertEquals("elem", e4.getFieldValue("name"));
		assertNull("not declared here", e4.getFieldValue("ns_cms"));
		assertNull("not declared here", e4.getFieldValue("ns_cms1"));
		assertNull("not declared here", e4.getFieldValue("ns_cms2"));
		assertNull("not declared here", e4.getFieldValue("ns_cms3"));
		
		assertNotNull("inherited", e4.getFieldValue("ins_cms"));
		assertNotNull("inherited", e4.getFieldValue("ins_cms1"));
		assertNotNull("inherited", e4.getFieldValue("ins_cms2"));
		assertNotNull("inherited", e4.getFieldValue("ins_cms3"));
		
		//System.out.println(e4.getFieldValue("ns_unused"));
		assertEquals("unused namespaces", "[http://www.simonsoft.se/namespace/cms\nhttp://www.simonsoft.se/namespace/test1\nhttp://www.simonsoft.se/namespace/test3\n]", 
				e4.getFieldValue("ns_unused").toString());
		e4 = null;
		
	}

}
