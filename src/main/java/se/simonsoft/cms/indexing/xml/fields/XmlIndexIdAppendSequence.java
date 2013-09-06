package se.simonsoft.cms.indexing.xml.fields;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.xmltracking.source.XmlSourceElement;

/**
 * The legacy type of ID.
 */
public class XmlIndexIdAppendSequence implements XmlIndexFieldExtraction {

	private String previd = null;
	private long num = Integer.MIN_VALUE;
	
	
	@Override
	public void extract(XmlSourceElement processedElemen, IndexingDoc fields) {
		String fileid = (String) fields.getFieldValue("id");
		if (fileid == null) {
			throw new IllegalArgumentException("Missing id field in indexing doc");
		}
		if (!fileid.equals(previd)) {
			num = 0;
		}
		fields.setField("id", fileid + "|" + num++);
	}

}
