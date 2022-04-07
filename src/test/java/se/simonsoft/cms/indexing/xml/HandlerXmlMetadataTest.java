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
import static org.junit.Assume.assumeNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.AfterClass;
import org.junit.BeforeClass;
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

public class HandlerXmlMetadataTest {

	private static ReposTestIndexing indexing;
	private static FilexmlSourceClasspath repoSource;
	private static CmsRepositoryFilexml repo;
	private static FilexmlRepositoryReadonly filexml;
	
	
	/**
	 * Manual dependency injection.
	 */
	@BeforeClass
	public static void setUpIndexing() {
		TestIndexOptions indexOptions = new TestIndexOptions().itemDefaultServices()
				.addCore("reposxml", "se/simonsoft/cms/indexing/xml/solr/reposxml/**")
				.addModule(new IndexingConfigXmlBase())
				.addModule(new IndexingConfigXmlDefault());
		indexing = ReposTestIndexing.getInstance(indexOptions);
		
		repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/metadata");
		repo = new CmsRepositoryFilexml("http://localtesthost/svn/namespace", repoSource);
		filexml = new FilexmlRepositoryReadonly(repo);
	}
	
	@AfterClass
	public static void tearDown() throws IOException {
		ReposTestIndexing.getInstance().tearDown();
	}
	
	// filexml backend could expose a https://github.com/hamcrest/JavaHamcrest matcher
	protected void assumeResourceExists(FilexmlSource source, String cmsItemPath) {
		assumeNotNull("Test skipped until large file " + cmsItemPath + " is exported",
				source.getFile(new CmsItemPath(cmsItemPath)));
	}
	
	@Test
	public void testMetadataBookmap() throws Exception {
		indexing.enable(new ReposTestBackendFilexml(filexml));
		assumeResourceExists(repoSource, "/bookmap1.ditamap");

		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList all = repositem.query(new SolrQuery("pathnamebase:bookmap1").setRows(2)).getResults();
		assertEquals(2, all.getNumFound()); 
		
		SolrDocument e1 = all.get(0);
		assertEquals("file", e1.getFieldValue("type"));
		assertEquals(true, e1.getFieldValue("head"));
		
		SolrDocument e2 = all.get(1);
		assertEquals("file", e2.getFieldValue("type"));
		assertEquals(false, e2.getFieldValue("head"));
		
		// Basics
		assertEquals("bookmap", e1.getFieldValue("embd_xml_name"));
		
		
		// Docno from bookmap
		assertEquals("BOM000", e1.getFieldValue("embd_xml_docno"));
		
		
		// Unified fields introduced in CMS 5.0
		// Additional unified fields on hold awaiting specification.
		assertEquals("Lifecycle_prodname ", e1.getFieldValue("embd_xml_meta_product"));
		
		
		// othermeta
		assertEquals(Arrays.asList("With double space"), e1.getFieldValue("meta_s_m_xml_a_othermeta_test_one"));
		assertEquals("With double space", e1.getFieldValue("meta_s_s_xml_a_othermeta_test_one"));
		
		assertEquals(Arrays.asList("nospace"), e1.getFieldValue("meta_s_m_xml_a_othermeta_test_two"));
		assertEquals("nospace", e1.getFieldValue("meta_s_s_xml_a_othermeta_test_two"));
		
		// No processing of separator, use multiple <othermeta>
		assertEquals(Arrays.asList("multi;value;separator"), e1.getFieldValue("meta_s_m_xml_a_othermeta_test_three"));
		assertEquals("multi;value;separator", e1.getFieldValue("meta_s_s_xml_a_othermeta_test_three"));
		
		// Demonstrate multiple othermeta with same name, becomes multivalue and singlevalue with newline separator.
		assertEquals(Arrays.asList("one", "two"), e1.getFieldValue("meta_s_m_xml_a_othermeta_multi-value"));
		assertEquals("one\ntwo", e1.getFieldValue("meta_s_s_xml_a_othermeta_multi-value"));
		
		assertEquals(Arrays.asList("content status", "lifecycle status"), e1.getFieldValue("meta_s_m_xml_a_othermeta_status"));
		assertEquals("content status\nlifecycle status", e1.getFieldValue("meta_s_s_xml_a_othermeta_status"));
		
		assertEquals(Arrays.asList("perhaps-included"), e1.getFieldValue("meta_s_m_xml_a_othermeta_outside_metadata"));
		assertEquals("perhaps-included", e1.getFieldValue("meta_s_s_xml_a_othermeta_outside_metadata"));
		
		// extended chars in field name
		assertEquals(Arrays.asList("Göteborg"), e1.getFieldValue("meta_s_m_xml_a_othermeta_h_gkvarter"));
		assertEquals("Göteborg", e1.getFieldValue("meta_s_s_xml_a_othermeta_h_gkvarter"));
		
		assertEquals(Arrays.asList("long name"), e1.getFieldValue("meta_s_m_xml_a_othermeta_0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"));
		assertEquals("long name", e1.getFieldValue("meta_s_s_xml_a_othermeta_0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"));
		
		
		
	}
	

}
