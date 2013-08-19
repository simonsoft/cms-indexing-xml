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
package se.simonsoft.cms.indexing.xml;

/**
 * Uniquely identifies an element's position in an XML DOM.
 * 
 * TODO Verify analogous to ACL:treeloc
 * TODO Use as helper when calculating pos in indexing (to encapsulate rules for text nodes etc.)
 */
public class TreeLocation {

	// doesn't work with withParent //private static final Pattern VALIDATION = Pattern.compile("^1(\\.\\d+)*$"); 
	
	private String pos;
	private int ordinal;

	public TreeLocation(String dotSeparated1based) {
		this(dotSeparated1based, getOrdinal(dotSeparated1based));
	}

	private TreeLocation(String dotSeparated1based, int ordinal) {
		this.pos = dotSeparated1based;
		this.ordinal = ordinal;
	}
	
	/**
	 * Used to build a pos in element traversal.
	 * @param ordinal 1 for root, position among sibilings if {@link #withParent(int)} is used to build a complete pos 
	 */
	public TreeLocation(int ordinal) {
		this.pos = Integer.toString(ordinal);
		this.ordinal = ordinal;
	}

	private static int getOrdinal(String dotSeparated1based) {
		int d = dotSeparated1based.lastIndexOf('.');
		if (d < 0) {
			return Integer.parseInt(dotSeparated1based);
		}
		return Integer.parseInt(dotSeparated1based.substring(d + 1));
	}
	
	@Override
	public String toString() {
		return pos;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj != null 
				&& obj instanceof TreeLocation // remove this?
				&& toString().equals(obj.toString());
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	public TreeLocation withChild(int childOrdinal) {
		return new TreeLocation(this.toString() + '.' + childOrdinal, childOrdinal);
	}
	
	public TreeLocation withParent(int parentOrdinal) {
		return new TreeLocation(parentOrdinal +  ('.' + this.toString()), this.ordinal);
	}
	
	/**
	 * Returns the last number in tree position.
	 * @return position among siblings of the deepset level in this instance, even if it is not a leaf in the actual XML structure
	 */
	public int getOrdinal() {
		return ordinal;
	}

	public boolean isAncestorOf(TreeLocation treeLocation) {
		return treeLocation.toString().startsWith(this.toString() + ".");
	}

	public boolean isDescendantOf(TreeLocation treeLocation) {
		return treeLocation.isAncestorOf(this);
	}

	public boolean isParentOf(TreeLocation treeLocation) {
		return isAncestorOf(treeLocation) && treeLocation.toString().substring(this.toString().length() + 1).indexOf(".") == -1;
	}

	public boolean isChildOf(TreeLocation treeLocation) {
		return treeLocation.isParentOf(this);
	}
	
}
