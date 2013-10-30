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

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.xmltracking.index.SchemaFieldNames;
import se.simonsoft.xmltracking.index.SchemaFieldNamesReposxml;
import se.simonsoft.cms.xmlsource.handler.XmlSourceAttribute;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;
import se.simonsoft.cms.xmlsource.handler.XmlSourceNamespace;

public class XmlIndexFieldElement implements XmlIndexFieldExtraction {

	private static final int hashmapInitialCapacity = 10000;
	
	private SchemaFieldNames fieldNames = new SchemaFieldNamesReposxml();
	
	private Map<XmlSourceElement, String> assigned = new HashMap<XmlSourceElement, String>(hashmapInitialCapacity); // not very efficient but we've done like this since the start of xml indexing, also impossible to use for next sibling
	
	public void endDocument() {
		
		assigned = new HashMap<XmlSourceElement, String>(hashmapInitialCapacity);
	}
	
	@Override
	public void extract(XmlSourceElement element, IndexingDoc doc) {
		String id = (String) doc.getFieldValue("id");
		if (id == null) {
			throw new IllegalStateException("The id field must be set before adding other fields");
		}
		keepElementId(element, id);
		doc.addField("name", element.getName());
		doc.addField("source", getSource(element));
		for (XmlSourceNamespace n : element.getNamespaces()) {
			doc.addField("ns_" + n.getName(), n.getUri());
		}
		for (XmlSourceAttribute a : element.getAttributes()) {
			doc.addField(fieldNames.getAttribute(a.getName()), a.getValue());
		}
		doc.addField("depth", element.getDepth());
		doc.addField("position", element.getLocation().getOrdinal());
		addAncestorData(element, doc);
		XmlSourceElement sp = element.getSiblingPreceding();
		if (sp != null) {
			doc.addField("id_s", getElementId(sp));
			doc.addField("sname", sp.getName());
			for (XmlSourceAttribute a : sp.getAttributes()) {
				doc.addField(fieldNames.getAttributeSiblingPreceding(a.getName()), a.getValue());
			}
		}
	}
	
	private void keepElementId(XmlSourceElement e, String id) {
		assigned.put(e, id);
	}
	
	private String getElementId(XmlSourceElement sp) {
		return assigned.get(sp);
	}

	/**
	 * Source is currently stored in index but could be very large xml chunks.
	 * @param element
	 * @return
	 */
	private String getSource(XmlSourceElement element) {
		Reader s = element.getSource();
		StringBuffer b = new StringBuffer();
		int c;
		try {
			while ((c = s.read()) > -1) {
				b.append((char) c);
			}
		} catch (IOException e) {
			throw new RuntimeException("Error reading XML source for indexing", e);
		}
		return b.toString();
	}

	/**
	 * Recursive from the actual element and up to root, aggregating field values.
	 * @param element Initial call with the element from {@link #begin(XmlSourceElement)}
	 * @param doc Field value holder
	 */
	protected void addAncestorData(XmlSourceElement element, IndexingDoc doc) {
		addAncestorData(element, doc, new StringBuffer());
	}
	
	protected void addAncestorData(XmlSourceElement element, IndexingDoc doc, StringBuffer pos) {
		boolean isSelf = !doc.containsKey("pname");
		// bottom first
		for (XmlSourceNamespace n : element.getNamespaces()) {
			String f = "ins_" + n.getName();
			if (!doc.containsKey(f)) {
				doc.addField(f, n.getUri());
			}
		}
		for (XmlSourceAttribute a : element.getAttributes()) {
			String f = fieldNames.getAttributeInherited(a.getName());
			if (!doc.containsKey(f)) {
				doc.addField(f, a.getValue());
			}
		}
		// handle root or recurse
		if (element.isRoot()) {
			doc.addField("id_r", getElementId(element));
			doc.addField("rname", element.getName());
			for (XmlSourceAttribute a : element.getAttributes()) {
				doc.addField(fieldNames.getAttributeRoot(a.getName()), a.getValue());
			}
		} else {
			XmlSourceElement parent = element.getParent();
			if (isSelf) {
				doc.addField("id_p", getElementId(parent));
				doc.addField("pname", parent.getName());
			}
			addAncestorData(parent, doc, pos);
		}
		pos.append('.').append(element.getLocation().getOrdinal());
		if (isSelf) {
			doc.addField("pos", pos.substring(1));
		} else {
			doc.addField("aname", element.getName());
			doc.addField("id_a", getElementId(element));
		}
	}	

}
