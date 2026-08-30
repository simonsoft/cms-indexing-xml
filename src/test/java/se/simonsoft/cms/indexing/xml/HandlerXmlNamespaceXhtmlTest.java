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

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Test;
import org.tmatesoft.svn.core.io.SVNRepository;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class HandlerXmlNamespaceXhtmlTest extends DatasetQuarkusTest {

	@Inject
	Instance<SVNRepository> repositories;

	@Inject
	@Named("reposxml")
	SolrClient reposxml;

	@Test
	@ActivateRequestContext
	public void testNamespaceXhtml() throws Exception {
		loadDataset("se/simonsoft/cms/indexing/xml/datasets/namespace-xhtml");
		assertEquals(2L, repositories.get().getLatestRevision());

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

}
