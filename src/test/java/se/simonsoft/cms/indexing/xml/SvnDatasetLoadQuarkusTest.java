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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
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
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Test;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNRepository;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import se.repos.indexing.ReposIndexing;
import se.repos.indexing.scheduling.IndexingSchedule;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.svn.runtime.RepoId;
import se.simonsoft.svn.runtime.SvnDumpConfig;
import se.simonsoft.svn.runtime.SvnRevisionAvailableEvent;

@QuarkusTest
@TestProfile(SvnDatasetLoadQuarkusTest.Profile.class)
public class SvnDatasetLoadQuarkusTest {

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

	@Test
	@ActivateRequestContext
	public void testLoadDatasetIntoSvnDevService() throws Exception {
		events.clear();

		SVNRepository repository = repositories.get();

		assertEquals(2L, repository.getLatestRevision());
		assertEquals(SVNNodeKind.FILE, repository.checkPath("test1.xml", 2));
		assertEquals(List.of(SvnDatasetRepoIdProducer.REPO_ID + " 1", SvnDatasetRepoIdProducer.REPO_ID + " 2"), events.revisions());

		SolrDocumentList indexedXmlItems = repositem.query(
				new SolrQuery("pathname:test1.xml AND flag:hasxml AND head:true")).getResults();
		assertEquals(1, indexedXmlItems.getNumFound());
		assertEquals(4, reposxml.query(new SolrQuery("pathname:test1.xml")).getResults().getNumFound());
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
