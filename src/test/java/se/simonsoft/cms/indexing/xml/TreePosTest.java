package se.simonsoft.cms.indexing.xml;

import static org.junit.Assert.*;

import org.junit.Test;

public class TreePosTest {

	@Test
	public void test() {
		TreePos p = new TreePos("1.23.456");
		assertEquals(456, p.getOrdinal());
	}
	
	@Test
	public void testIntConstructor() {
		assertEquals(1, new TreePos(1).getOrdinal());
		assertEquals(943, new TreePos(943).getOrdinal());
	}
	
	@Test
	public void testBuild() {
		TreePos p = new TreePos("1");
		assertEquals("1.33", p.withChild(33).toString());
		assertEquals(33, p.withChild(33).getOrdinal());
		assertEquals("1.23.555", p.withChild(23).withChild(555).toString());
		assertEquals(555, p.withChild(23).withChild(555).getOrdinal());
	}
	
	@Test
	public void testBuildReverse() {
		TreePos p = new TreePos(2);
		TreePos pp = p.withParent(98765);
		assertEquals(2, pp.getOrdinal());
		assertEquals("98765.2", pp.toString());
	}
	
	@Test
	public void testEquals() {
		assertTrue(new TreePos("1.2.3").equals(new TreePos("1.2.3")));
		assertFalse(new TreePos("1.2.3").equals(new TreePos("1.2")));
		assertEquals(new TreePos("1.2.3").hashCode(), new TreePos("1.2.3").hashCode());
	}

}
