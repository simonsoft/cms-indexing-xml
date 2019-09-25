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
package se.simonsoft.xmltracking.index;

/**
 * Attribute fields prefixed with "a_", names not escaped at all.
 * Namespace separator ':' replaced with '.' and '.' replaced with ','.
 */
public class SchemaFieldNamesReposxml implements SchemaFieldNames {

	@Override
	public String getAttribute(String xmlAttributeName) {
		// #1261 Attribute names are allowed to contain '.' which makes the NS replacement ambiguous.
		// Attribute names are not allowed to contain ',' (comma) which is also not an important character in SolR.
		xmlAttributeName = xmlAttributeName.replace('.', ',');
		
		return "a_" + xmlAttributeName.replace(':', '.');
	}

	@Override
	public String getAttributeInherited(String xmlAttributeName) {
		return "i" + getAttribute(xmlAttributeName);
	}
	
	@Override
	public String getAttributeAncestor(String xmlAttributeName) {
		return "a" + getAttribute(xmlAttributeName);
	}

	@Override
	public String getAttributeRoot(String xmlAttributeName) {
		return "r" + getAttribute(xmlAttributeName);
	}
	
	@Override
	public String getAttributeSiblingPreceding(String xmlAttributeName) {
		return "s" + getAttribute(xmlAttributeName);
	}

}
