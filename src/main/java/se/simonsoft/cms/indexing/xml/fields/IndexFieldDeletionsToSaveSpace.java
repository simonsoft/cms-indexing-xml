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

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.indexing.xml.XmlIndexElementId;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.indexing.xml.XmlIndexProgress;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;

public class IndexFieldDeletionsToSaveSpace implements XmlIndexFieldExtraction {

	@Override
	public void begin(XmlSourceElement processedElement, XmlIndexElementId idProvider) throws XmlNotWellFormedException {
		
	}
	
	@Override
	public void end(XmlSourceElement processedElement, XmlIndexElementId idProvider, IndexingDoc fields) {
		fields.removeField("prop_abx.Dependencies");
	}

	@Override
	public void endDocument() {
		
	}

	@Override
	public void startDocument(XmlIndexProgress xmlProgress) {
		
	}

}
