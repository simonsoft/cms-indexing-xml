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
package se.simonsoft.cms.indexing.xml.source;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import se.simonsoft.xmltracking.source.XmlSourceAttribute;
import se.simonsoft.xmltracking.source.XmlSourceElement;

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

	@Test
	public void testGetPosOnlyElements() {
		XmlSourceElement a = new XmlSourceElement("a", new LinkedList<XmlSourceAttribute>(), "<a><b><c/><d/></b><b2/></a>");
		XmlSourceElement b = new XmlSourceElement("b", new LinkedList<XmlSourceAttribute>(), "<b><c/></b>");
		XmlSourceElement c = new XmlSourceElement("c", new LinkedList<XmlSourceAttribute>(), "<c/>");
		XmlSourceElement b2 = new XmlSourceElement("b2", new LinkedList<XmlSourceAttribute>(), "<b2/>");
		a.setDepth(1, null);
		a.setPosition(1, null);
		b.setDepth(2, a);
		b.setPosition(1, null);
		c.setDepth(3, b);
		c.setPosition(1, null);
		b2.setDepth(2, a);
		b2.setPosition(2, b);
		assertEquals("1", a.getLocation().toString());
		assertEquals("1.1", b.getLocation().toString());
		assertEquals("1.2", b2.getLocation().toString());
		assertEquals("1.1.1", c.getLocation().toString());
	}
	
	@Test
	@Ignore // need a full xml doc with text, comments, pi-s etc
	public void testGetPosCompatibilityWithTreeLoc() {
	}
	
	@Test
	public void testIsAncestorOf() {
		XmlSourceElement a = new XmlSourceElement("a", new LinkedList<XmlSourceAttribute>(), "<a><b><c/><d/></b><b2/></a>");
		XmlSourceElement b = new XmlSourceElement("b", new LinkedList<XmlSourceAttribute>(), "<b><c/></b>");
		XmlSourceElement c = new XmlSourceElement("c", new LinkedList<XmlSourceAttribute>(), "<c/>");
		XmlSourceElement b2 = new XmlSourceElement("b2", new LinkedList<XmlSourceAttribute>(), "<b2/>");
		a.setDepth(1, null);
		b.setDepth(2, a);
		c.setDepth(3, b);
		b2.setDepth(2, a);
		assertTrue(a.isAncestorOf(b));
		assertTrue(b.isAncestorOf(c));
		assertFalse(b2.isAncestorOf(c));
		assertTrue(a.isAncestorOf(c));
		assertFalse(b.isAncestorOf(b));
		assertTrue(b.isDescendantOf(a));
		assertTrue(b2.isDescendantOf(a));
		assertTrue(c.isDescendantOf(a));
		assertFalse(c.isDescendantOf(b2));
		assertFalse(c.isDescendantOf(c));
		XmlSourceElement bn = new XmlSourceElement("b", new LinkedList<XmlSourceAttribute>(), "<b><c/></b>");
		assertFalse("For now we assume that there is only one instance of each element for each document", bn.isDescendantOf(a));
		assertFalse(c.isDescendantOf(bn));
		assertFalse(a.isAncestorOf(bn));
		assertFalse(bn.isAncestorOf(c));
	}
	
}
