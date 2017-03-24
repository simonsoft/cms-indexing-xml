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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.indexing.xml.XmlIndexElementId;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.indexing.xml.XmlIndexProgress;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceElementS9api;

public class XmlIndexContentReferences implements XmlIndexFieldExtraction {

	public final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	/**
	 * Add element during begin and remove at end when setting content fields.
	 */
	Stack<XmlSourceElement> parentsStack = new Stack<XmlSourceElement>();
	
	/**
	 * All descendant elements.
	 * Add each element to all lists in this map (should be same elements as in stack). 
	 */
	Map<XmlSourceElement, List<String>> contentRecursive = new HashMap<XmlSourceElement, List<String>>();
	
	/**
	 * Three levels of descendant elements.
	 * Add each element to the lists in this map for 3 most recent ancestors. 
	 */
	Map<XmlSourceElement, List<String>> content1_3 = new HashMap<XmlSourceElement, List<String>>();
	
	/**
	 * Direct children.
	 * Add each element to list of the "peek" element in stack.
	 */
	Map<XmlSourceElement, List<XmlSourceElement>> content1 = new HashMap<XmlSourceElement, List<XmlSourceElement>>();
	
	/**
	 * Grand-children, actually quite tedious to figure out unless we use a list for the stack.
	 */
	Map<XmlSourceElement, List<XmlSourceElement>> content2 = new HashMap<XmlSourceElement, List<XmlSourceElement>>();
	
	// Can do any number of child levels by looking at stack position.
	
	/**
	 * Clear all data structures.
	 */
	private void init() {
		
		parentsStack = new Stack<XmlSourceElement>();
		contentRecursive = new HashMap<XmlSourceElement, List<String>>();
		content1_3 = new HashMap<XmlSourceElement, List<String>>();
	}
	
	
	@Override
	public void begin(XmlSourceElement processedElement, XmlIndexElementId idProvider) throws XmlNotWellFormedException {
		
		if (!(processedElement instanceof XmlSourceElementS9api)) {
			// Well, we are technically not constrained to S9API, but JDOM would require a massive Heap.
			throw new IllegalArgumentException("This field extraction handler can not participate unless XML Source impl is S9API");
		}
		
		parentsStack.push(processedElement);
		contentRecursive.put(processedElement, new LinkedList<String>());
		content1_3.put(processedElement, new LinkedList<String>());
	}
	
	@Override
	public void end(XmlSourceElement processedElement, XmlIndexElementId idProvider, IndexingDoc fields) throws XmlNotWellFormedException {
		
		XmlSourceElement popped = parentsStack.pop();
		if (!popped.equals(processedElement)) {
			throw new IllegalStateException("end of element which is not at the top of the stack: " + processedElement);
		}
		// Remove the list for this element
		@SuppressWarnings("unused")
		List<String> listR = contentRecursive.remove(popped);
		List<String> list1_3 = content1_3.remove(popped);
		//logger.info("{} children: {}", processedElement.getName(), listR.size());
		//logger.info("{} children_1-3: {}", processedElement.getName(), list1_3.size());
		// Add to a single-value field (with keyword analysis)
		String joined1_3 = join(list1_3, " ");
		fields.addField("content_c_1_3", joined1_3);
		/* For multi-value field.
		for (String s: list1_3) {
			fields.addField("patharea", s);
		}
		*/
		
		// Add this element to list of all ancestor elements
		String sha1 = (String) fields.getFieldValue("c_sha1_source_reuse");
		if (sha1 == null || sha1.isEmpty()) {
			throw new IllegalStateException("element lacks \"c_sha1_source_reuse\"");
		}
		/*
		for (XmlSourceElement e: contentRecursive.keySet()) {
			List<String> list = contentRecursive.get(e);
			list.add(sha1);
		}
		*/
		// Add this element to the 3 closest ancestors' child-lists.
		List<XmlSourceElement> parent1_3 = parentsStack.subList(Math.max(0, parentsStack.size()-3), parentsStack.size());
		for (XmlSourceElement e: parent1_3) {
			List<String> list = content1_3.get(e);
			list.add(sha1);
		}
		
	}
	
	@Override
	public void startDocument(XmlIndexProgress xmlProgress) {
		
	}

	@Override
	public void endDocument() {
		
		init();
	}
	
	public static String join(Collection<?> list, String sep) {
		
		StringBuilder sb = new StringBuilder();
		Iterator<?> it = list.iterator();
		if (it.hasNext()) {
			sb.append(it.next().toString());
		}
		while (it.hasNext()) {
			sb.append(sep);
			sb.append(it.next().toString());
		}
		return sb.toString();
	}

}
