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

import java.io.IOException;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.tmatesoft.svn.core.io.SVNRepository;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import se.simonsoft.svn.runtime.SvnDataset;

@QuarkusTest
@TestProfile(MockableSvnDatasetProfile.class)
public class HandlerXmlInvalidQuarkusIntegrationTest {

	private static final SvnDataset DATASET = new SvnDataset(
			"se/simonsoft/cms/indexing/xml/datasets/tiny-invalid", 2);

	@Inject
	Instance<SVNRepository> repositories;

	@Inject
	@Named("repositem")
	SolrClient repositem;

	@Inject
	@Named("reposxml")
	SolrClient reposxml;

	@AfterEach
	public void clearIndexes() throws SolrServerException, IOException {
		repositem.deleteByQuery("*:*");
		repositem.commit();
		reposxml.deleteByQuery("*:*");
		reposxml.commit();
	}

	@Test
	@ActivateRequestContext
	public void testInvalidXml() throws Exception {
		QuarkusMock.installMockForType(DATASET, SvnDataset.class);

		assertEquals(DATASET.revision(0), repositories.get().getLatestRevision());

		SolrDocumentList x1 = reposxml.query(new SolrQuery("*:*")).getResults();
		assertEquals("Should skip the document because it is not parseable as XML. Thus we can try formats that may be XML, such as html, without breaking indexing.",
				0, x1.getNumFound());

		SolrDocumentList flagged = repositem.query(new SolrQuery("flag:hasxmlerror AND head:true")).getResults();
		assertEquals("Should be flagged as error in repositem", 1, flagged.getNumFound());
	}
}
