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

import java.util.Iterator;
import java.util.Set;

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
public class HandlerXmlReleaseLabelQuarkusIntegrationTest extends DatasetQuarkusTest {

	@Inject
	Instance<SVNRepository> repositories;

	@Inject
	@Named("repositem")
	SolrClient repositem;

	@Test
	@ActivateRequestContext
	public void testReleaseLabelSort1() throws Exception {
		loadDataset("se/simonsoft/cms/indexing/xml/datasets/releaselabels");
		assertEquals(9L, repositories.get().getLatestRevision());

		SolrDocumentList rlLegacy = repositem.query(new SolrQuery("patharea:release AND head:true").setSort("prop_abx.ReleaseLabel", ORDER.asc).setFields("*")).getResults();
		assertEquals("no of releases", 8, rlLegacy.getNumFound());

		// The Legacy string based sorting
		// Case variants have the same lowercased sort key, so their relative order is undefined.
		Iterator<SolrDocument> itLegacy = rlLegacy.iterator();
		assertEquals("10", itLegacy.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals("2", itLegacy.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals(Set.of("ab", "AB"), Set.of(
				itLegacy.next().getFieldValue("prop_abx.ReleaseLabel"),
				itLegacy.next().getFieldValue("prop_abx.ReleaseLabel")));
		assertEquals("AB-beta", itLegacy.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals("AB.1", itLegacy.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals(Set.of("b", "B"), Set.of(
				itLegacy.next().getFieldValue("prop_abx.ReleaseLabel"),
				itLegacy.next().getFieldValue("prop_abx.ReleaseLabel")));

		SolrDocumentList rlSort = repositem.query(new SolrQuery("patharea:release AND head:true").setSort("meta_s_s_releaselabel_sort", ORDER.asc).setFields("*")).getResults();
		assertEquals("no of releases", 8, rlSort.getNumFound());

		// The correct SemVer sorting (via String in SolR)
		Iterator<SolrDocument> itSort = rlSort.iterator();
		assertEquals("2", itSort.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals("10", itSort.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals("B", itSort.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals("AB-beta", itSort.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals("AB", itSort.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals("AB.1", itSort.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals("b", itSort.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals("ab", itSort.next().getFieldValue("prop_abx.ReleaseLabel"));
	}

}
