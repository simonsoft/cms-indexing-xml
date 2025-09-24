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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.simonsoft.cms.backend.svnkit.info.change.CmsChangesetReaderSvnkit;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.indexing.IdStrategyDefault;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.inspection.CmsChangesetReader;

/**
 *
 * @author Markus Mattsson
 */
public class HandlerAbxFoldersTest {
	
	@BeforeClass
	public static void setUpClass() {
	}
	
	@AfterClass
	public static void tearDownClass() {
	}
	
	@Before
	public void setUp() {
	}
	
	@After
	public void tearDown() {
	}

	/**
	 * Testing parent path handling for dependencies.
	 * Tested in scenariotesting now.
	 */
	@Test @Ignore
	public void testHandleDependenciesParents() {
		
		String abxdeps = "x-svn:///svn/documentation^/graphics/cms/process/2.0/op-edit.png\n" +
				"x-svn:///svn/documentation^/xml/reference/cms/adapter/Introduction%20to%20CMS.xml\n" +
				"x-svn:///svn/documentation^/xml/reference/cms/User_interface.xml?p=123";
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		when(p.getFields()).thenReturn(doc);
		when(p.getRepository()).thenReturn(new CmsRepository("http://host:123/svn/documentation"));
		doc.addField("repohost", "host:123");
		doc.addField("prop_abx.Dependencies", abxdeps);
		doc.addField("ref_link", "/existing/url/");
		doc.addField("refid", "host:123/existing/url/");
		
		IndexingItemHandler handler = new HandlerAbxDependencies(new IdStrategyDefault());
		handler.handle(p);
		System.out.println("ref_pathparents: " + doc.getFieldValues("ref_pathparents"));
		
		Collection<Object> pathParents = doc.getFieldValues("ref_pathparents");
		assertEquals("Expected ref_pathparents to be populated.", 11, pathParents.size());

	}
	
	/**
	 * Testing parent path handling for masters.
	 */
	@Test
	public void testHandleMastersParents() {

		String path = "/vvab/xml/documents/900108.xml";
		String authorMaster = "x-svn:///svn/demo1^/vvab/xml/documents/900108.xml?p=129";
		String translationMaster = "x-svn:///svn/demo1^/vvab/release/A/xml/documents/900108.xml?p=131";
		
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		CmsRepository repo = new CmsRepository("http://host:123/svn/demo1");
		when(item.getPath()).thenReturn(new CmsItemPath(path));
		when(p.getRevision()).thenReturn(new RepoRevision(129, null));
		when(p.getFields()).thenReturn(doc);
		when(p.getRepository()).thenReturn(repo);
		when(p.getItem()).thenReturn(item);
		doc.addField("repohost", "host:123");
		doc.addField("prop_abx.AuthorMaster", authorMaster);
		doc.addField("prop_abx.TranslationMaster", translationMaster);

		HandlerAbxMasters handler = new HandlerAbxMasters(new IdStrategyDefault());
		CmsChangesetReader changesetReader = mock(CmsChangesetReaderSvnkit.class);
		handler.setCmsChangesetReader(changesetReader);
		handler.handle(p);
		System.out.println("rel_abx.Masters_pathparents: " + doc.getFieldValues("rel_abx.Masters_pathparents"));
		
		Collection<Object> abxMastersPathParents = doc.getFieldValues("rel_abx.Masters_pathparents");
		
		assertEquals("Expected rel_abx.Masters_pathparents to be populated.", 8, abxMastersPathParents.size());
		
		// Also testing the individual Master fields and aggregated
		assertEquals(1, doc.getFieldValues("rel_abx.TranslationMaster").size());
		assertEquals(1, doc.getFieldValues("rel_abx.AuthorMaster").size());
		
		assertEquals("Expected rel_abx.Masters to be populated.", 2, doc.getFieldValues("rel_abx.Masters").size());
		assertTrue(doc.getFieldValues("rel_abx.Masters").contains("host:123/svn/demo1/vvab/release/A/xml/documents/900108.xml@0000000131"));
		
	}

}
