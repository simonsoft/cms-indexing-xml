package se.simonsoft.cms.indexing.graphics;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;

public class HandlerGraphicsResolution implements IndexingItemHandler {

	private static final Logger logger = LoggerFactory.getLogger(HandlerGraphicsResolution.class);
	
	private static final String FIELD_PREFIX = "embd_cms-export_";
	
	private static final Set<String> PNG_FIELDS = new HashSet<String>(Arrays.asList("embd_height", "embd_width", "embd_Dimension_VerticalPixelSize", "embd_Dimension_HorizontalPixelSize"));
	
	// JPG interesting fields: "xmp_tiff.ResolutionUnit":"Inch", "xmp_tiff.XResolution":"300.0","xmp_tiff.YResolution":"300.0","xmp_tiff.ImageWidth":"3646","xmp_tiff.ImageLength":"4146"
	
	
	@Inject
	public HandlerGraphicsResolution() {}
	
	
	@Override
	public void handle(IndexingItemProgress progress) {
		
		IndexingDoc doc = progress.getFields();
		
		
		if (hasFields(doc, PNG_FIELDS)) {
			try {
				doc.addField(FIELD_PREFIX + "css_height", calculateDimensionPixelSize(getInt(doc, "embd_height"), getDouble(doc, "embd_Dimension_VerticalPixelSize")));
				doc.addField(FIELD_PREFIX + "css_width", calculateDimensionPixelSize(getInt(doc, "embd_width"), getDouble(doc, "embd_Dimension_HorizontalPixelSize")));
			} catch (Exception e) {
				// Best effort
				logger.warn("Failed to extract CSS dimensions: {}", e.getMessage(), e);
			}
			
		}
	}
	
	String calculateDimensionPixelSize(int pixels, double pixelSize) {
		// CSS pixel is normatively defined as 1/96th of 1 inch
		// https://developer.mozilla.org/en-US/docs/Glossary/CSS_pixel
		
		// Tika extracts HorizontalPixelSize / VerticalPixelSize in mm.
		
		double size = (pixels * pixelSize) / 25.4; // image size in inch.
		double css = size*96;
		
		return Long.toString(Math.round(css));
	}
	
	private static int getInt(IndexingDoc doc, String field) {
		String v = doc.getFieldValue(field).toString();
		return Integer.parseInt(v);
	}
	
	private static double getDouble(IndexingDoc doc, String field) {
		String v = doc.getFieldValue(field).toString();
		return Double.parseDouble(v);
	}

	private static boolean hasFields(IndexingDoc doc, Set<String> fields) {
		for (String f: fields) {
			if (!doc.containsKey(f)) {
				return false;
			}
		}
		return true;
	}
	
	
	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}
}
