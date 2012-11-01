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
package se.simonsoft.xmltracking.index.add;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import se.simonsoft.cms.indexing.xml.hook.IdStrategyRepoRevisionItemElem;
import se.simonsoft.cms.indexing.xml.hook.IndexingContext;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.inspection.CmsRepositoryInspection;
import se.simonsoft.xmltracking.source.XmlSourceAttribute;
import se.simonsoft.xmltracking.source.XmlSourceElement;

/**
 * Common requirements for id strategies.
 */
public class IdStrategyTest {

	private IndexingContext c = null;
	private IdStrategy s = null;
	
	protected IdStrategyRepoRevisionItemElem getImpl(IndexingContext c) {
		return new IdStrategyRepoRevisionItemElem(c);
	}
	
	@Before
	public void setUo() {
		c = mock(IndexingContext.class);
		
		// select implementation to test
		s = getImpl(c);
		
		CmsRepository repo1 = new CmsRepositoryInspection("/svn", "repo1", new File("."));
		CmsItemPath item1 = new CmsItemPath("/docs/a.xml");
		RepoRevision rev1 = new RepoRevision(77, new Date());
		when(c.getItemPath()).thenReturn(item1);
		when(c.getRepository()).thenReturn(repo1);
		when(c.getRevision()).thenReturn(rev1);
	}
	
	@Test
	public void testGetElementId() {
		
		s.start();
		
		XmlSourceElement e1 = new XmlSourceElement("figure",
				Arrays.asList(new XmlSourceAttribute("cms:component", "xz0")),
				"<figure cms:component=\"xz0\"><title>Title</title>Figure</figure>")
				.setDepth(1, null).setPosition(1, null);
		
		XmlSourceElement e2 = new XmlSourceElement("title",
				new LinkedList<XmlSourceAttribute>(),
				"<title>Title</title>")
				.setDepth(2, e1).setPosition(1, null);		
		
		String id1 = s.getElementId(e1);
		String id2 = s.getElementId(e2);
		assertTrue("ids should be unique", !id1.equals(id2));
		String id1b = s.getElementId(e1); // common for getting parent id while indexing
		// this is a requirement from indexing, so we can get id_parent etc in a stateless fashion
		assertEquals("ids should be re-generatable per element after each start()", id1, id1b);
		
		// dew document
		when(c.getItemPath()).thenReturn(new CmsItemPath("/f.xml"));
		s.start();
		
		String d2id1 = s.getElementId(e1);
		assertTrue("ids should be unique between documents", !id1.equals(d2id1));
		
	}

}
