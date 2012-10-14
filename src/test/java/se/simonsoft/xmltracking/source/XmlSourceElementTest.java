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

	@Test
	public void testGetPos() {
		
	}
	
}
