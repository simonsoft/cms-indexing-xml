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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

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
import se.simonsoft.cms.indexing.xml.XmlIndexingGuard;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;

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

	@Test
	public void abortedRevisionCleanupPreservesOlderAndNewerDocuments() throws Exception {
		addRevision(1, "/repo/test.xml");
		addRevision(3, "/repo/test.xml");
		addRevision(2, "/other/test.xml");
		reposxml.commit();
		// The failed batch need not be searchable yet. Cleanup must still remove it.
		addRevision(2, "/repo/test.xml");
		new XmlIndexWriterSolrj(reposxml).deleteRevision(repository(), item(), new RepoRevision(2, null));
		reposxml.commit();
		assertEquals(Set.of(1L, 3L), revisions("/repo/test.xml"));
		assertEquals(Set.of(2L), revisions("/other/test.xml"));
	}

	@Test
	public void replacementBoundsMixedRevisionFallback() throws Exception {
		addRevision(1, "/repo/test.xml");
		addRevision(2, "/repo/test.xml");
		addRevision(3, "/repo/test.xml");
		reposxml.commit();
		new XmlIndexWriterSolrj(reposxml).deletePath(repository(), item(), new RepoRevision(2, null),
				new XmlIndexingGuard(() -> true));
		reposxml.commit();
		assertEquals(Set.of(3L), revisions("/repo/test.xml"));
	}

	@Test
	public void replacementKeepsEfficientIdDeletionWithinRevisionBound() throws Exception {
		addRevision(1, "/repo/test.xml");
		addRevision(3, "/repo/test.xml");
		reposxml.commit();
		boolean allowed = XmlIndexWriterSolrj.deleteByQueryAllowed;
		try {
			XmlIndexWriterSolrj.deleteByQueryAllowed = false;
			new XmlIndexWriterSolrj(reposxml).deletePath(repository(), item(), new RepoRevision(2, null),
					new XmlIndexingGuard(() -> true));
		} finally {
			XmlIndexWriterSolrj.deleteByQueryAllowed = allowed;
		}
		reposxml.commit();
		assertEquals(Set.of(3L), revisions("/repo/test.xml"));
	}

	@Test
	public void staleDeleteLeavesAllRevisionsUntouched() throws Exception {
		addRevision(1, "/repo/test.xml");
		addRevision(3, "/repo/test.xml");
		reposxml.commit();
		new XmlIndexWriterSolrj(reposxml).deletePath(repository(), item(), new RepoRevision(2, null),
				new XmlIndexingGuard(() -> false));
		reposxml.commit();
		assertEquals(Set.of(1L, 3L), revisions("/repo/test.xml"));
	}

	private void addRevision(long revision, String path) throws Exception {
		SolrInputDocument doc = new SolrInputDocument();
		doc.setField("id", path + "@" + revision + "|00000001");
		doc.setField("pathfull", path);
		doc.setField("rev", revision);
		doc.setField("depth", 1);
		doc.setField("treelocation", "1");
		doc.setField("name", "doc");
		reposxml.add(doc);
	}

	private Set<Long> revisions(String path) throws Exception {
		return reposxml.query(new SolrQuery("pathfull:" + XmlIndexWriterSolrj.quote(path)))
				.getResults().stream().map(d -> (Long) d.getFieldValue("rev")).collect(Collectors.toSet());
	}

	private CmsRepository repository() {
		CmsRepository repository = mock(CmsRepository.class);
		when(repository.getPath()).thenReturn("/repo");
		return repository;
	}

	private CmsChangesetItem item() {
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		when(item.getPath()).thenReturn(new CmsItemPath("/test.xml"));
		return item;
	}

}
