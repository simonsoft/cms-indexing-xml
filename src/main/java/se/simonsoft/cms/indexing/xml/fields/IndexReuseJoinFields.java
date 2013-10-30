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
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;

public class IndexReuseJoinFields implements XmlIndexFieldExtraction {

	@Override
	public void extract(XmlSourceElement processedElement, IndexingDoc fields) {
		String translationmaster = (String) fields.getFieldValue("prop_abx.TranslationMaster");
		if (translationmaster != null) {
			CmsItemIdArg id = new CmsItemIdArg(translationmaster);
			fields.setField("reuserelease", id.getRepository().getPath() + id.getRelPath()); // TODO not needed if we join on RID
		}
		String reusevalue = (String) fields.getFieldValue("reusevalue"); // should we parse to integer in the extraction from xsl instead?
		String locale = (String) fields.getFieldValue("prop_abx.TranslationLocale");
		if (locale != null && reusevalue != null && Integer.parseInt(reusevalue) > 0) {
			// we can't support ranges in join so all >0 reusevalues must be 1 here, multivalued is another option
			fields.setField("reusevaluelocale", "1" + locale);
		}
	}

	@Override
	public void endDocument() {
		
	}

}
