/**
 * Copyright (C) 2009-2013 Simonsoft Nordic AB
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.impl.CmsItemIdArg;

public class HandlerLogicalIdFromPropertyBaseLogicalIdTest {

	@Test
	public void testHandle() {
		String baseLogicalId = "x-svn:///svn/documentation^/xml/reference/cms/User%20interface.xml?p=133";
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		CmsChangesetItem pi = mock(CmsChangesetItem.class);
		when(pi.getRevisionChanged()).thenReturn(new RepoRevision(145, null));
		when(p.getItem()).thenReturn(pi);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		when(p.getFields()).thenReturn(doc);
		when(p.getRepository()).thenReturn(new CmsRepository("http://host:123/svn/documentation"));
		doc.addField("repohost", "host:123");
		doc.addField("prop_abx.BaseLogicalId", baseLogicalId);
		
		IndexingItemHandler handler = new HandlerLogicalIdFromProperty();
		handler.handle(p);
		String urlid = (String) doc.getFieldValue("urlid");
		assertNotNull("Should set field", urlid);
		CmsItemIdArg id = new CmsItemIdArg(urlid);
		assertEquals("/xml/reference/cms/User interface.xml", id.getRelPath().getPath());
		assertEquals("Revision should not be from base but from commit rev", new Long(145), id.getPegRev());
	}

	@Test
	public void testHandleAdd() {
		String baseLogicalId = "x-svn:///svn/repo1^/qa/Documents/Bursting/Bursting%20Document%201.xml?p=-1";
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		CmsChangesetItem pi = mock(CmsChangesetItem.class);
		when(pi.getRevisionChanged()).thenReturn(new RepoRevision(99999, null));
		when(p.getItem()).thenReturn(pi);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		when(p.getFields()).thenReturn(doc);
		when(p.getRepository()).thenReturn(new CmsRepository("http://host:123/svn/documentation"));
		doc.addField("repohost", "host:123");
		doc.addField("prop_abx.BaseLogicalId", baseLogicalId);
		
		IndexingItemHandler handler = new HandlerLogicalIdFromProperty();
		handler.handle(p);
		String urlid = (String) doc.getFieldValue("urlid");
		assertNotNull("Should set field", urlid);
		CmsItemIdArg id = new CmsItemIdArg(urlid);
		assertEquals("/qa/Documents/Bursting/Bursting Document 1.xml", id.getRelPath().getPath());
		assertEquals("Revision should not be from base but from commit rev", new Long(99999), id.getPegRev());
	}
	
}
