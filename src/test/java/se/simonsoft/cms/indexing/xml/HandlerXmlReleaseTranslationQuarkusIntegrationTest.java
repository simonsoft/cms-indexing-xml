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

import java.util.Arrays;
import java.util.Map;

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

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import se.simonsoft.svn.runtime.SvnDumpConfig;

@QuarkusTest
@TestProfile(HandlerXmlReleaseTranslationQuarkusIntegrationTest.Profile.class)
public class HandlerXmlReleaseTranslationQuarkusIntegrationTest {

	@Inject
	Instance<SVNRepository> repositories;

	@Inject
	@Named("reposxml")
	SolrClient reposxml;

	@Test
	@ActivateRequestContext
	public void testAttributesReleasetranslationRelease() throws Exception {
		assertEquals(14L, repositories.get().getLatestRevision());

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

	public static class Profile implements QuarkusTestProfile {

		@Override
		public Map<String, String> getConfigOverrides() {
			return Map.of(
					SvnDumpConfig.DATASET_PATH, "se/simonsoft/cms/indexing/xml/datasets/releasetranslation");
		}
	}
}
