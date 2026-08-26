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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Test;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNRepository;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import se.repos.indexing.ReposIndexing;
import se.repos.indexing.scheduling.IndexingSchedule;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.svn.runtime.RepoId;
import se.simonsoft.svn.runtime.SvnDumpConfig;
import se.simonsoft.svn.runtime.SvnRevisionAvailableEvent;

@QuarkusTest
@TestProfile(HandlerXmlQuarkusIntegrationTest.Profile.class)
public class HandlerXmlQuarkusIntegrationTest {

	@Inject
	Instance<SVNRepository> repositories;

	@Inject
	SvnDatasetRevisionEvents events;

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
	public void testTinyInline() throws Exception {
		events.clear();

		SVNRepository repository = repositories.get();

		assertEquals(2L, repository.getLatestRevision());
		assertEquals(SVNNodeKind.FILE, repository.checkPath("test1.xml", 2));
		assertEquals(List.of(SvnDatasetRepoIdProducer.REPO_ID + " 1", SvnDatasetRepoIdProducer.REPO_ID + " 2"), events.revisions());

		SolrDocumentList x1 = reposxml.query(new SolrQuery("*:*").setSort("treelocation", ORDER.asc)).getResults();
		assertEquals(4, x1.getNumFound());
		assertEquals("should get 'repoid' from repositem", idStrategy.getIdRepository(cmsRepository),
				x1.get(0).getFieldValue("repoid"));

		SolrDocumentList flagged = repositem.query(new SolrQuery("flag:hasxml AND head:true")).getResults();
		assertEquals("Documents that got added to reposxml should be flagged 'hasxml' in repositem", 1,
				flagged.getNumFound());
		Collection<Object> flags = flagged.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", flagged.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasxml'", flagged.get(0).getFieldValues("flag").contains("hasxml"));
		assertTrue("Flag 'hasxmlrepositem'", flagged.get(0).getFieldValues("flag").contains("hasxmlrepositem"));
		assertFalse("Flag 'hasridduplicate'", flagged.get(0).getFieldValues("flag").contains("hasridduplicate"));
		assertEquals("", 2, flags.size());

		// Statistics in repositem schema
		assertEquals("Should count elements", 4L, flagged.get(0).getFieldValue("count_elements"));
		assertEquals("Should count words", 3L, flagged.get(0).getFieldValue("count_words_text"));
		assertNull("not calculated, no RID", flagged.get(0).getFieldValue("count_words_translate"));

		// DOCTYPE in repositem schema
		assertEquals("repositem root element name", "document", flagged.get(0).getFieldValue("embd_xml_typename"));
		assertEquals("repositem systemid", "techdoc.dtd", flagged.get(0).getFieldValue("embd_xml_typesystem"));
		assertEquals("repositem publicid", "-//Simonsoft//DTD TechDoc Base V1.0 Techdoc//EN",
				flagged.get(0).getFieldValue("embd_xml_typepublic"));

		// Depth for reposxml
		assertEquals("null since item is not a translation", null,
				flagged.get(0).getFieldValue("count_reposxml_depth"));

		// Reposxml
		assertEquals("Should index all elements", 4, x1.size());

		assertEquals("document/root element name", "doc", x1.get(0).getFieldValue("name"));
		assertEquals("element pos", "1", x1.get(0).getFieldValue("treelocation"));
		assertEquals("all elements", 4L, x1.get(0).getFieldValue("count_elements"));
		assertEquals("word count identical to repositem (document element)", 3L,
				x1.get(0).getFieldValue("count_words_text"));
		assertEquals("word count translate", 3L, x1.get(0).getFieldValue("count_words_translate"));
		assertEquals("word count child (immediate text)", 0L, x1.get(0).getFieldValue("count_words_child"));
		//assertEquals("Currently not including 'hasxml': " + flags.toString(), 1, x1.get(0).getFieldValues("flag").size());
		assertNull("no flags in reposxml at this time", x1.get(0).getFieldValues("flag"));

		assertEquals("cms namespace", "http://www.simonsoft.se/namespace/cms", x1.get(0).getFieldValue("ns_cms"));
		assertNull("cmsrepoxml ns suppressed", x1.get(0).getFieldValue("ns_cmsreposxml"));

		assertEquals("document/root element name", "elem", x1.get(2).getFieldValue("name"));
		assertEquals("element pos", "1.2", x1.get(2).getFieldValue("treelocation"));
		assertEquals("elements below", 2L, x1.get(2).getFieldValue("count_elements"));
		assertEquals("word count", 2L, x1.get(2).getFieldValue("count_words_text"));
		assertEquals("word count child (immediate text)", 1L, x1.get(2).getFieldValue("count_words_child"));

		assertNull("no ns on element", x1.get(2).getFieldValue("ns_cms"));
		assertNull("no ns on element", x1.get(2).getFieldValue("ns_cmsreposxml"));
		assertEquals("inherited cms namespace", "http://www.simonsoft.se/namespace/cms",
				x1.get(2).getFieldValue("ins_cms"));
		assertNull("inherited cmsrepoxml ns suppressed", x1.get(2).getFieldValue("ins_cmsreposxml"));

		// The "typename" is quite debatable because the test document has an incorrect DOCTYPE declaration (root element is "doc" not "document").
		// Now keeping DOCTYPE in repositem.
		/*
		assertEquals("should set root element name", "document", x1.get(0).getFieldValue("typename"));
		assertEquals("should set systemid", "techdoc.dtd", x1.get(0).getFieldValue("typesystem"));
		assertEquals("should set publicid", "-//Simonsoft//DTD TechDoc Base V1.0 Techdoc//EN", x1.get(0).getFieldValue("typepublic"));
		*/
		assertEquals("should extract source", "<elem>text</elem>", x1.get(1).getFieldValue("source_reuse"));
	}

	public static class Profile implements QuarkusTestProfile {

		@Override
		public Map<String, String> getConfigOverrides() {
			return Map.ofEntries(
					Map.entry(SvnDumpConfig.DATASET_PATH, "se/simonsoft/cms/indexing/xml/datasets/tiny-inline"),
					Map.entry("quarkus.solr.enabled", "true"),
					Map.entry("quarkus.solr.devservices.cores.repositem.config-path", "se/repos/indexing/solr/repositem"),
					Map.entry("quarkus.solr.devservices.cores.reposxml.config-path", "se/simonsoft/cms/indexing/xml/solr/reposxml"),
					// Solr multi-core owns the named repositem client in this Quarkus test.
					Map.entry("quarkus.arc.exclude-types", "se.repos.indexing.config.RepositemSolrClientProducer"));
		}
	}
}

@ApplicationScoped
class SvnDatasetRepoIdProducer {

	static final String REPO_ID = "cms-indexing-xml-dataset";

	@Produces
	@RepoId
	public String produceRepoId() {
		return REPO_ID;
	}
}

@ApplicationScoped
class SvnDatasetRevisionEvents {

	private final List<String> revisions = new ArrayList<>();

	@Inject
	ReposIndexing indexing;

	@Inject
	IndexingSchedule schedule;

	void onRevisionAvailable(@Observes SvnRevisionAvailableEvent event) {
		revisions.add(event.repoId() + " " + event.revision());
		schedule.start();
		try {
			indexing.sync(new RepoRevision(event.revision(), null));
		} finally {
			schedule.stop();
		}
	}

	void clear() {
		revisions.clear();
	}

	List<String> revisions() {
		return List.copyOf(revisions);
	}
}
