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
package se.simonsoft.xmltracking.source.jdom;

import java.util.List;

import org.jdom2.Element;

import se.simonsoft.xmltracking.source.XmlSourceAttribute;
import se.simonsoft.xmltracking.source.XmlSourceElement;
import se.simonsoft.xmltracking.source.XmlSourceNamespace;

public class XmlSourceElementJdom extends XmlSourceElement {

	private Element element;

	protected XmlSourceElementJdom(Element current,
			String name, List<XmlSourceNamespace> namespaces,
			List<XmlSourceAttribute> attributes, String elementsource) {
		super(name, namespaces, attributes, elementsource);
		this.element = current;
	}
	
	public Element getElement() {
		return this.element;
	}
	
}
