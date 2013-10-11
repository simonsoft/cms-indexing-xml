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
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;

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
