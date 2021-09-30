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
package se.simonsoft.cms.indexing.graphics;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;

public class HandlerGraphicsResolutionTest {

	HandlerGraphicsResolution handler = new HandlerGraphicsResolution();
	
	@Test
	public void testCalculateDimensionPixelSize() {
		assertEquals("406", handler.calculateDimensionPixelSize(812, 0.13229263));
	}
	
	@Test
	public void testHandlerPng() {
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		when(p.getFields()).thenReturn(doc);
		
		doc.addField("embd_Dimension_VerticalPixelSize", "0.13229263");
		doc.addField("embd_Dimension_HorizontalPixelSize", "0.13229263");
		doc.addField("embd_height","984");
		doc.addField("embd_width","812");
		
		handler.handle(p);
		
		assertEquals("406", doc.getFieldValue("embd_cms-export_css_width"));
		assertEquals("492", doc.getFieldValue("embd_cms-export_css_height"));
	}
	
	@Test
	public void testHandlerJpg() {
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		when(p.getFields()).thenReturn(doc);
		
		doc.addField("xmp_tiff.XResolution", "300.0");
		doc.addField("xmp_tiff.YResolution", "300.0");
		doc.addField("xmp_tiff.ImageLength","4146");
		doc.addField("xmp_tiff.ImageWidth","3646");
		doc.addField("xmp_tiff.ImageWidth","3646");
		
		//doc.addField("xmp_tiff.ResolutionUnit","Inch"); // Defaults to Inch.
		
		handler.handle(p);
		
		assertEquals("1167", doc.getFieldValue("embd_cms-export_css_width"));
		assertEquals("1327", doc.getFieldValue("embd_cms-export_css_height"));
	}

}
