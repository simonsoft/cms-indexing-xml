/**
 * Copyright (C) 2009-2016 Simonsoft Nordic AB
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;

public class HandlerTitleSelectionTest {
	
	
	@Test
	public void shouldIndexKeyValueWithHighestPrio() {
		IndexingItemProgress progress = mock(IndexingItemProgress.class);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		doc.setField("xmp_dc.title", "xmp dc title");
		doc.setField("embd_title", "embd title");
		doc.setField("xmp_dc.subject", "xmp dc subject");
		
		when(progress.getFields()).thenReturn(doc);
		
		HandlerTitleSelection handler = new HandlerTitleSelection();
		handler.handle(progress);
		
		assertEquals("embd title", doc.getFieldValue("title"));
		
	}
	
}
