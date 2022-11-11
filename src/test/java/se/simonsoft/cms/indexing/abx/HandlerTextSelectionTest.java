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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;

public class HandlerTextSelectionTest {
	
	
	@Test
	public void shouldIndexXmlField() {
		IndexingItemProgress progress = mock(IndexingItemProgress.class);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		doc.setField("text", "tika");
		doc.setField("embd_xml_text", "xml");
		
		when(progress.getFields()).thenReturn(doc);
		
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		when(progress.getItem()).thenReturn(item);
		
		HandlerTextSelection handler = new HandlerTextSelection();
		handler.handle(progress);
		
		// Currently disabled.
		/*
		assertEquals("xml", doc.getFieldValue("text"));
		assertNull("should remove the xml field", doc.getFieldValue("embd_xml_text"));
		*/
		// #1407 Add truncated text to text_stored, prefer XML extraction.
		assertEquals("xml", doc.getFieldValue("text_stored"));
	}
	
}
