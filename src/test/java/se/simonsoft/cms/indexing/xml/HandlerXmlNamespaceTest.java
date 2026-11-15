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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Test;
import org.tmatesoft.svn.core.io.SVNRepository;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import se.simonsoft.svn.runtime.SvnDataset;

@QuarkusTest
@TestProfile(MockableSvnDatasetProfile.class)
public class HandlerXmlNamespaceTest extends MockableSvnDatasetTest {

	private static final SvnDataset XML_DATASET = new SvnDataset(
			"se/simonsoft/cms/indexing/xml/datasets/namespace-xml");

	private static final SvnDataset XHTML_DATASET = new SvnDataset(
			"se/simonsoft/cms/indexing/xml/datasets/namespace-xhtml");

	@Inject
	Instance<SVNRepository> repositories;

	@Inject
	@Named("reposxml")
	SolrClient reposxml;

	@Test
	@ActivateRequestContext
	public void testNamespaceXhtml() throws Exception {
		QuarkusMock.installMockForType(XHTML_DATASET, SvnDataset.class);

		assertEquals(2, repositories.get().getLatestRevision());

		SolrDocumentList all = reposxml.query(new SolrQuery("*:*").setRows(2).setSort("treelocation", ORDER.asc)).getResults();
		assertEquals(13, all.getNumFound());

		SolrDocument e1 = all.get(0);
		assertEquals("html", e1.getFieldValue("name"));
		// Solr allows the wildcard part of dynamic fields to be empty.
		// TODO: Is ns_ what we want or would we like to define as "ns"?
		assertEquals("declared ns", "http://www.w3.org/1999/xhtml", e1.getFieldValue("ns_"));
		assertEquals("inherited and declared ns", "http://www.w3.org/1999/xhtml", e1.getFieldValue("ins_"));
		assertNotNull("used as well", e1.getFieldValue("uns_"));

		SolrDocument e2 = all.get(1);
		assertEquals("head", e2.getFieldValue("name"));
		assertNull("not declared here", e2.getFieldValue("ns_"));
		assertEquals("inherited ns", "http://www.w3.org/1999/xhtml", e2.getFieldValue("ins_"));
	}

	@Test
	@ActivateRequestContext
	public void testNamespaceXml() throws Exception {
		QuarkusMock.installMockForType(XML_DATASET, SvnDataset.class);

		assertEquals(2, repositories.get().getLatestRevision());

		SolrDocumentList all = reposxml.query(new SolrQuery("*:*").setRows(5).setSort("treelocation", ORDER.asc)).getResults();
		assertEquals(5, all.getNumFound()); 
		
		SolrDocument e1 = all.get(0);
		assertEquals("doc", e1.getFieldValue("name"));
		
		assertEquals("declared ns", "http://www.simonsoft.se/namespace/cms", e1.getFieldValue("ns_cms"));
		assertEquals("inherited and declared ns", "http://www.simonsoft.se/namespace/cms", e1.getFieldValue("ins_cms"));
		
		assertEquals("declared ns", "http://www.simonsoft.se/namespace/test1", e1.getFieldValue("ns_cms1"));
		assertEquals("declared ns", "http://www.simonsoft.se/namespace/test2", e1.getFieldValue("ns_cms2"));
		assertEquals("declared ns", "http://www.simonsoft.se/namespace/test3", e1.getFieldValue("ns_cms3"));
		
		assertNull("no default ns", e1.getFieldValue("uns_"));
		assertNotNull("xml namespace actually included", e1.getFieldValue("uns_xml"));
		
		assertNotNull("used", e1.getFieldValue("uns_cms"));
		assertNotNull("used", e1.getFieldValue("uns_cms1"));
		assertNotNull("used", e1.getFieldValue("uns_cms2"));
		assertNull(e1.getFieldValue("uns_cms3"));
		e1 = null;
		
		SolrDocument e2 = all.get(1);
		assertEquals("elem", e2.getFieldValue("name"));
		assertEquals("ch1", e2.getFieldValue("a_name"));
		assertNull("not declared here", e2.getFieldValue("ns_cms"));
		assertNull("not declared here", e2.getFieldValue("ns_cms1"));
		assertNull("not declared here", e2.getFieldValue("ns_cms2"));
		assertNull("not declared here", e2.getFieldValue("ns_cms3"));
		
		assertNotNull("inherited", e2.getFieldValue("ins_cms"));
		assertNotNull("inherited", e2.getFieldValue("ins_cms1"));
		assertNotNull("inherited", e2.getFieldValue("ins_cms2"));
		assertNotNull("inherited", e2.getFieldValue("ins_cms3"));
		
		assertNull("no default ns", e2.getFieldValue("uns_"));
		assertNull("no xml namespace below", e2.getFieldValue("uns_xml"));
		
		assertNull(e2.getFieldValue("uns_cms"));
		assertNotNull("used", e2.getFieldValue("uns_cms1"));
		assertNull(e2.getFieldValue("uns_cms2"));
		assertNull(e2.getFieldValue("uns_cms3"));
		
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
		
		assertNull("used", e3.getFieldValue("uns_cms"));
		assertNotNull("used", e3.getFieldValue("uns_cms1"));
		assertNull("used", e3.getFieldValue("uns_cms2"));
		assertNull("used", e3.getFieldValue("uns_cms3"));
	
		e3 = null;
		
		SolrDocument e4 = all.get(3);
		assertEquals("elem", e4.getFieldValue("name"));
		assertEquals("e2", e4.getFieldValue("a_id"));
		assertNull("not declared here", e4.getFieldValue("ns_cms"));
		assertNull("not declared here", e4.getFieldValue("ns_cms1"));
		assertNull("not declared here", e4.getFieldValue("ns_cms2"));
		assertNull("not declared here", e4.getFieldValue("ns_cms3"));
		
		assertNotNull("inherited", e4.getFieldValue("ins_cms"));
		assertNotNull("inherited", e4.getFieldValue("ins_cms1"));
		assertNotNull("inherited", e4.getFieldValue("ins_cms2"));
		assertNotNull("inherited", e4.getFieldValue("ins_cms3"));
		
		assertNotNull("used", e4.getFieldValue("uns_cms2"));
		
		e4 = null;
	}

}
