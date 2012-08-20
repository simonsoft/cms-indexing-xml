package se.simonsoft.xmltracking.source;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class XmlSourceElementTest {

	@Test
	public void testIsRoot() {
		assertTrue(new XmlSourceElement("n", new LinkedList<XmlSourceAttribute>(), "<e/>")
			.setDepth(1, null).isRoot());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testSetLevelInvalid() {
		new XmlSourceElement("n", new LinkedList<XmlSourceAttribute>(), "<e/>")
			.setDepth(0, null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testSetLevelRootButWithParent() {
		List<XmlSourceAttribute> list = new LinkedList<XmlSourceAttribute>();
		new XmlSourceElement("n", list, "<e/>")
			.setDepth(1, new XmlSourceElement("p", list, "<p/>"));
	}
	
	@Test(expected=IllegalStateException.class)
	public void testIsRootNoLevel() {
		new XmlSourceElement("n", new LinkedList<XmlSourceAttribute>(), "<e/>").isRoot();
	}
	
}
