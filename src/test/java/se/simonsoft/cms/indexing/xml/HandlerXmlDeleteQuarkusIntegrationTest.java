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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

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
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import se.repos.indexing.solrj.SolrCommit;
import se.simonsoft.cms.indexing.xml.solr.XmlIndexWriterSolrj;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.svn.runtime.SvnDumpConfig;

@QuarkusTest
@TestProfile(HandlerXmlDeleteQuarkusIntegrationTest.Profile.class)
public class HandlerXmlDeleteQuarkusIntegrationTest {

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

	@Inject
	XmlIndexWriter xmlIndexWriter;

	@Test
	@ActivateRequestContext
	public void testNextRevisionDeletesElement() throws Exception {
		assertEquals(2L, repositories.get().getLatestRevision());

		SolrDocumentList x1 = reposxml.query(new SolrQuery("*:*").setSort("treelocation", ORDER.asc)).getResults();
		assertEquals(4, x1.getNumFound());
		assertEquals("should get 'repoid' from repositem", idStrategy.getIdRepository(cmsRepository),
				x1.get(0).getFieldValue("repoid"));
		assertEquals("should get 'pathfull' from repositem", cmsRepository.getPath() + "/test1.xml",
				x1.get(0).getFieldValue("pathfull"));

		SolrDocumentList flagged = repositem.query(new SolrQuery("flag:hasxml AND head:true")).getResults();
		assertEquals("Documents that got added to reposxml should be flagged 'hasxml' in repositem", 1,
				flagged.getNumFound());

		// Basic tests related to the deletePath implementation (avoiding the use of deleteByQuery due to performance).
		String idBase = idStrategy.getId(cmsRepository, new RepoRevision(2, null), new CmsItemPath("/test1.xml")) + "|";
		SolrDocument idDocument = x1.stream()
				.filter(document -> (idBase + "00000002").equals(document.getFieldValue("id")))
				.findFirst().orElseThrow();
		String idReposxml = (String) idDocument.getFieldValue("id");
		assertEquals("reposxml id format is vital for delete", idBase + "00000002", idReposxml);
		assertEquals("remove the element part of id", idBase, XmlIndexWriterSolrj.getIdBase(idDocument, null));

		// TODO delete one of the elements and make sure it is not there after indexing next revision, would indicate reliance on id overwrite

		// At least managed to test a faked delete.
		CmsChangesetItem c = mock(CmsChangesetItem.class);
		when(c.getPath()).thenReturn(new CmsItemPath("/test1.xml"));

		// Test the query
		SolrQuery qD = XmlIndexWriterSolrj.getDeleteQuery(cmsRepository, c);
		SolrDocumentList xD = reposxml.query(qD).getResults();
		assertEquals(4, xD.getNumFound());

		// Test actual delete
		xmlIndexWriter.deletePath(cmsRepository, c);
		new SolrCommit(reposxml, true).run();

		SolrDocumentList xDeleted = reposxml.query(new SolrQuery("*:*")).getResults();
		assertEquals(0, xDeleted.getNumFound());
	}

	public static class Profile implements QuarkusTestProfile {

		@Override
		public Map<String, String> getConfigOverrides() {
			return Map.of(
					SvnDumpConfig.DATASET_PATH, "se/simonsoft/cms/indexing/xml/datasets/tiny-inline");
		}
	}
}
