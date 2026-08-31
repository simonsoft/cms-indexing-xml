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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Test;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNRepository;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import se.repos.indexing.ReposIndexing;
import se.repos.indexing.scheduling.IndexingSchedule;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.svn.runtime.RepoId;
import se.simonsoft.svn.runtime.SvnDataset;
import se.simonsoft.svn.runtime.SvnRevisionAvailableEvent;

@QuarkusTest
@TestProfile(MockableSvnDatasetProfile.class)
public class HandlerXmlQuarkusIntegrationTest extends MockableSvnDatasetTest {

	private static final SvnDataset DATASET = new SvnDataset(
			"se/simonsoft/cms/indexing/xml/datasets/tiny-inline", 2);

	@Inject
	Instance<SVNRepository> repositories;

	@Inject
	@RepoId
	Instance<String> repoIds;

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
		QuarkusMock.installMockForType(DATASET, SvnDataset.class);
		events.clear();
		String repoId = repoIds.get();
		SVNRepository repository = repositories.get();

		assertEquals(DATASET.revision(0), repository.getLatestRevision());
		assertEquals(SVNNodeKind.FILE, repository.checkPath("test1.xml", 2));
		assertEquals(List.of(repoId + " 1", repoId + " 2"), events.revisions());

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

	@Test
	@ActivateRequestContext
	public void testJoin() throws SolrServerException, IOException, SVNException {
		QuarkusMock.installMockForType(DATASET, SvnDataset.class);

		assertEquals(DATASET.revision(0), repositories.get().getLatestRevision());

		SolrDocumentList j1 = reposxml.query(new SolrQuery("{!join from=id to=id_p}*:*")).getResults();
		assertEquals("all elements that have a parent, got " + j1, 3, j1.getNumFound());
		for (SolrDocument e : j1) {
			assertNotEquals("root does not have a parent", "doc", e.getFieldValue("name"));
		}

		SolrDocumentList j2 = reposxml.query(new SolrQuery("{!join from=id_p to=id}*:*")).getResults();
		assertEquals("all elements that have a child, got " + j2, 2, j2.getNumFound());

		SolrDocumentList j3 = reposxml.query(new SolrQuery("{!join from=id_p to=id}name:inline")).getResults();
		assertEquals("all elements that have a child which is an <inline/>, got " + j3, 1, j3.getNumFound());
		assertEquals("elem", j3.get(0).getFieldValue("name"));
		String expectedElementId = idStrategy.getId(cmsRepository, new RepoRevision(2, null),
				new CmsItemPath("/test1.xml")) + "|00000003";
		assertEquals(expectedElementId, j3.get(0).getFieldValue("id"));

		SolrDocumentList j4 = reposxml.query(new SolrQuery("name:elem AND {!join from=id_p to=id}*:*")).getResults();
		assertEquals("all elements that are an elem and have a child, got " + j4, 1, j4.getNumFound());
		assertEquals(expectedElementId, j4.get(0).getFieldValue("id"));

		SolrDocumentList j5 = reposxml.query(new SolrQuery("{!join from=id_p to=id}(name:elem OR name:inline)")).getResults();
		assertEquals("all elements that have a child which is either <elem/> or <inline/>" + j5, 2, j5.getNumFound());

		// why doesn't this run? instead use Parameter dereferencing?
		//SolrDocumentList j6 = reposxml.query(new SolrQuery("repo:tiny-inline AND {!join from=id_p to=id}(name:elem OR name:inline)")).getResults();
		//assertEquals("all elements that have a child which is either <elem/> or <inline/>, in the test repo" + j6, 2, j6.getNumFound());

		SolrDocumentList j7 = reposxml.query(new SolrQuery("{!join from=id_p to=id}(text:\"elem text\" AND name:elem)")).getResults();
		assertEquals("elements that have a child which matches two criterias" + j7, 1, j7.getNumFound());

		SolrDocumentList j8 = reposxml.query(new SolrQuery("{!join from=id_a to=id}name:inline")).getResults();
		assertEquals("elements with a descendat which is an <inline/>, got " + j8, 2, j8.getNumFound());

		// "Parameter dereferencing", http://wiki.apache.org/solr/LocalParams#parameter_dereferencing, but how to do "qq" in solrj?
//		// find all figures with a bylinew with value "me"
//		assertJQ(req("q", "{!join from=id_p to=id v=$qq}",
//					"qq", "name:byline AND pos:1.2.2", // we don't have text indexed in this test so we use pos instead
//					//"qf", "name",
//					"fl", "id",
//					"debugQuery", "true"),
//				"/response=={'numFound':1,'start':0,'docs':[{'id':'testdoc1_e3'}]}");
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

	List<String> revisions() {
		return List.copyOf(revisions);
	}

	void clear() {
		revisions.clear();
	}
}
