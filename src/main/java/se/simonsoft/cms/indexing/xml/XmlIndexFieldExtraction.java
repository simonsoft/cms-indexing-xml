/**
 * Copyright (C) 2009-2013 Simonsoft Nordic AB
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

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;

/**
 * Services that extract fields from source element or from earlier extracted fields.
 * 
 * A list of these services is for example provided to {@link se.simonsoft.xmltracking.index.addXmlSourceHandlerSolrj}.
 */
public interface XmlIndexFieldExtraction {

	
	/**
	 * Notification that the element will be extracted when reaching the end of the element.
	 * 
	 * @param processedElement
	 * @param idProvider of IDs identifying elements in index
	 * @throws XmlNotWellFormedException
	 */
	void begin(XmlSourceElement processedElement, XmlIndexElementId idProvider) throws XmlNotWellFormedException;
	
	/**
	 * Extracts element fields for per-element indexing in xml core.
	 * 
	 * Indexing should be aborted for all errors except the declared. Index must never be inconsistent.
	 * 
	 * @param processedElement original element
	 * @param idProvider of IDs identifying elements in index
	 * @param fields collection of fields for the index
	 */
	void end(XmlSourceElement processedElement, XmlIndexElementId idProvider, IndexingDoc fields) throws XmlNotWellFormedException;
	
	
	/**
	 * Enable handlers to prepare for the document.
	 * @param base indexing doc that will be cloned for each element (read-only)
	 */
	public void startDocument(XmlIndexProgress xmlProgress);
	
	/**
	 * Enable handlers to clean up after document.
	 */
	public void endDocument();
}
