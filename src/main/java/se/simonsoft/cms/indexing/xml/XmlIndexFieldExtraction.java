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
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;

/**
 * Services that extract fields from source element or from earlier extracted fields.
 * 
 * A list of these services is for example provided to {@link se.simonsoft.xmltracking.index.addXmlSourceHandlerSolrj}.
 */
public interface XmlIndexFieldExtraction {

	/**
	 * @param fields to read from and add/overwrite to
	 * @param processedElement original element
	 */
	void extract(XmlSourceElement processedElement, IndexingDoc fields);
	
}
