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
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

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

import io.quarkus.arc.ClientProxy;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangeset;
import se.simonsoft.cms.item.inspection.CmsChangesetReader;

@QuarkusTest
public class HandlerXmlLargeFileQuarkusIntegrationTest extends DatasetQuarkusTest {

	private static final String DATASET_PATH = "se/simonsoft/cms/indexing/xml/datasets/single-860k";
	private static final String DATASET_FILE = DATASET_PATH + "/T501007.xml";

	private static final Map<String, String> CHECKSUMS = Map.of(
			"p", "c30f06122daa3fde28755ea85f59c14d0d5ac073",
			"title", "b5aa8764d806e08f75b3face83d742115fad7a05",
			"entry", "ae614be4301722538d5efcf071e92f536113996c",
			"row", "c275cb2a3e59784bce03478d189cc251646374d2",
			"table", "598b6e604ec60130e91534701fb4694413daca38",
			"section", "80248e3cf0f8353d952b00fad0e5e79bb0e4050f",
			"body", "6a63852186cf1fb4ecaaa8d139d0278c89f519ab",
			"document", "f6a2c5d40f6cad4b4223101a9b12d28127d4f8e2");

	@Inject
	Instance<SVNRepository> repositories;

	@Inject
	Instance<CmsChangesetReader> changesetReaders;

	@Inject
	@Named("reposxml")
	SolrClient reposxml;

	@Test
	@ActivateRequestContext
	public void testSingle860k() throws Exception {
		assumeTrue(datasetAvailable(), "Test skipped until large file /T501007.xml is exported");
		CmsChangesetReader changesetReader = ClientProxy.unwrap(changesetReaders.get());
		QuarkusMock.installMockForType(new CmsChangesetReader() {
			@Override
			public CmsChangeset read(RepoRevision revision) {
				return changesetReader.read(revision);
			}

			@Override
			public CmsChangeset read(RepoRevision revision, RepoRevision referenceRevision) {
				return changesetReader.read(revision, referenceRevision);
			}

			@Override
			public RepoRevision getChangedRevision(CmsItemPath path, long revision) {
				// Filexml returned no match for historical references outside this one-revision dataset.
				return revision > 1 ? null : changesetReader.getChangedRevision(path, revision);
			}
		}, CmsChangesetReader.class);

		loadDataset(DATASET_PATH);
		assertEquals(1L, repositories.get().getLatestRevision());

		SolrDocumentList all = reposxml.query(new SolrQuery("*:*").setRows(1)).getResults();
		assertEquals(11488, all.getNumFound());

		SolrDocumentList pathmain = reposxml.query(new SolrQuery("pathmain:true").setRows(1)).getResults();
		assertEquals(0, pathmain.getNumFound());

		SolrDocumentList area = reposxml.query(new SolrQuery("patharea:*").setRows(1)).getResults();
		assertEquals(11488, area.getNumFound());

		SolrDocumentList releases = reposxml.query(new SolrQuery("patharea:release").setRows(1)).getResults();
		assertEquals(11488, releases.getNumFound());

		SolrDocumentList translations = reposxml.query(new SolrQuery("patharea:translation").setRows(1)).getResults();
		assertEquals(0, translations.getNumFound());

		SolrDocumentList releaseTop = reposxml.query(new SolrQuery("patharea:release AND depth:1").setRows(1)).getResults();
		assertEquals(1, releaseTop.getNumFound());
		Collection<Object> releaseDescendants = releaseTop.get(0).getFieldValues("reuse_c_sha1_release_descendants");
		assertNotNull(releaseDescendants);
		assertEquals(10544, releaseDescendants.size());

		for (Entry<String, String> checksum : CHECKSUMS.entrySet()) {
			SolrDocumentList elements = reposxml.query(new SolrQuery("name:" + checksum.getKey())
					.setRows(1).addSort("treelocation", ORDER.asc)).getResults();
			assertEquals("checksum for first " + checksum.getKey(), checksum.getValue(),
					elements.get(0).getFieldValue("c_sha1_source_reuse"));
		}
	}

	private static boolean datasetAvailable() {
		return HandlerXmlLargeFileQuarkusIntegrationTest.class.getClassLoader().getResource(DATASET_FILE) != null;
	}

}
