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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
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
public class HandlerXmlReleaseTranslationQuarkusIntegrationTest extends MockableSvnDatasetTest {

	private static final SvnDataset DATASET = new SvnDataset(
			"se/simonsoft/cms/indexing/xml/datasets/releasetranslation", 9);

	@Inject
	Instance<SVNRepository> repositories;

	@Inject
	@Named("reposxml")
	SolrClient reposxml;

	@Inject
	@Named("repositem")
	SolrClient repositem;

	@Test
	@ActivateRequestContext
	public void testReleaseTranslationTitleText() throws Exception {
		QuarkusMock.installMockForType(DATASET, SvnDataset.class);

		assertEquals(DATASET.revision(0), repositories.get().getLatestRevision());

		SolrDocumentList doc = repositem.query(new SolrQuery("patharea:release AND flag:hasxml AND head:true")).getResults();
		assertEquals("Document should exist", 1, doc.getNumFound());
		assertEquals("NOTE: This filexml repo was manually created, file name does not match.", "My First Novel.xml", doc.get(0).getFieldValue("pathname"));

		Collection<Object> flags = doc.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", flags.contains(""));
		assertTrue("Flag 'hasxml'", flags.contains("hasxml"));
		assertEquals("2 flag(s)", 2, flags.size());

		assertEquals("word count excl keyref", 24L, doc.get(0).getFieldValue("count_words_text"));
		assertEquals("", "My First Novel", doc.get(0).getFieldValue("embd_xml_title"));
		assertEquals("", "Once upon a time...\nSubchapters are quite rare in novels.", doc.get(0).getFieldValue("embd_xml_intro"));
	}

	@Test
	@ActivateRequestContext
	public void testAttributesReleasetranslationRelease() throws Exception {
		QuarkusMock.installMockForType(DATASET, SvnDataset.class);

		assertEquals(DATASET.revision(0), repositories.get().getLatestRevision());

		SolrDocument elem;
		// search for the first title
		SolrDocumentList findUsingRid = reposxml.query(new SolrQuery("a_cms.rid:2gyvymn15kv0001 AND -prop_abx.TranslationLocale:*")).getResults();
		assertEquals("Should find the first title in the release (though actually a future one)", 1, findUsingRid.getNumFound());
		elem = findUsingRid.get(0);
		assertEquals("get the rid attribute", "2gyvymn15kv0001", elem.getFieldValue("a_cms.rid"));
		assertEquals("get the parent rlogicalid", "x-svn:///svn/testaut1^/tms/xml/Docs/My%20First%20Novel.xml?p=5", elem.getFieldValue("ia_cms.rlogicalid"));

		findUsingRid = reposxml.query(new SolrQuery("a_cms.rid:2gyvymn15kv0006 AND -prop_abx.TranslationLocale:*")).getResults();
		assertEquals("Should find a para", 1, findUsingRid.getNumFound());
		elem = findUsingRid.get(0);
		assertEquals("verify it is a para", "p", elem.getFieldValue("name"));
		assertEquals("get the rid attribute", "2gyvymn15kv0006", elem.getFieldValue("a_cms.rid"));
		assertEquals("get the ancestor rid attribute (in this case parent rid)", "2gyvymn15kv0004", elem.getFieldValue("aa_cms.rid"));
		assertEquals("get the inherited rid attribute (in this case context element rid)", "2gyvymn15kv0006", elem.getFieldValue("ia_cms.rid"));
		assertEquals("get the root rid attribute", "2gyvymn15kv0000", elem.getFieldValue("ra_cms.rid"));
		assertEquals("get the preceding sibling rid attribute", "2gyvymn15kv0005", elem.getFieldValue("sa_cms.rid"));
		assertNull("get the project id attribute", elem.getFieldValue("a_cms.translation-project"));

		assertEquals("get the inherited rlogicalid attribute", "x-svn:///svn/testaut1^/tms/xml/Secs/First%20chapter.xml?p=4", elem.getFieldValue("ia_cms.rlogicalid"));

		assertEquals("assist depends on patharea", Arrays.asList(new String[] {"release"}), elem.getFieldValue("patharea"));
		assertEquals("assist depends on reusevalue even for a Release", 1, elem.getFieldValue("reusevalue"));
	}

