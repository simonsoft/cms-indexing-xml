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

import static org.junit.Assert.*;

import org.junit.Test;

public class TreeLocationTest {

	@Test
	public void test() {
		TreeLocation p = new TreeLocation("1.23.456");
		assertEquals(456, p.getOrdinal());
	}
	
	@Test
	public void testIntConstructor() {
		assertEquals(1, new TreeLocation(1).getOrdinal());
		assertEquals(943, new TreeLocation(943).getOrdinal());
	}
	
	@Test
	public void testBuild() {
		TreeLocation p = new TreeLocation("1");
		assertEquals("1.33", p.withChild(33).toString());
		assertEquals(33, p.withChild(33).getOrdinal());
		assertEquals("1.23.555", p.withChild(23).withChild(555).toString());
		assertEquals(555, p.withChild(23).withChild(555).getOrdinal());
	}
	
	@Test
	public void testBuildReverse() {
		TreeLocation p = new TreeLocation(2);
		TreeLocation pp = p.withParent(98765);
		assertEquals(2, pp.getOrdinal());
		assertEquals("98765.2", pp.toString());
	}
	
	@Test
	public void testEquals() {
		assertTrue(new TreeLocation("1.2.3").equals(new TreeLocation("1.2.3")));
		assertFalse(new TreeLocation("1.2.3").equals(new TreeLocation("1.2")));
		assertEquals(new TreeLocation("1.2.3").hashCode(), new TreeLocation("1.2.3").hashCode());
	}
	
	@Test
	public void testRelations() {
		assertTrue(new TreeLocation("1.2.3").isAncestorOf(new TreeLocation("1.2.3.4.5")));
		assertFalse(new TreeLocation("1.2.3").isAncestorOf(new TreeLocation("1.2.3")));
		assertFalse(new TreeLocation("1.2.3").isAncestorOf(new TreeLocation("1.2.33")));
		
		assertTrue(new TreeLocation("1.2.3.4").isDescendantOf(new TreeLocation("1.2")));
		assertFalse(new TreeLocation("1.2.3.4").isDescendantOf(new TreeLocation("1.2.3.456")));
		
		assertTrue(new TreeLocation("1.2").isParentOf(new TreeLocation("1.2.3")));
		assertFalse(new TreeLocation("1").isParentOf(new TreeLocation("1.2.3")));
		assertTrue(new TreeLocation("1.2.3").isChildOf(new TreeLocation("1.2")));
		assertFalse(new TreeLocation("1.2.3.4").isChildOf(new TreeLocation("1.2")));
	}

}
