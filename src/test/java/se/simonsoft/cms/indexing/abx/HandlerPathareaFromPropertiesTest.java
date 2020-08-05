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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;

public class HandlerPathareaFromPropertiesTest {

	@Test
	public void testMasterProps() {
		// Should identify Release/Translation based on Master-props.
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		IndexingItemProgress progress = mock(IndexingItemProgress.class);
		when(progress.getFields()).thenReturn(doc);
		HandlerPathareaFromProperties area = new HandlerPathareaFromProperties();
		
		area.handle(progress);
		assertNull("Not a release or translation", doc.getFieldValues("patharea"));
		assertEquals("Thus a 'main'", true, (Boolean) doc.getFieldValue("pathmain"));
		
		// Having only AuthorMaster is an undefined situation.
		doc.addField("prop_abx.AuthorMaster", "x-svn:///svn/demo1^/vvab/xml/Documents/900108.xml?p=100");
		doc.addField("prop_abx.ReleaseMaster", "x-svn:///svn/demo1^/vvab/xml/Documents/900108.xml?p=100");
		area.handle(progress);
		assertNotNull("Should have identified area", doc.getFieldValues("patharea"));
		assertEquals("Authormaster and ReleaseMaster", "release", doc.getFieldValues("patharea").iterator().next());
		assertEquals("Thus no longer author area", false, (Boolean) doc.getFieldValue("pathmain"));
		
		// Having only both RM and TM is an undefined situation.
		doc.removeField("prop_abx.ReleaseMaster");
		doc.addField("prop_abx.TranslationMaster", "x-svn:///svn/demo1^/vvab/xml/Documents/900108.xml?p=100");
		doc.removeField("patharea");
		area.handle(progress);
		assertNotNull("Should have identified area", doc.getFieldValues("patharea"));
		assertEquals("Has translation master", "translation", doc.getFieldValues("patharea").iterator().next());
		assertEquals("No longer relase", 1, doc.getFieldValues("patharea").size());
		assertEquals("Also not author area", false, (Boolean) doc.getFieldValue("pathmain"));
	}
	
	@Test
	public void testLabelProps() {
		// Should identify Release/Translation based on Label/Locale-props.
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		IndexingItemProgress progress = mock(IndexingItemProgress.class);
		when(progress.getFields()).thenReturn(doc);
		HandlerPathareaFromProperties area = new HandlerPathareaFromProperties();
		
		area.handle(progress);
		assertNull("Not a release or translation", doc.getFieldValues("patharea"));
		assertEquals("Thus a 'main'", true, (Boolean) doc.getFieldValue("pathmain"));
		
		doc.addField("prop_abx.ReleaseLabel", "X");
		area.handle(progress);
		assertNotNull("Should have identified area", doc.getFieldValues("patharea"));
		assertEquals("Authormaster but no locale", "release", doc.getFieldValues("patharea").iterator().next());
		assertEquals("Thus no longer author area", false, (Boolean) doc.getFieldValue("pathmain"));
		
		doc.addField("prop_abx.TranslationLocale", "pt-PT");
		doc.removeField("patharea");
		area.handle(progress);
		assertNotNull("Should have identified area", doc.getFieldValues("patharea"));
		assertEquals("Has translation master", "translation", doc.getFieldValues("patharea").iterator().next());
		assertEquals("No longer relase", 1, doc.getFieldValues("patharea").size());
		assertEquals("Also not author area", false, (Boolean) doc.getFieldValue("pathmain"));
	}
	
	
	@Test
	public void testGraphicTranslated() {
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		IndexingItemProgress progress = mock(IndexingItemProgress.class);
		when(progress.getFields()).thenReturn(doc);
		HandlerPathareaFromProperties area = new HandlerPathareaFromProperties();
		
		area.handle(progress);
		assertNull("Not a release or translation", doc.getFieldValues("patharea"));
		assertEquals("Thus a 'main'", true, (Boolean) doc.getFieldValue("pathmain"));
		
		// Translated graphics did not exist in CMS 1.0.
		doc.addField("prop_abx.AuthorMaster", "x-svn:///svn/demo1^/vvab/graphics/0005.tif?p=157");
		doc.addField("prop_abx.TranslationMaster", "x-svn:///svn/demo1^/vvab/graphics/0005.tif?p=157");
		doc.addField("prop_abx.TranslationLocale", "pt-PT");
		doc.removeField("patharea");
		area.handle(progress);
		assertNotNull("Should have identified area", doc.getFieldValues("patharea"));
		assertEquals("Also not author area", false, (Boolean) doc.getFieldValue("pathmain"));
		assertEquals("Has translation master", "translation", doc.getFieldValues("patharea").iterator().next());
		assertEquals("No longer relase", 1, doc.getFieldValues("patharea").size());

	}
	

	@Test
	public void testLegacyTranslations() {
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		IndexingItemProgress progress = mock(IndexingItemProgress.class);
		when(progress.getFields()).thenReturn(doc);
		HandlerPathareaFromProperties area = new HandlerPathareaFromProperties();
		
		area.handle(progress);
		assertNull("Not a release or translation", doc.getFieldValues("patharea"));
		assertEquals("Thus a 'main'", true, (Boolean) doc.getFieldValue("pathmain"));
		
		// translations created before the release concept don't have translation locale //doc.addField("prop_abx.TranslationLocale", "pt-PT");
		doc.addField("prop_abx.TranslationMaster", "x-svn:///svn/repo1^/demo/Documents/Presentation_B.xml?p=45");
		doc.removeField("patharea");
		area.handle(progress);
		assertNotNull("Should have identified area", doc.getFieldValues("patharea"));
		assertEquals("Has translation master", "translation10", doc.getFieldValues("patharea").iterator().next());
		assertEquals("No longer relase", 1, doc.getFieldValues("patharea").size());
		assertEquals("Also not author area", false, (Boolean) doc.getFieldValue("pathmain"));
		
	}	
	
}