	@SuppressWarnings("unchecked")
	@Test
	@ActivateRequestContext
	public void testAttributesReleasetranslationTranslation() throws Exception {
		QuarkusMock.installMockForType(DATASET, SvnDataset.class);

		assertEquals(DATASET.revision(0), repositories.get().getLatestRevision());

		SolrDocumentList flagged = repositem.query(new SolrQuery("flag:hasxml AND head:true")).getResults();
		assertEquals("Documents that got added to reposxml should be flagged 'hasxml' in repositem", 2, flagged.getNumFound());
		assertNull("Should NOT limit depth of Release", flagged.get(0).getFieldValue("count_reposxml_depth"));
		assertEquals("Should limit depth of Translation", 1L, flagged.get(1).getFieldValue("count_reposxml_depth"));
		assertEquals("no of topics - 3 techdoc sections", 3L, flagged.get(1).getFieldValue("count_elements_topic"));

		SolrDocumentList findAll = reposxml.query(new SolrQuery("prop_abx.TranslationLocale:*")).getResults();
		assertEquals("Should find all elements in the single translation", 1, findAll.getNumFound());
		assertEquals("Should limit reposxml extraction depth", 1L, findAll.get(0).getFieldValue("count_reposxml_depth"));

		SolrDocumentList findUsingRid0 = reposxml.query(new SolrQuery("a_cms.rid:2gyvymn15kv0000 AND prop_abx.TranslationLocale:*")).getResults();
		assertEquals("Should find root element in the Translation", 1, findUsingRid0.getNumFound());
		SolrDocument elem0 = findUsingRid0.get(0);

		String ridStr = (String) elem0.getFieldValue("reuseridreusevalue");
		assertEquals("number of elements is 13, verified",  13, ridStr.split(" ").length);
		assertEquals("RIDs with reusevalue > 0", "2gyvymn15kv0000 2gyvymn15kv0001 2gyvymn15kv0002 2gyvymn15kv0003 2gyvymn15kv0004 2gyvymn15kv0005 2gyvymn15kv0006 2gyvymn15kv0007 2gyvymn15kv0008 2gyvymn15kv0009 2gyvymn15kv000a 2gyvymn15kv000b 2gyvymn15kv000c ", ridStr);

		List<String> cList = (List<String>) elem0.getFieldValue("reuse_c_sha1_release_descendants");
		//assertEquals("debug contents", "...", cList);
		assertTrue("should contain Release checksum", cList.contains("c5fed03ed1304cecce75d63aee2ada2b0f2326af"));
		Collection<Object> shard = elem0.getFieldValues("reuse_rid_c5");
		assertNotNull(shard);
		assertEquals("number of RIDs in shard 'c5'", 1, shard.size());
		assertEquals("get RID by checksum", "c5fed03ed1304cecce75d63aee2ada2b0f2326af 2gyvymn15kv0006", shard.iterator().next());
	}

	@Test
	@ActivateRequestContext
	public void testJoinReleasetranslationNoExtraFields() throws Exception {
		QuarkusMock.installMockForType(DATASET, SvnDataset.class);

		assertEquals(DATASET.revision(0), repositories.get().getLatestRevision());

		// search for the first title
		SolrDocumentList findUsingRid = reposxml.query(new SolrQuery("a_cms.rid:2gyvymn15kv0001 AND -prop_abx.TranslationLocale:*")).getResults();
		assertEquals("Should find the first title in the release (though actually a future one)", 1, findUsingRid.getNumFound());
		String wantedReleaseSha1 = (String) findUsingRid.get(0).getFieldValue("c_sha1_source_reuse");

		SolrDocumentList findAllMatchesWithoutJoin = reposxml.query(new SolrQuery("c_sha1_source_reuse:" + wantedReleaseSha1)).getResults();
		assertEquals("Could search for the checksum in all xml", 1, findAllMatchesWithoutJoin.getNumFound());

		SolrQuery q = new SolrQuery("c_sha1_source_reuse:" + wantedReleaseSha1
				// Because we join on the same filed name we must explicitly state that the hit should be a release, or In_Translation items would join with themselves and match
				// Do we have a release specific field that is not copied to translations? For now just exclude translations.
				+ " AND -prop_abx.TranslationLocale:*"
				// Haven't found how to combine two criterias on the join into a single join, when there's also criteria on the actual match (see join test above)
				+ " AND {!join from=prop_abx.AuthorMaster to=prop_abx.AuthorMaster}prop_abx.TranslationLocale:sv-SE"
				+ " AND {!join from=prop_abx.AuthorMaster to=prop_abx.AuthorMaster}reusevalue:1");
		SolrDocumentList findReusevalue = reposxml.query(q).getResults();
		assertEquals(1, findReusevalue.getNumFound());
		// TODO with the current data set it is impossible to assert that we don't get false positives with the above query
		// Would need another release with an Obsolete sv-SE translation and a reusevalue=1 de-DE one, which probably would match falsely
	}

}
