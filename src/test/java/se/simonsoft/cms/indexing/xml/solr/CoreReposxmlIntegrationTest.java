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
package se.simonsoft.cms.indexing.xml.solr;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Verify features of the actual index, without using our abstractions.
 * This test should match stuff that we rely on for direct queries to solr from various places.
 * Create new test methods per integration case.
 */
@QuarkusTest
public class CoreReposxmlIntegrationTest {

	@Inject
	@Named("reposxml")
	SolrClient reposxml;

	@AfterEach
	public void clearIndex() throws SolrServerException, IOException {
		reposxml.deleteByQuery("*:*");
		reposxml.commit();
	}

	@Test
	public void testCommon() throws SolrServerException, IOException {
		assertEquals("index should be empty on each test run", 0,
				reposxml.query(new SolrQuery("*:*")).getResults().getNumFound());

		SolrInputDocument doc1 = new SolrInputDocument();
		doc1.addField("id", "x");
		doc1.addField("name", "x");
		doc1.addField("treelocation", "1");
		reposxml.add(doc1);
		reposxml.commit();

		QueryResponse query = reposxml.query(new SolrQuery("*:*"));
		assertEquals(1, query.getResults().getNumFound());
	}

}
