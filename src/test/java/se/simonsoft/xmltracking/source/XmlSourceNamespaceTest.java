package se.simonsoft.xmltracking.source;

import static org.junit.Assert.*;

import org.junit.Test;

public class XmlSourceNamespaceTest {

	@Test
	public void testEquals() {
		assertTrue(new XmlSourceNamespace("x", "http://x").equals(new XmlSourceNamespace("x", "http://x")));
		assertFalse(new XmlSourceNamespace("x", "http://x").equals(new XmlSourceNamespace("x", "http://y")));
		assertFalse(new XmlSourceNamespace("x", "http://x").equals(new XmlSourceNamespace("y", "http://x")));
		assertFalse(new XmlSourceNamespace("x", "http://y").equals(new XmlSourceNamespace("x", "http://x")));
		assertFalse(new XmlSourceNamespace("y", "http://x").equals(new XmlSourceNamespace("x", "http://x")));
		assertFalse(new XmlSourceNamespace("x", "http://x").equals(null));
	}

	@Test
	public void testToString() {
		assertEquals("xmlns:cms=\"http://www.simonsoft.se/namespace/cms\"",
				new XmlSourceNamespace("cms", "http://www.simonsoft.se/namespace/cms").toString());
	}
	
}
