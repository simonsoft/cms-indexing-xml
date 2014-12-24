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
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.indexing.xml.XmlIndexElementId;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;

public class XmlIndexFieldExtractionSource implements XmlIndexFieldExtraction {

	private static final Logger logger = LoggerFactory.getLogger(XmlIndexFieldExtractionSource.class);
	
	/**
	 * This is a hack to remove Abx Change Tracking namespace from source.
	 * Finalize Release aborts if there is CT in the document, so there should be none in Translations.
	 */
	private static final boolean REMOVE_ABXCT_NAMESPACE = true;
	
	private Integer MAX_CHARACTERS_SOURCE = 0; // null means 'always extract source'
	
	@Override
	public void startDocument(IndexingDoc baseDoc) {
		
	}
	
	public void endDocument() {

	}
	
	@Override
	public void begin(XmlSourceElement element, XmlIndexElementId idProvider) throws XmlNotWellFormedException {
		
	}
	
	@Override
	public void end(XmlSourceElement element, XmlIndexElementId idProvider, IndexingDoc doc) {
		
		Integer depth = (Integer) doc.getFieldValue("depth");
		if (depth == null || depth < 1) {
			throw new IllegalStateException("The 'depth' must be extracted before 'source'");
		}
		
		String sourceReuse = (String) doc.getFieldValue("source_reuse");
		if (sourceReuse == null || sourceReuse.isEmpty()) {
			// No source_reuse, then we suppress source as well.
			return;
		}
		// If source_reuse is large, we avoid getting source completely.
		if (MAX_CHARACTERS_SOURCE != null && sourceReuse.length() > MAX_CHARACTERS_SOURCE) {
			logger.debug("Suppressing 'source' and 'source_reuse' field ({}) from index for element: {}", sourceReuse.length(), element);
			//doc.removeField("source_reuse");
			// Suppress source by returning early.
			return;
		}
		
		
		
		String source = getSource(element);
		
		// No longer extracting source for the whole file unconditionally.
		if (MAX_CHARACTERS_SOURCE != null && source.length() > MAX_CHARACTERS_SOURCE) {
			logger.debug("Suppressing 'source' field ({}) from index for element: {}", source.length(), element);
			return;
		}

		if (REMOVE_ABXCT_NAMESPACE) {
			// Remove the Arbortext CT namespace in translations.
			Collection<Object> patharea = doc.getFieldValues("patharea");
			if (patharea != null && patharea.contains("translation")) {
				logger.debug("Patharea translation: {}", patharea.contains("translation"));
				source = source.replaceAll(" xmlns:atict=\"http://www.arbortext.com/namespace/atict\"", "");
			}
		}
		
		String nsUnused = (String) doc.getFieldValue("ns_unused");
		if (nsUnused != null && !nsUnused.isEmpty()) {
			logger.debug("Unused Namespaces: {}", nsUnused);
			//throw new RuntimeException(nsUnused);
		}
		
		doc.addField("source", source);
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

}
