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
package se.simonsoft.cms.indexing.xml;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexIdAppendDepthFirstPosition;
import se.simonsoft.cms.xmlsource.handler.XmlSourceDoctype;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;
import se.simonsoft.cms.xmlsource.handler.XmlSourceHandler;

/**
 * Instantiated once per document.
 */
class XmlSourceHandlerFieldExtractors implements XmlSourceHandler {

	private static final Logger logger = LoggerFactory.getLogger(XmlSourceHandlerFieldExtractors.class);
	
	private XmlIndexProgress xmlProgress;
	private IndexingDoc baseDoc;
	private String baseId;
	private Set<XmlIndexFieldExtraction> fieldExtraction;
	private XmlIndexAddSession docHandler;
	
	private XmlIndexIdAppendDepthFirstPosition idAppender;
	
	private long elementCount;
	private Integer maxDepth = null;
	
	/**
	 * @param commonFieldsDoc fieds that should be set/kept same for all elements
	 * @param fieldExtraction the extractors to run for this xml file
	 * @param docHandler Where to put the documents for each element that has been completed
	 */
	public XmlSourceHandlerFieldExtractors(XmlIndexProgress xmlProgress, Set<XmlIndexFieldExtraction> fieldExtraction, XmlIndexAddSession docHandler) {
		this.xmlProgress = xmlProgress;
		this.baseDoc = xmlProgress.getBaseDoc();
		this.baseId = (String) this.baseDoc.getFieldValue("id");
		if (baseId == null) {
			throw new IllegalArgumentException("Missing id field in indexing doc");
		}
		this.idAppender = new XmlIndexIdAppendDepthFirstPosition(baseId);
		this.fieldExtraction = fieldExtraction;
		this.docHandler = docHandler;

		this.elementCount = 0; // The first element is 1.
		this.maxDepth = getDepthReposxml(this.baseDoc);
	}
	
	public static Integer getDepthReposxml(IndexingDoc itemDoc) {
		// The reposxml indexing depth controlled by repositem XSL.
		// Some Unit tests don't execute the repositem extraction.
		String reposxmlDepth = (String) itemDoc.getFieldValue("count_reposxml_depth");
		if (reposxmlDepth != null) {
			return Integer.parseInt(reposxmlDepth);
		} else {
			return null;
		}
	}
	
	@Override
	public void startDocument(XmlSourceDoctype doctype) {
		logger.debug("Source handler starts with {} fields, extractors {}", this.baseDoc.getFieldNames().size(), fieldExtraction);
		
		for (XmlIndexFieldExtraction ex : fieldExtraction) {
			ex.startDocument(this.xmlProgress);
		}
	}

	@Override
	public void endDocument() {
		
		for (XmlIndexFieldExtraction ex : fieldExtraction) {
			ex.endDocument();
		}
		docHandler.end();
	}

	@Override
	public void begin(XmlSourceElement element) {
		
		if (this.maxDepth != null && element.getDepth() > this.maxDepth) {
			return;
		}
		
		this.elementCount++;
		this.idAppender.setXmlElementDepthFirstPosition(element, elementCount);

		//IndexingDoc doc = this.baseDoc.deepCopy();
		for (XmlIndexFieldExtraction ex : fieldExtraction) {
			ex.begin(element, idAppender);
			//ex.end(element, doc);
		}
		//docHandler.add(doc);
	}

	@Override
	public void end(XmlSourceElement element) {
		
		if (this.maxDepth != null && element.getDepth() > this.maxDepth) {
			return;
		}
		
		
		String id = idAppender.getXmlElementId(element);
		
		IndexingDoc doc = this.baseDoc.deepCopy();
		doc.setField("id", id);
		
		for (XmlIndexFieldExtraction ex : fieldExtraction) {
			ex.end(element, idAppender, doc);
		}
		docHandler.add(doc);
	}
	
}
