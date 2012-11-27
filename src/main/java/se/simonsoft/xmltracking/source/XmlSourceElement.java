/**
 * Copyright (C) 2009-2012 Simonsoft Nordic AB
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
package se.simonsoft.xmltracking.source;

import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.simonsoft.cms.indexing.xml.TreePos;

/**
 * Raw information about an element.
 * Callers can aggregate structural information using {@link #getParent()} and {@link #getSiblingPreceding()}.
 * 
 * @todo Instances currently buffers the full source, could probably share stream with other elements.
 */
public class XmlSourceElement {

	private String name;
	
	private List<XmlSourceNamespace> namespaces;
	
	private List<XmlSourceAttribute> attributes;
	
	private String source;

	private XmlSourceElement parent = null;

	private int depth = 0;

	private XmlSourceElement siblingPreceding = null;	
	
	private int position = 0;

	public XmlSourceElement(String name,
			List<XmlSourceAttribute> attributes,
			String source) {
		this(name, new java.util.LinkedList<XmlSourceNamespace>(), attributes, source);
	}
	
	public XmlSourceElement(String name,
			List<XmlSourceNamespace> namespaces,			
			List<XmlSourceAttribute> attributes,
			String source) {
		this.name = name;
		this.namespaces = namespaces;
		this.attributes = attributes;
		this.source = source;
	}
	
	public XmlSourceElement setDepth(int depth, XmlSourceElement parent) {
		if (depth < 1) {
			throw new IllegalArgumentException("Invalid depth " + depth + ". Root is 1.");
		}
		if (depth == 1 && parent != null) {
			throw new IllegalArgumentException("Depth was set to root but with a parent " + parent);
		}
		this.parent = parent;
		this.depth = depth;
		return this;
	}
	
	public XmlSourceElement setPosition(int position, XmlSourceElement siblingPreceding) {
		this.siblingPreceding = siblingPreceding;
		this.position = position;
		return this;
	}
	
	public boolean isRoot() {
		if (this.depth == 0) {
			throw new IllegalStateException("Depth has not been set, isRoot not known");
		}
		return this.depth == 1;
	}
	
	/**
	 * @return parent element, null if this is root
	 */
	public XmlSourceElement getParent() {
		return parent;
	}
	
	/**
	 * @return Element name including namespace
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @return The namespaces declared on this element, not the inherited declarations.
	 */
	public List<XmlSourceNamespace> getNamespaces() {
		return Collections.unmodifiableList(namespaces);
	}
	
	/**
	 * @return all attributes on the element
	 */
	public List<XmlSourceAttribute> getAttributes() {
		return Collections.unmodifiableList(attributes);
	}
	
	/**
	 * @return unordered name-value map for attribute lookup, new every time, namespaces included in names
	 */
	public Map<String, String> getAttributeMap() {
		Map<String, String> map = new HashMap<String, String>();
		for (XmlSourceAttribute a : attributes) {
			map.put(a.getName(), a.getValue());
		}
		return map;
	}
	
	/**
	 * @return Exact source of the element and its contents,
	 *  only parsed with character set, not unescaped,
	 *  including newlines and PIs
	 */
	public Reader getSource() {
		return new StringReader(source); // TODO see class javadocs
	}

	/**
	 * @return level in tree, 1 is root
	 */
	public int getDepth() {
		return depth;
	}

	/**
	 * TODO convert too {@link TreePos}
	 * @return position among siblings, 1 is first
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * @return preceding sibling if there is one with the same parent, null if not
	 */
	public XmlSourceElement getSiblingPreceding() {
		return siblingPreceding;
	}

	@Override
	public String toString() {
		return name + attributes + source.length();
	}
	
	public TreePos getPos() {
		throw new UnsupportedOperationException("not implemented");
	}
	
	/**
	 * Uses {@link #setDepth(int, XmlSourceElement)} information to check ancestry.
	 * For non-instance based checks use analogous methods from {@link #getPos()}.
	 * @param other possibly a child
	 * @return true if the instance is among the parents of the argument instance
	 */
	public boolean isAncestorOf(XmlSourceElement other) {
		return other.isDescendantOf(this);
	}
	
	/**
	 * Uses {@link #setDepth(int, XmlSourceElement)} information to traverse children.
	 * For non-instance based checks use analogous methods from {@link #getPos()}.
	 * @param other possibly a child
	 * @return true if the instance is among the parents of the argument instance
	 */
	public boolean isDescendantOf(XmlSourceElement other) {
		XmlSourceElement p = this;
		while ((p = p.getParent()) != null) {
			if (p == other) return true;
		}
		return false;
	}
	
}
