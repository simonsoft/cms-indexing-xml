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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;

public class XmlIndexContentReferences implements XmlIndexFieldExtraction {

	
	/**
	 * Add element during begin and remove at end when setting content fields.
	 */
	Stack<XmlSourceElement> parentsStack = new Stack<XmlSourceElement>();
	
	/**
	 * All descendant elements.
	 * Add each element to all lists in this map (should be same elements as in stack). 
	 */
	Map<XmlSourceElement, List<XmlSourceElement>> contentRecursive = new HashMap<XmlSourceElement, List<XmlSourceElement>>();
	
	/**
	 * Direct children.
	 * Add each element that to list of the "peek" element in stack.
	 */
	Map<XmlSourceElement, List<XmlSourceElement>> content1 = new HashMap<XmlSourceElement, List<XmlSourceElement>>();
	
	/**
	 * Grand-children, actually quite tedious to figure out unless we use a list for the stack.
	 */
	Map<XmlSourceElement, List<XmlSourceElement>> content2 = new HashMap<XmlSourceElement, List<XmlSourceElement>>();
	
	// Can do any number of child levels by looking at stack position.
	

	@Override
	public void extract(XmlSourceElement processedElement, IndexingDoc fields) throws XmlNotWellFormedException {
		// TODO Auto-generated method stub

	}

	@Override
	public void endDocument() {
		// TODO Auto-generated method stub

	}

}
