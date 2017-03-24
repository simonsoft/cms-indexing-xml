/**
 * Copyright (C) 2009-2016 Simonsoft Nordic AB
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

import java.util.Collection;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.indexing.xml.XmlIndexElementId;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.indexing.xml.XmlIndexProgress;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;

/**
 * Detects duplicate RIDs when indexing the document (repositem) and propagates to reposxml elements.
 * @author takesson
 *
 */
public class XmlIndexRidDuplicateDetection implements XmlIndexFieldExtraction {
	
	private static final String REUSEVALUE_FIELD = "reusevalue";
	
	private static final String REPOSITEM_FLAG_FIELD = "flag";
	private static final String REPOSITEM_FLAG_RIDDUPLICATE = "hasridduplicate";
	
	
	@Override
	public void begin(XmlSourceElement processedElement, XmlIndexElementId idProvider) throws XmlNotWellFormedException {
		
	}

	@Override
	public void end(XmlSourceElement processedElement, XmlIndexElementId idProvider, IndexingDoc fields) throws XmlNotWellFormedException {
		
		Collection<Object> flags = fields.getFieldValues(REPOSITEM_FLAG_FIELD); 
		/*
		if (flags == null || flags.size() == 0) {
			// There must be at least the hasxml flag, otherwise the flag field has been lost.
			throw new RuntimeException("the 'flag' field is empty, must have 'hasxml' flag: " + processedElement);
		}
		*/
		
		Boolean flagRidDup = false;
		if (flags != null) {
			flagRidDup = flags.contains(REPOSITEM_FLAG_RIDDUPLICATE); 
			
			// TODO: Need to remove the flag field until we introduce it in reposxml.
			fields.removeField(REPOSITEM_FLAG_FIELD);
		}
		
		
		// Disqualify element from Pretranslate if the document has duplicated RIDs.
		if (flagRidDup) {
			
			fields.setField(REUSEVALUE_FIELD, -5);	
		}
	}

	@Override
	public void startDocument(XmlIndexProgress xmlProgress) {
		
	}
	
	@Override
	public void endDocument() {
		
	}

}
