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
package se.simonsoft.cms.indexing.xml.fields;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.indexing.xml.XmlIndexElementId;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.indexing.xml.XmlIndexProgress;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceAttribute;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;
import se.simonsoft.cms.xmlsource.handler.XmlSourceNamespace;
import se.simonsoft.xmltracking.index.SchemaFieldNames;
import se.simonsoft.xmltracking.index.SchemaFieldNamesReposxml;

public class XmlIndexFieldElement implements XmlIndexFieldExtraction {
	
	private static final Logger logger = LoggerFactory.getLogger(XmlIndexFieldElement.class);
	
	private SchemaFieldNames fieldNames = new SchemaFieldNamesReposxml();
		
	@Override
	public void startDocument(XmlIndexProgress xmlProgress) {
		
	}
	
	public void endDocument() {
		
	}
	
	@Override
	public void begin(XmlSourceElement element, XmlIndexElementId idProvider) throws XmlNotWellFormedException {
		
	}
	
	@Override
	public void end(XmlSourceElement element, XmlIndexElementId idProvider, IndexingDoc doc) {
		
		doc.addField("name", element.getName());
		for (XmlSourceNamespace n : element.getNamespaces()) {
			// The 'ns_' fields will only contain 'namespacesIntroduced', which might be unexpected. See 'ins_'.
			doc.addField("ns_" + n.getName(), n.getUri());
		}
		for (XmlSourceAttribute a : element.getAttributes()) {
			if (!isAttributeExcluded(a)) { 
				doc.addField(fieldNames.getAttribute(a.getName()), a.getValue());
			}
		}
		doc.addField("depth", element.getDepth());
		int position = element.getLocation().getOrdinal();
		doc.addField("position", position);
		addAncestorData(element, idProvider, doc);
		XmlSourceElement sp = element.getSiblingPreceding();
		if (sp != null) {
			doc.addField("id_s", idProvider.getXmlElementId(sp));
			doc.addField("sname", sp.getName());
			for (XmlSourceAttribute a : sp.getAttributes()) {
				if (!isAttributeExcluded(a)) { 
					doc.addField(fieldNames.getAttributeSiblingPreceding(a.getName()), a.getValue());
				}
			}
		} else if (position > 1) {
			logger.warn("failed to navigate to preceding sibling despite position: {}", position);
		}
		
	}
	

	/**
	 * Recursive from the actual element and up to root, aggregating field values.
	 * @param element Initial call with the element from {@link #begin(XmlSourceElement)}
	 * @param doc Field value holder
	 */
	protected void addAncestorData(XmlSourceElement element, XmlIndexElementId idProvider, IndexingDoc doc) {
		addAncestorData(element, idProvider, doc, new StringBuffer());
	}
	
	protected void addAncestorData(XmlSourceElement element, XmlIndexElementId idProvider, IndexingDoc doc, StringBuffer pos) {
		boolean isSelf = !doc.containsKey("pname");
		// bottom first
		
		// Namespaces are by definition inherited, but the XmlSourceElement API provides 'namespacesIntroduced'.
		// Treating them similar to inherited attributes renders a useful result in 'ins_'.
		for (XmlSourceNamespace n : element.getNamespaces()) {
			String f = "ins_" + n.getName();
			if (!doc.containsKey(f)) {
				doc.addField(f, n.getUri());
			}
		}
		
		// Inherited includes self
		for (XmlSourceAttribute a : element.getAttributes()) {
			if (isAttributeExcluded(a)) {
				continue;
			}
			String f = fieldNames.getAttributeInherited(a.getName());
			if (!doc.containsKey(f)) {
				doc.addField(f, a.getValue());
			}
		}
		
		// Ancestor does not include self
		if (!isSelf) {
		for (XmlSourceAttribute a : element.getAttributes()) {
			if (isAttributeExcluded(a)) {
				continue;
			}
			String f = fieldNames.getAttributeAncestor(a.getName());
			if (!doc.containsKey(f)) {
				doc.addField(f, a.getValue());
			}
		}
		}
		// handle root or recurse
		if (element.isRoot()) {
			doc.addField("id_r", idProvider.getXmlElementId(element));
			doc.addField("rname", element.getName());
			for (XmlSourceAttribute a : element.getAttributes()) {
				if (isAttributeExcluded(a)) {
					continue;
				}
				doc.addField(fieldNames.getAttributeRoot(a.getName()), a.getValue());
			}
		} else {
			XmlSourceElement parent = element.getParent();
			if (isSelf) {
				doc.addField("id_p", idProvider.getXmlElementId(parent));
				doc.addField("pname", parent.getName());
			}
			addAncestorData(parent, idProvider, doc, pos);
		}
		pos.append('.').append(element.getLocation().getOrdinal());
		if (isSelf) {
			doc.addField("pos", pos.substring(1));
		} else {
			doc.addField("aname", element.getName());
			doc.addField("id_a", idProvider.getXmlElementId(element));
		}
	}	

	private boolean isAttributeExcluded(XmlSourceAttribute a) {
		if (a.getName().startsWith("cmsreposxml:")) {
			return true;
		}
		return false;
	}
}
