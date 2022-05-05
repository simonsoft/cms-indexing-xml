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

public class HandlerReleaseLabelTest {

	
	@Test
	public void testLabelProps() {
		// Should identify Release/Translation based on Label/Locale-props.
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		IndexingItemProgress progress = mock(IndexingItemProgress.class);
		when(progress.getFields()).thenReturn(doc);
		HandlerReleaseLabel area = new HandlerReleaseLabel();
		
		doc.addField("prop_abx.ReleaseLabel", "X.10");
		area.handle(progress);
		assertEquals("X.10", doc.getFieldValue("meta_s_s_releaselabel"));
		assertEquals("@@@@@@@@@X.////////10-", doc.getFieldValue("meta_s_s_releaselabel_sort"));
		assertEquals("X", doc.getFieldValue("meta_s_s_releaselabel_version0"));
		assertEquals("@@@@@@@@@X", doc.getFieldValue("meta_s_s_releaselabel_version0_sort"));
		assertEquals("10", doc.getFieldValue("meta_s_s_releaselabel_version1"));
		assertEquals("////////10", doc.getFieldValue("meta_s_s_releaselabel_version1_sort"));
		
		assertEquals(null, doc.getFieldValue("meta_s_s_releaselabel_version2"));
		assertEquals(null, doc.getFieldValue("meta_s_s_releaselabel_version2_sort"));
		
		assertEquals(null, doc.getFieldValue("meta_s_s_releaselabel_prerelease0"));
		assertEquals(null, doc.getFieldValue("meta_s_s_releaselabel_prerelease0_sort"));
	}
	

	@Test
	public void testLabelPropsPrerelease() {
		// Should identify Release/Translation based on Label/Locale-props.
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		IndexingItemProgress progress = mock(IndexingItemProgress.class);
		when(progress.getFields()).thenReturn(doc);
		HandlerReleaseLabel area = new HandlerReleaseLabel();
		
		doc.addField("prop_abx.ReleaseLabel", "X.10-beta.1");
		area.handle(progress);
		assertEquals("X.10-beta.1", doc.getFieldValue("meta_s_s_releaselabel"));
		assertEquals("@@@@@@@@@X.////////10+beta./////////1", doc.getFieldValue("meta_s_s_releaselabel_sort"));
		assertEquals("X", doc.getFieldValue("meta_s_s_releaselabel_version0"));
		assertEquals("@@@@@@@@@X", doc.getFieldValue("meta_s_s_releaselabel_version0_sort"));
		assertEquals("10", doc.getFieldValue("meta_s_s_releaselabel_version1"));
		assertEquals("////////10", doc.getFieldValue("meta_s_s_releaselabel_version1_sort"));
		
		assertEquals(null, doc.getFieldValue("meta_s_s_releaselabel_version2"));
		assertEquals(null, doc.getFieldValue("meta_s_s_releaselabel_version2_sort"));
		
		assertEquals("beta", doc.getFieldValue("meta_s_s_releaselabel_prerelease0"));
		assertEquals("beta", doc.getFieldValue("meta_s_s_releaselabel_prerelease0_sort"));
		assertEquals("1", doc.getFieldValue("meta_s_s_releaselabel_prerelease1"));
		assertEquals("/////////1", doc.getFieldValue("meta_s_s_releaselabel_prerelease1_sort"));
		
		assertEquals(null, doc.getFieldValue("meta_s_s_releaselabel_prerelease2"));
		assertEquals(null, doc.getFieldValue("meta_s_s_releaselabel_prerelease2_sort"));
	}
	
}
