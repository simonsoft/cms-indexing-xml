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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Map;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Test;
import org.tmatesoft.svn.core.io.SVNRepository;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.svn.runtime.SvnDumpConfig;

@QuarkusTest
@TestProfile(HandlerXmlRidDuplicateQuarkusIntegrationTest.Profile.class)
public class HandlerXmlRidDuplicateQuarkusIntegrationTest {

	@Inject
	Instance<SVNRepository> repositories;

	@Inject
	@Named("repositem")
	SolrClient repositem;

	@Inject
	@Named("reposxml")
	SolrClient reposxml;

	@Inject
	CmsRepository cmsRepository;

	@Inject
	IdStrategy idStrategy;

	@Test
	@ActivateRequestContext
	public void testTinyRidDuplicate() throws Exception {
		assertEquals(2L, repositories.get().getLatestRevision());

		SolrDocumentList x1 = reposxml.query(new SolrQuery("pathname:test1.xml").addSort("treelocation", ORDER.asc)).getResults();
		assertEquals("Should index all elements", 5, x1.getNumFound());
		assertEquals("should get 'repoid' from repositem", idStrategy.getIdRepository(cmsRepository),
				x1.get(0).getFieldValue("repoid"));

		SolrDocumentList flagged = repositem.query(new SolrQuery("pathname:test1.xml AND flag:hasxml AND head:true")).getResults();
		assertEquals("Documents that got added to reposxml should be flagged 'hasxml' in repositem", 1, flagged.getNumFound());
		Collection<Object> flags = flagged.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", flagged.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasxml'", flagged.get(0).getFieldValues("flag").contains("hasxml"));
		assertTrue("Flag 'hasridduplicate'", flagged.get(0).getFieldValues("flag").contains("hasridduplicate"));
		assertEquals("3 flag(s)", 3, flags.size());

		Collection<Object> duplicates = flagged.get(0).getFieldValues("embd_xml_ridduplicate");
		assertEquals("one duplicate, mentioned once", 1, duplicates.size());
		assertEquals("List the duplicate RIDs in repositem core", "2gyvymn15kv0002", duplicates.iterator().next());

		// Back to asserting on reposxml.
		assertEquals("second element", "section", x1.get(1).getFieldValue("name"));
		assertEquals("third element", "elem", x1.get(2).getFieldValue("name"));
		// No longer providing source, has been blocked by other extractor since a few years.
		//assertEquals("should extract source", "<elem xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" name=\"ch1\" cms:rid=\"2gyvymn15kv0002\">text</elem>", x1.get(2).getFieldValue("source"));
		assertEquals("should extract source_reuse", "<elem>text</elem>", x1.get(2).getFieldValue("source_reuse"));
	}

	@Test
	@ActivateRequestContext
	public void testTinyRidDuplicateTsuppress() throws Exception {
		assertEquals(2L, repositories.get().getLatestRevision());

		SolrDocumentList x1 = reposxml.query(new SolrQuery("pathname:test1-tsuppress.xml").addSort("treelocation", ORDER.asc)).getResults();
		assertEquals("Should index all elements", 5, x1.getNumFound());
		assertEquals("should get 'repoid' from repositem", idStrategy.getIdRepository(cmsRepository),
				x1.get(0).getFieldValue("repoid"));

		SolrDocumentList flagged = repositem.query(new SolrQuery("pathname:test1-tsuppress.xml AND flag:hasxml AND head:true")).getResults();
		assertEquals("Documents that got added to reposxml should be flagged 'hasxml' in repositem", 1, flagged.getNumFound());
		Collection<Object> flags = flagged.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", flagged.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasxml'", flagged.get(0).getFieldValues("flag").contains("hasxml"));
		assertFalse("Flag 'hasridduplicate'", flagged.get(0).getFieldValues("flag").contains("hasridduplicate"));
		assertTrue("Flag 'hastsuppress'", flagged.get(0).getFieldValues("flag").contains("hastsuppress"));
		assertEquals("only hasxml, hasxmlrepositem, hastsuppress flag", 3, flags.size());

		// Back to asserting on reposxml.
		assertEquals("second element", "section", x1.get(1).getFieldValue("name"));
		assertEquals("third element", "elem", x1.get(2).getFieldValue("name"));
		assertEquals("should extract source_reuse", "<elem>text</elem>", x1.get(2).getFieldValue("source_reuse"));
	}

	public static class Profile implements QuarkusTestProfile {

		@Override
		public Map<String, String> getConfigOverrides() {
			return Map.of(
					SvnDumpConfig.DATASET_PATH, "se/simonsoft/cms/indexing/xml/datasets/tiny-ridduplicate");
		}
	}
}
