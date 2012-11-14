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
package se.simonsoft.cms.indexing.xml;

/**
 * Uniquely identifies an element's position in an XML DOM.
 * 
 * TODO Verify analogous to ACL:treeloc
 * TODO Use as helper when calculating pos in indexing (to encapsulate rules for text nodes etc.)
 */
public class TreePos {

	private String pos;

	public TreePos(String dotSeparated1based) {
		this.pos = dotSeparated1based;
	}
	
	@Override
	public String toString() {
		return pos;
	}
	
	/**
	 * Returns the last number in tree position.
	 * @return position among siblings of the deepset level in this instance, even if it is not a leaf in the actual XML structure
	 */
	public int getChildNumber() {
		throw new UnsupportedOperationException("To be implemented together with XmlSourceElement#getPosition()");
	}
	
}
