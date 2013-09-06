package se.simonsoft.cms.indexing.xml;

import java.util.Collection;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.xmltracking.index.XmlIndexFieldExtraction;
import se.simonsoft.xmltracking.source.XmlSourceDoctype;
import se.simonsoft.xmltracking.source.XmlSourceElement;
import se.simonsoft.xmltracking.source.XmlSourceHandler;

/**
 * Instantiated once per document.
 */
class XmlSourceHandlerFieldExtractors implements XmlSourceHandler {

	private static final Logger logger = LoggerFactory.getLogger(XmlSourceHandler.class);
	
	private IndexingDoc baseDoc;
	private Set<XmlIndexFieldExtraction> fieldExtraction;
	private Collection<IndexingDoc> docHandler;

	/**
	 * @param commonFieldsDoc fieds that should be set/kept same for all elements
	 * @param fieldExtraction the extractors to run for this xml file
	 * @param docHandler Where to put the documents for each element that has been completed
	 */
	public XmlSourceHandlerFieldExtractors(IndexingDoc commonFieldsDoc, Set<XmlIndexFieldExtraction> fieldExtraction, Collection<IndexingDoc> docHandler) {
		this.baseDoc = commonFieldsDoc;
		this.fieldExtraction = fieldExtraction;
		this.docHandler = docHandler;
	}
	
	@Override
	public void startDocument(XmlSourceDoctype doctype) {
		if (doctype != null) {
			baseDoc.setField("typename", doctype.getElementName());
			baseDoc.setField("typepublic", doctype.getPublicID());
			baseDoc.setField("typesystem", doctype.getSystemID());
		}
	}

	@Override
	public void endDocument() {
	}

	@Override
	public void begin(XmlSourceElement element) {
		IndexingDoc doc = this.baseDoc.deepCopy();
		logger.debug("Source handler starts with {} common fields and extractors {}", doc.getFieldNames().size(), fieldExtraction);
		for (XmlIndexFieldExtraction ex : fieldExtraction) {
			ex.extract(element, doc);
		}
		docHandler.add(doc);
	}
	
}
