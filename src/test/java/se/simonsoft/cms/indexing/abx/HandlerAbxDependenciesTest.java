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
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.item.CmsRepository;

public class HandlerAbxDependenciesTest {

	@Test
	public void test() {
		String abxdeps = "x-svn:///svn/documentation^/graphics/cms/process/2.0/op-edit.png\n" +
				"x-svn:///svn/documentation^/xml/reference/cms/adapter/Introduction%20to%20CMS.xml\n" +
				"x-svn:///svn/documentation^/xml/reference/cms/User%20interface.xml?p=123";
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		when(p.getFields()).thenReturn(doc);
		when(p.getRepository()).thenReturn(new CmsRepository("http://host:123/svn/documentation"));
		doc.addField("repohost", "host:123");
		doc.addField("prop_abx.Dependencies", abxdeps);
		doc.addField("ref", "/existing/url");
		doc.addField("refurl", "http://host:123/existing/url");
		
		IndexingItemHandler handler = new HandlerAbxDependencies();
		handler.handle(p);
		Collection<Object> ref = doc.getFieldValues("ref");
		Collection<Object> refid = doc.getFieldValues("refid");
		Collection<Object> refurl = doc.getFieldValues("refurl");
		assertEquals("Should have added the three dependencies as refid", 3, refid.size());
		assertTrue(refid.contains("x-svn:///svn/documentation^/graphics/cms/process/2.0/op-edit.png"));
		assertTrue(refid.contains("x-svn:///svn/documentation^/xml/reference/cms/adapter/Introduction%20to%20CMS.xml"));
		assertTrue(refid.contains("x-svn:///svn/documentation^/xml/reference/cms/User%20interface.xml?p=123"));
		assertEquals("refurl should contain the already added url and one extra per dependency", 4, refurl.size());
		assertTrue(refurl.contains("http://host:123/svn/documentation/graphics/cms/process/2.0/op-edit.png"));
		assertTrue(refurl.contains("http://host:123/svn/documentation/xml/reference/cms/adapter/Introduction%20to%20CMS.xml"));
		assertTrue(refurl.contains("http://host:123/svn/documentation/xml/reference/cms/User%20interface.xml?p=123"));
		// TODO how to handle revision, extra refres field with plain resource URL (no query, no hash), maybe without protocol
	}

}
