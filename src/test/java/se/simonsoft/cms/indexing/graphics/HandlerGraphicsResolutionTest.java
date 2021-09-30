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
