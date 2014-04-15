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
import static org.mockito.Mockito.*;

import java.util.Collection;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.simonsoft.cms.item.indexing.IdStrategyDefault;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.item.CmsRepository;

public class HandlerAbxDependenciesTest {

	@Test
	public void test() {
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
		doc.addField("refurl", "http://host:123/existing/url/");
		
		IndexingItemHandler handler = new HandlerAbxDependencies(new IdStrategyDefault());
		handler.handle(p);
		Collection<Object> refid = doc.getFieldValues("ref_abx.Dependencies");
		assertEquals("Should have added the dependencies as refid", 3, refid.size());
		assertTrue(refid.contains("host:123/svn/documentation/graphics/cms/process/2.0/op-edit.png"));
		assertTrue(refid.contains("host:123/svn/documentation/xml/reference/cms/adapter/Introduction%20to%20CMS.xml"));
		assertTrue(refid.contains("host:123/svn/documentation/xml/reference/cms/User_interface.xml@0000000123"));
		assertFalse("Revision-locked dependencies are irrelevant in the typical 'where used' use case, don't get false positives when using idhead search/join",
				refid.contains("host:123/svn/documentation/xml/reference/cms/User_interface.xml"));
		
	}
	
	@Test
	public void testAggregatedDependenciesOnly() {
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
		doc.addField("refurl", "http://host:123/existing/url/");
		
		IndexingItemHandler handler = new HandlerAbxDependencies(new IdStrategyDefault());
		handler.handle(p);
		Collection<Object> refid = doc.getFieldValues("refid");
		Collection<Object> refurl = doc.getFieldValues("refurl");
		assertEquals("Should have added the dependencies as refid", 3, refid.size());
		assertTrue(refid.contains("host:123/svn/documentation/graphics/cms/process/2.0/op-edit.png"));
		assertTrue(refid.contains("host:123/svn/documentation/xml/reference/cms/adapter/Introduction%20to%20CMS.xml"));
		assertTrue(refid.contains("host:123/svn/documentation/xml/reference/cms/User_interface.xml@0000000123"));
		assertFalse("Revision-locked dependencies are irrelevant in the typical 'where used' use case, don't get false positives when using idhead search/join",
				refid.contains("host:123/svn/documentation/xml/reference/cms/User_interface.xml"));
		assertEquals("refurl should contain the already added url and one extra per dependency", 4, refurl.size());
		assertTrue(refurl.contains("http://host:123/svn/documentation/graphics/cms/process/2.0/op-edit.png"));
		assertTrue(refurl.contains("http://host:123/svn/documentation/xml/reference/cms/adapter/Introduction%20to%20CMS.xml"));
		assertTrue(refurl.contains("http://host:123/svn/documentation/xml/reference/cms/User_interface.xml?p=123"));
		assertFalse("Revision should be set in URLs",
				refurl.contains("http://host:123/svn/documentation/xml/reference/cms/User_interface.xml"));
		assertTrue("Should preserve existing URLs", refurl.contains("http://host:123/existing/url/"));
		
		// pathparents are tested in HandlerAbxFoldersTest.java
	}
	
	@Test
	public void testAggregated() {
		String abxdeps = "x-svn:///svn/documentation^/graphics/cms/process/2.0/op-edit.png\n";
		String abxcross = "x-svn:///svn/documentation^/xml/reference/cms/adapter/Introduction%20to%20CMS.xml\n";
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		when(p.getFields()).thenReturn(doc);
		when(p.getRepository()).thenReturn(new CmsRepository("http://host:123/svn/documentation"));
		doc.addField("repohost", "host:123");
		doc.addField("prop_abx.Dependencies", abxdeps);
		doc.addField("prop_abx.CrossRefs", abxcross);
		doc.addField("ref_link", "/existing/url/");
		doc.addField("refurl", "http://host:123/existing/url/");
		
		IndexingItemHandler handler = new HandlerAbxDependencies(new IdStrategyDefault());
		handler.handle(p);
		Collection<Object> refdep = doc.getFieldValues("ref_abx.Dependencies");
		Collection<Object> refcross = doc.getFieldValues("ref_abx.CrossRefs");
		
		assertEquals("Should have added the dependency as refid", 1, refdep.size());
		assertTrue(refdep.contains("host:123/svn/documentation/graphics/cms/process/2.0/op-edit.png"));
		assertTrue(refcross.contains("host:123/svn/documentation/xml/reference/cms/adapter/Introduction%20to%20CMS.xml"));
		
		Collection<Object> refid = doc.getFieldValues("refid");
		assertEquals("Should have aggregated references in refid", 2, refid.size());
		
		Collection<Object> refurl = doc.getFieldValues("refurl");
		assertTrue(refurl.contains("http://host:123/svn/documentation/graphics/cms/process/2.0/op-edit.png"));
		assertTrue(refurl.contains("http://host:123/svn/documentation/xml/reference/cms/adapter/Introduction%20to%20CMS.xml"));

		Collection<Object> refpathparents = doc.getFieldValues("ref_pathparents");
		assertEquals("Should have aggregated references pathparents", 8, refpathparents.size()); //guessing the number

	}


}
