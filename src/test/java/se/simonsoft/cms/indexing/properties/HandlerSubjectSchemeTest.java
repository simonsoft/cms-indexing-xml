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
package se.simonsoft.cms.indexing.properties;

import org.junit.Test;
import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.indexing.graphics.HandlerGraphicsResolution;
import se.simonsoft.cms.item.properties.CmsItemPropertiesMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HandlerSubjectSchemeTest {

	// HandlerSubjectScheme handler = new HandlerSubjectScheme();

	@Test
	public void testHandler() {
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		CmsItemPropertiesMap props = new CmsItemPropertiesMap();
		when(p.getProperties()).thenReturn(props);

		props.and("abx:TranslationLocale", "se_SV");

		// handler.handle(p);
	}
}
