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

import java.util.HashMap;

import se.simonsoft.cms.indexing.xml.XmlIndexElementId;
import se.simonsoft.cms.xmlsource.TreeLocation;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;

public class XmlIndexIdAppendDepthFirstPosition implements XmlIndexElementId {

	private String baseId;
	private HashMap<TreeLocation, Long> positions = new HashMap<>();

	public XmlIndexIdAppendDepthFirstPosition(String baseId) {
		
		this.baseId = baseId;
	}
	
	@Override
	public String getXmlElementId(XmlSourceElement processedElement) {
		
		Long elementPos = positions.get(processedElement.getLocation());
		if (elementPos == null) {
			throw new IllegalStateException("XmlIndexElementId requested for unknown element: " + processedElement);
		}
		
		return new String(baseId + "|" + getElementId(elementPos));
	}

	public void setXmlElementDepthFirstPosition(XmlSourceElement processedElement, long elementCount) {
		positions.put(processedElement.getLocation(), elementCount);
	}
	
	
	public static String getElementId(long l) {
		return String.format("%08d", l);
	}
	

}
