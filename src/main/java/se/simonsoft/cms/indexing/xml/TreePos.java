package se.simonsoft.cms.indexing.xml;

/**
 * Uniquely identifies an element's position in an XML DOM.
 * 
 * TODO Verify analogous to ACL:Treepos
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
	
}
