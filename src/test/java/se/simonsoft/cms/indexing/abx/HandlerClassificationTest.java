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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.ItemPropertiesBufferStrategy;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.properties.CmsItemProperties;
import se.simonsoft.cms.item.properties.CmsItemPropertiesMap;

public class HandlerClassificationTest {

	@Test
	public void testFolderShard() {
		CmsRepository repo = new CmsRepository("http://host:123/svn/demo1");
		String shardparent = "/vvab/xml/documents";
		String shardfolder = "/vvab/xml/documents/TOP_00000000";
		RepoRevision r = new RepoRevision(10L, null);
		
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		when(p.getFields()).thenReturn(doc);
		when(p.getRepository()).thenReturn(repo);
		when(p.getRevision()).thenReturn(r);
		doc.addField("repohost", "host:123");
		
		//CmsItemId itemId = repo.getItemId(new CmsItemPath(shardfolder), r.getNumber());
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		when(item.getPath()).thenReturn(new CmsItemPath(shardfolder));
		when(item.isFolder()).thenReturn(true);
		when(p.getItem()).thenReturn(item);
		
		ItemPropertiesBufferStrategy itemPropsStrategy = mock(ItemPropertiesBufferStrategy.class);
		CmsItemProperties propsParent = new CmsItemPropertiesMap("cms:class", "hej shardparent hopp");
		when(itemPropsStrategy.getProperties(r, new CmsItemPath(shardparent))).thenReturn(propsParent);
		IndexingItemHandler handler = new HandlerClassification(itemPropsStrategy);
		handler.handle(p);
		
		assertTrue(doc.containsKey("flag"));
		Collection<Object> flags = doc.getFieldValues("flag");
		assertEquals(1, flags.size());
		assertTrue("child should have isshard flag", flags.contains("isshard"));
		
		assertNull("not extracted for folder", doc.getFieldValue("meta_s_s_pathdirname"));
		assertNull("not extracted for folder", doc.getFieldValue("meta_s_s_pathdirnonshard"));
	}
	
	@Test
	public void testFileInShard() {
		CmsRepository repo = new CmsRepository("http://host:123/svn/demo1");
		String shardparent = "/vvab/xml/documents";
		String shardfolder = "/vvab/xml/documents/TOP_00000000";
		String file = "/vvab/xml/documents/TOP_00000000/file.xml";
		RepoRevision r = new RepoRevision(10L, null);
		
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		when(p.getFields()).thenReturn(doc);
		when(p.getRepository()).thenReturn(repo);
		when(p.getRevision()).thenReturn(r);
		doc.addField("repohost", "host:123");
		
		//CmsItemId itemId = repo.getItemId(new CmsItemPath(shardfolder), r.getNumber());
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		when(item.getPath()).thenReturn(new CmsItemPath(file));
		when(item.isFolder()).thenReturn(false);
		when(p.getItem()).thenReturn(item);
		
		ItemPropertiesBufferStrategy itemPropsStrategy = mock(ItemPropertiesBufferStrategy.class);
		CmsItemProperties propsParent = new CmsItemPropertiesMap("cms:class", "hej shardparent hopp");
		when(itemPropsStrategy.getProperties(r, new CmsItemPath(shardparent))).thenReturn(propsParent);
		IndexingItemHandler handler = new HandlerClassification(itemPropsStrategy);
		handler.handle(p);
		
		assertEquals("TOP_00000000", doc.getFieldValue("meta_s_s_pathdirname"));
		assertEquals("documents", doc.getFieldValue("meta_s_s_pathdirnonshard"));
	}
	
	@Test
	public void testFileFolder() {
		CmsRepository repo = new CmsRepository("http://host:123/svn/demo1");
		String shardparent = "/vvab/xml";
		String shardfolder = "/vvab/xml/documents";
		String file = "/vvab/xml/documents/file.xml";
		RepoRevision r = new RepoRevision(10L, null);
		
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		when(p.getFields()).thenReturn(doc);
		when(p.getRepository()).thenReturn(repo);
		when(p.getRevision()).thenReturn(r);
		doc.addField("repohost", "host:123");
		
		//CmsItemId itemId = repo.getItemId(new CmsItemPath(shardfolder), r.getNumber());
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		when(item.getPath()).thenReturn(new CmsItemPath(file));
		when(item.isFolder()).thenReturn(false);
		when(p.getItem()).thenReturn(item);
		
		ItemPropertiesBufferStrategy itemPropsStrategy = mock(ItemPropertiesBufferStrategy.class);
		CmsItemProperties propsParent = new CmsItemPropertiesMap("cms:class", "nada");
		when(itemPropsStrategy.getProperties(r, new CmsItemPath(shardparent))).thenReturn(propsParent);
		IndexingItemHandler handler = new HandlerClassification(itemPropsStrategy);
		handler.handle(p);
		
		assertEquals("documents", doc.getFieldValue("meta_s_s_pathdirname"));
		assertEquals("documents", doc.getFieldValue("meta_s_s_pathdirnonshard"));
	}
	
	@Test
	public void testFileProjectFolder() {
		CmsRepository repo = new CmsRepository("http://host:123/svn/demo1");
		String file = "/vvab/file.xml";
		RepoRevision r = new RepoRevision(10L, null);
		
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		when(p.getFields()).thenReturn(doc);
		when(p.getRepository()).thenReturn(repo);
		when(p.getRevision()).thenReturn(r);
		doc.addField("repohost", "host:123");
		
		//CmsItemId itemId = repo.getItemId(new CmsItemPath(shardfolder), r.getNumber());
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		when(item.getPath()).thenReturn(new CmsItemPath(file));
		when(item.isFolder()).thenReturn(false);
		when(p.getItem()).thenReturn(item);
		
		ItemPropertiesBufferStrategy itemPropsStrategy = mock(ItemPropertiesBufferStrategy.class);
		IndexingItemHandler handler = new HandlerClassification(itemPropsStrategy);
		handler.handle(p);
		
		assertEquals("vvab", doc.getFieldValue("meta_s_s_pathdirname"));
		assertEquals("vvab", doc.getFieldValue("meta_s_s_pathdirnonshard"));
	}

	
	@Test
	public void testFileRoot() {
		CmsRepository repo = new CmsRepository("http://host:123/svn/demo1");
		String file = "/file.xml";
		RepoRevision r = new RepoRevision(10L, null);
		
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		when(p.getFields()).thenReturn(doc);
		when(p.getRepository()).thenReturn(repo);
		when(p.getRevision()).thenReturn(r);
		doc.addField("repohost", "host:123");
		
		//CmsItemId itemId = repo.getItemId(new CmsItemPath(shardfolder), r.getNumber());
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		when(item.getPath()).thenReturn(new CmsItemPath(file));
		when(item.isFolder()).thenReturn(false);
		when(p.getItem()).thenReturn(item);
		
		ItemPropertiesBufferStrategy itemPropsStrategy = mock(ItemPropertiesBufferStrategy.class);
		IndexingItemHandler handler = new HandlerClassification(itemPropsStrategy);
		handler.handle(p);
		
		assertEquals(null, doc.getFieldValue("meta_s_s_pathdirname"));
		assertEquals(null, doc.getFieldValue("meta_s_s_pathdirnonshard"));
	}
}
