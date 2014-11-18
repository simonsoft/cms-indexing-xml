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
package se.simonsoft.cms.indexing.xml.fields;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;

/**
 * Detects duplicate RIDs when indexing the document root element and propagates
 * @author takesson
 *
 */
public class XmlIndexRidDuplicateDetection implements XmlIndexFieldExtraction {
	
	private static final String REUSEVALUE_FIELD = "reusevalue";
	
	private String ridDuplicate = null;
	
	
	@Override
	public void begin(XmlSourceElement processedElement) throws XmlNotWellFormedException {
		
	}

	@Override
	public void end(XmlSourceElement processedElement, IndexingDoc fields) throws XmlNotWellFormedException {
		
		// Will be null before seeing the document root element, then empty string or a list of RIDs.
		if (this.ridDuplicate == null) {
			String ridDuplicate = (String) fields.getFieldValue("reuseridduplicate");
			if (ridDuplicate == null) {
				this.ridDuplicate = "";
			} else {
				this.ridDuplicate = ridDuplicate;
			}
		}
		
		// Disqualify element from Pretranslate if the document has duplicated RIDs.
		if (this.ridDuplicate != null && this.ridDuplicate.length() > 0) {
			if (fields.containsKey(REUSEVALUE_FIELD)) {
				fields.setField(REUSEVALUE_FIELD, -5);
			} else {
				fields.addField(REUSEVALUE_FIELD, -5);
			}
		}
	}

	@Override
	public void endDocument() {
		
		ridDuplicate = null;
	}

}
