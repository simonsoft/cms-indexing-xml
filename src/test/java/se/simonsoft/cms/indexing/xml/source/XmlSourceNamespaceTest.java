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
package se.simonsoft.cms.indexing.xml.source;

import static org.junit.Assert.*;

import org.junit.Test;

import se.simonsoft.xmltracking.source.XmlSourceNamespace;

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
