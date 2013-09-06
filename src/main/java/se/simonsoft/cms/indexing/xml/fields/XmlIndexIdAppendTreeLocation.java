package se.simonsoft.cms.indexing.xml.fields;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.xmltracking.source.XmlSourceElement;

public class XmlIndexIdAppendTreeLocation implements XmlIndexFieldExtraction {

	@Override
	public void extract(XmlSourceElement processedElement, IndexingDoc fields) {
		String fileid = (String) fields.getFieldValue("id");
		if (fileid == null) {
			throw new IllegalArgumentException("Missing id field in indexing doc");
		}
		fields.setField("id", fileid + "|" + processedElement.getLocation());
	}

}
