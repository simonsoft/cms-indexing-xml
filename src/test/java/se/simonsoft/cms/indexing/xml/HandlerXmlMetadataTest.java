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
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeNotNull;

import java.io.IOException;
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
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
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
		
		// No of fields, just during development
		//assertEquals(81, e1.getFieldNames().size());
		
		
		// Basics
		assertEquals("bookmap", e1.getFieldValue("embd_xml_name"));
		
		
		// Docno from bookmap
		assertEquals("1234 ABCD", e1.getFieldValue("embd_xml_docno"));
		assertEquals("BOM000", e1.getFieldValue("embd_xml_partno"));
		
		
		// Unified fields introduced in CMS 5.0
		// Additional unified fields on hold awaiting specification.
		assertEquals("Lifecycle_prodname ", e1.getFieldValue("embd_xml_meta_product"));
		
		// prodinfo (supports multiple prodinfo, see "Another prodinfo")
		// TODO: Consider filtering whole "meta" element to remove elements with xml:lang != document lang.
		assertEquals(Arrays.asList("Lifecycle prodname"), e1.getFieldValue("meta_s_m_xml_prodinfo_prodname"));
		assertEquals("Lifecycle prodname", e1.getFieldValue("meta_s_s_xml_prodinfo_prodname"));
		assertEquals(Arrays.asList("The Product name", "Another prodinfo"), e1.getFieldValue("meta_s_m_xml_metadata_prodinfo_prodname"));
		assertEquals("The Product name\nAnother prodinfo", e1.getFieldValue("meta_s_s_xml_metadata_prodinfo_prodname"));

		assertEquals(null, e1.getFieldValue("meta_s_s_xml_prodinfo_brand"));
		assertEquals("Amazing", e1.getFieldValue("meta_s_s_xml_metadata_prodinfo_brand"));
		assertEquals(null, e1.getFieldValue("meta_s_s_xml_prodinfo_component"));
		assertEquals("A", e1.getFieldValue("meta_s_s_xml_metadata_prodinfo_component"));
		assertEquals(null, e1.getFieldValue("meta_s_s_xml_prodinfo_platform"));
		assertEquals("Left\nRight", e1.getFieldValue("meta_s_s_xml_metadata_prodinfo_platform"));
		
		assertNull(e1.getFieldValue("meta_s_m_xml_prodinfo_series"));
		assertNull(e1.getFieldValue("meta_s_s_xml_prodinfo_series"));
		assertEquals(Arrays.asList("Knatte", "Fnatte", "Tjatte"), e1.getFieldValue("meta_s_m_xml_metadata_prodinfo_series"));
		assertEquals("Knatte\nFnatte\nTjatte", e1.getFieldValue("meta_s_s_xml_metadata_prodinfo_series"));
		
		// bookid
		assertEquals("BOM000\nBOM123", e1.getFieldValue("meta_s_s_xml_bookid_bookpartno"));
		assertEquals("A.10", e1.getFieldValue("meta_s_s_xml_bookid_edition"));
		assertEquals("ISBN", e1.getFieldValue("meta_s_s_xml_bookid_isbn"));
		assertEquals("1234 ABCD", e1.getFieldValue("meta_s_s_xml_bookid_booknumber"));
		
		
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
	
	@Test
	public void testMetadataTechdocmap1() throws Exception {
		assumeResourceExists(repoSource, "/techdocmap1.ditamap");

		SolrClient repositem = indexing.getCore("repositem");
		SolrDocumentList all = repositem.query(new SolrQuery("pathnamebase:techdocmap1").setRows(2)).getResults();
		assertEquals(2, all.getNumFound()); 
		
		SolrDocument e1 = all.get(0);
		assertEquals("file", e1.getFieldValue("type"));
		assertEquals(true, e1.getFieldValue("head"));
		
		SolrDocument e2 = all.get(1);
		assertEquals("file", e2.getFieldValue("type"));
		assertEquals(false, e2.getFieldValue("head"));
		
		// No of fields, just during development
		//assertEquals(81, e1.getFieldNames().size());
		
		
		// Basics
		assertEquals("techdocmap", e1.getFieldValue("embd_xml_name"));
		
		
		// Docno from bookmap
		assertEquals("1234 ABCD", e1.getFieldValue("embd_xml_docno"));
		assertEquals("BOM000", e1.getFieldValue("embd_xml_partno"));
		
		
		// Unified fields introduced in CMS 5.0
		// Additional unified fields on hold awaiting specification.
		assertEquals("The_Product_name Another_prodinfo ", e1.getFieldValue("embd_xml_meta_product"));
		
		
		// techdocinfo
		assertEquals(Arrays.asList("The Product name", "Another prodinfo"), e1.getFieldValue("meta_s_m_xml_product"));
		assertEquals("The Product name\nAnother prodinfo", e1.getFieldValue("meta_s_s_xml_product"));
		
		assertEquals(Arrays.asList("Model A", "Model B"), e1.getFieldValue("meta_s_m_xml_model"));
		assertEquals("Model A\nModel B", e1.getFieldValue("meta_s_s_xml_model"));
		
		assertEquals("1234 ABCD\n4567 QWER", e1.getFieldValue("meta_s_s_xml_docno"));
		assertEquals("BOM000\nBOM123", e1.getFieldValue("meta_s_s_xml_partno"));		
		assertEquals("A0001\nB2000", e1.getFieldValue("meta_s_s_xml_serialno"));
		
		
		// prodinfo (supports multiple prodinfo, see "Another prodinfo")
		// TODO: Consider filtering whole "meta" element to remove elements with xml:lang != document lang.
		assertEquals(Arrays.asList("The Product name"), e1.getFieldValue("meta_s_m_xml_metadata_prodinfo_prodname"));
		assertEquals("The Product name", e1.getFieldValue("meta_s_s_xml_metadata_prodinfo_prodname"));

		assertEquals("Amazing", e1.getFieldValue("meta_s_s_xml_metadata_prodinfo_brand"));
		assertEquals("A", e1.getFieldValue("meta_s_s_xml_metadata_prodinfo_component"));
		assertEquals("Left\nRight", e1.getFieldValue("meta_s_s_xml_metadata_prodinfo_platform"));
		
		assertNull(e1.getFieldValue("meta_s_m_xml_prodinfo_series"));
		assertNull(e1.getFieldValue("meta_s_s_xml_prodinfo_series"));
		assertEquals(Arrays.asList("Knatte", "Fnatte", "Tjatte"), e1.getFieldValue("meta_s_m_xml_metadata_prodinfo_series"));
		assertEquals("Knatte\nFnatte\nTjatte", e1.getFieldValue("meta_s_s_xml_metadata_prodinfo_series"));
		
		
		// othermeta (see bookmap1 for full test suite)
		// Demonstrate multiple othermeta with same name, becomes multivalue and singlevalue with newline separator.
		assertEquals(Arrays.asList("one", "two"), e1.getFieldValue("meta_s_m_xml_a_othermeta_multi-value"));
		assertEquals("one\ntwo", e1.getFieldValue("meta_s_s_xml_a_othermeta_multi-value"));
	}
	

}
