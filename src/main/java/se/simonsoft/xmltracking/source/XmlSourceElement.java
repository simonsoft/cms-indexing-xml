package se.simonsoft.xmltracking.source;

import java.util.List;

public class XmlSourceElement {

	private String name;
	
	private List<XmlSourceAttribute> attributes;
	
	private String source;

	private XmlSourceElement parent = null;

	private int depth = 0;

	private XmlSourceElement siblingPreceding = null;	
	
	private int position = 0;

	public XmlSourceElement(String name,
			List<XmlSourceAttribute> attributes,
			String source) {
		this.name = name;
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
	 * @return all attributes on the element
	 */
	public List<XmlSourceAttribute> getAttributes() {
		return attributes;
	}
	
	/**
	 * @return Exact source of the element and its contents,
	 *  only parsed with character set, not unescaped,
	 *  including newlines and PIs
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @return level in tree, 1 is root
	 */
	public int getDepth() {
		return depth;
	}

	/**
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
	
}
