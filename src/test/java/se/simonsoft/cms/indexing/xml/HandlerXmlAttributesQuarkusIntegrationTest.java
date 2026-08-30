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

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Test;
import org.tmatesoft.svn.core.io.SVNRepository;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class HandlerXmlAttributesQuarkusIntegrationTest extends DatasetQuarkusTest {

	@Inject
	Instance<SVNRepository> repositories;

	@Inject
	@Named("reposxml")
	SolrClient reposxml;

	@Test
	@ActivateRequestContext
	public void testTinyAttributes() throws Exception {
		loadDataset("se/simonsoft/cms/indexing/xml/datasets/tiny-attributes");
		assertEquals(2L, repositories.get().getLatestRevision());

		SolrQuery q1 = new SolrQuery("*:*").addSort("treelocation", SolrQuery.ORDER.asc);
		SolrDocumentList x1 = reposxml.query(q1).getResults();
		assertEquals(4, x1.getNumFound());

		assertEquals("get name of root", "root", x1.get(0).getFieldValue("a_name"));
		assertEquals("get depth of root", 1, x1.get(0).getFieldValue("depth"));
		assertEquals("get pos/treeloc of root", "1", x1.get(0).getFieldValue("treelocation"));
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
		assertEquals("get pos/treeloc of inline", "1.2.1", x1.get(3).getFieldValue("treelocation"));
		assertNull("get p-sibling name of inline", x1.get(3).getFieldValue("sa_name"));
	}

}
