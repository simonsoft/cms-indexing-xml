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
package se.simonsoft.xmltracking.index.add;

import se.simonsoft.xmltracking.source.XmlSourceElement;

/**
 * Generate unique and predictable IDs per document and element.
 * 
 * Minimal predictability is that after each {@link #start()} an element
 * gets the same id at any time from {@link #getElementId(XmlSourceElement)}.
 */
public interface IdStrategy {

	/**
	 * Signals that a new document is started.
	 * 
	 * If we don't need to reuse implementations this method can be scrapped.
	 * 
	 * At this point there should be enough information to produce a
	 * full ID prefix, with only element id within document to be appended.
	 * 
	 * Impl expected to get document identification by some other means,
	 * constructor or event handlers.
	 */
	void start();
	
	/**
	 * Solr id strategy for elements.
	 * 
	 * TODO Per document and repository deletions should be possible.
	 * 
	 * @param element From current document, in order
	 * @return Solr document id for the element
	 */
	String getElementId(XmlSourceElement element);
	
}
