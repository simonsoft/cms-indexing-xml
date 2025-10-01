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
package se.simonsoft.cms.indexing.abx;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.backend.svnkit.info.change.CmsChangesetReaderSvnkit;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.cms.item.indexing.IdStrategyDefault;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class HandlerAbxMastersTest {

	@Mock
	private CmsChangesetReaderSvnkit changesetReader;

	@Mock
	private CmsChangesetItem changesetItem;

	private IdStrategy idStrategy;
	private HandlerAbxMasters handler;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		idStrategy = new IdStrategyDefault();
		handler = new HandlerAbxMasters(idStrategy);
		handler.setCmsChangesetReader(changesetReader);
	}

	@Test
	public void testCommitRevisionWithPegRev() {
		IndexingItemProgress progress = mock(IndexingItemProgress.class);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		when(progress.getFields()).thenReturn(doc);
		when(progress.getRepository()).thenReturn(new CmsRepository("http://host:123/svn/documentation"));
		when(progress.getItem()).thenReturn(changesetItem);
		when(changesetItem.isAdd()).thenReturn(true);

		doc.addField("repohost", "host:123");
		doc.addField("prop_abx.ReleaseMaster", "x-svn:///svn/documentation/xml/test.xml?p=456");

		// Commit revision lower than peg.
		RepoRevision commitRev = new RepoRevision(400L, null);
		when(changesetReader.getChangedRevision(any(CmsItemPath.class), eq(456L))).thenReturn(commitRev);

		handler.handle(progress);

		assertEquals("host:123/svn/documentation/xml/test.xml@0000000456", doc.getFieldValue("rel_abx.ReleaseMaster"));

		assertTrue("Should have rel_commit_abx.ReleaseMaster field", doc.getFieldNames().contains("rel_commit_abx.ReleaseMaster"));
		assertEquals("host:123/svn/documentation/xml/test.xml@0000000400", doc.getFieldValue("rel_commit_abx.ReleaseMaster"));

		verify(changesetReader).getChangedRevision(any(CmsItemPath.class), eq(456L));
	}

	@Test
	public void testCommitRevisionWithNullCommitRev() {
		IndexingItemProgress progress = mock(IndexingItemProgress.class);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		when(progress.getFields()).thenReturn(doc);
		when(progress.getRepository()).thenReturn(new CmsRepository("http://host:123/svn/documentation"));
		when(progress.getItem()).thenReturn(changesetItem);
		when(changesetItem.isAdd()).thenReturn(true);

		doc.addField("repohost", "host:123");
		doc.addField("prop_abx.ReleaseMaster", "x-svn:///svn/documentation/xml/test.xml?p=456");

		when(changesetReader.getChangedRevision(any(CmsItemPath.class), eq(456L))).thenReturn(null);

		handler.handle(progress);

		assertEquals("host:123/svn/documentation/xml/test.xml@0000000456", doc.getFieldValue("rel_abx.ReleaseMaster"));

		assertFalse("Should not have rel_commit_abx.ReleaseMaster field when commit revision is null", doc.getFieldNames().contains("rel_commit_abx.ReleaseMaster"));

		verify(changesetReader).getChangedRevision(any(CmsItemPath.class), eq(456L));
	}

	@Test
	public void testNoPegRevisionSkipsCommitRevisionLogic() {
		IndexingItemProgress progress = mock(IndexingItemProgress.class);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		when(progress.getFields()).thenReturn(doc);
		when(progress.getRepository()).thenReturn(new CmsRepository("http://host:123/svn/documentation"));
		when(progress.getItem()).thenReturn(changesetItem);
		when(changesetItem.isAdd()).thenReturn(true);

		doc.addField("repohost", "host:123");
		doc.addField("prop_abx.ReleaseMaster", "x-svn:///svn/documentation/xml/test.xml");

		handler.handle(progress);

		assertEquals("host:123/svn/documentation/xml/test.xml", doc.getFieldValue("rel_abx.ReleaseMaster"));

		assertFalse("Should not have rel_commit_abx.ReleaseMaster field when no peg revision", doc.getFieldNames().contains("rel_commit_abx.ReleaseMaster"));

		verify(changesetReader, never()).getChangedRevision(any(CmsItemPath.class), anyLong());
	}
}
