package se.simonsoft.xmltracking.index.svnadmin;

import se.simonsoft.xmltracking.index.add.IndexFieldExtraction;
import se.simonsoft.xmltracking.index.add.IndexFields;
import se.simonsoft.xmltracking.source.XmlSourceElement;

/**
 * Adds cms item id and revision info to indexed fields.
 * 
 * TODO design for notification from indexing script.
 */
public class IndexFieldExtractionCmsItem implements IndexFieldExtraction {

	@Override
	public void extract(IndexFields fields,
			XmlSourceElement processedElement) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("not implemented");
	}

}
