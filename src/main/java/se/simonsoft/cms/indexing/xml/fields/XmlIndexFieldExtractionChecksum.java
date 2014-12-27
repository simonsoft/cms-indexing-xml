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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.indexing.xml.XmlIndexElementId;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.indexing.xml.XmlIndexProgress;
import se.simonsoft.cms.item.Checksum;
import se.simonsoft.cms.item.impl.ChecksumRead;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;

/**
 * Creates checksums of selected fields and adds with checksum type prefixed field names.
 */
public class XmlIndexFieldExtractionChecksum implements XmlIndexFieldExtraction {

	private List<String> prefixes;

	public XmlIndexFieldExtractionChecksum() {
		this("text", "source");
	}
	
	public XmlIndexFieldExtractionChecksum(String... fieldprefixes) {
		this.prefixes = Arrays.asList(fieldprefixes);
	}

	protected ChecksumRead getChecksummer() {
		return new ChecksumRead();
	}
	
	@Override
	public void begin(XmlSourceElement processedElement, XmlIndexElementId idProvider) throws XmlNotWellFormedException {
		
	}
	
	@Override
	public void end(XmlSourceElement processedElement, XmlIndexElementId idProvider, IndexingDoc fields) {
		Collection<String> orgFieldNames = new LinkedList<String>(fields.getFieldNames());
		for (String n : orgFieldNames) {
			for (String p : prefixes) {
				if (n.startsWith(p)) {
					Object v = fields.getFieldValue(n);
					if (v instanceof String) {
						Checksum c = getChecksum((String) v);
						fields.addField("c_md5_" + n, c.getMd5());
						fields.addField("c_sha1_" + n, c.getSha1());
					} else {
						throw new IllegalArgumentException("Only string fields can be checksummed, field " + n + " was " + v.getClass() + " " + v);
					}
				}
			}
		}
	}

	private Checksum getChecksum(String value) {
		ChecksumRead c = new ChecksumRead();
		try {
			c.add(new ByteArrayInputStream(value.getBytes()));
		} catch (IOException e) { // would be very odd when we use byte array
			throw new RuntimeException("Value read error");
		}
		return c;
	}

	@Override
	public void startDocument(XmlIndexProgress xmlProgress) {
		
	}
	@Override
	public void endDocument() {
		// TODO Should prefixes be cleaned? Very little data anyway.
		
	}

}
