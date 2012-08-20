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
package se.simonsoft.cms.indexing.xml.hook;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;

import org.junit.Test;

import se.simonsoft.cms.admin.CmsRepositoryInspection;
import se.simonsoft.cms.indexing.xml.hook.IdStrategyRepoRevisionItemElem;
import se.simonsoft.cms.indexing.xml.hook.IndexingContext;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.xmltracking.index.add.IdStrategy;
import se.simonsoft.xmltracking.source.XmlSourceAttribute;
import se.simonsoft.xmltracking.source.XmlSourceElement;

public class IdStrategyRepoRevisionItemElemTest {

	@Test
	public void testGetElementId() {
		IndexingContext c = mock(IndexingContext.class);
		IdStrategy s = new IdStrategyRepoRevisionItemElem(c);
		
		CmsRepository repo1 = new CmsRepositoryInspection("/svn", "repo1", new File("."));
		CmsItemPath item1 = new CmsItemPath("/docs/a.xml");
		RepoRevision rev1 = new RepoRevision(77, new Date());
		when(c.getItemPath()).thenReturn(item1);
		when(c.getRepository()).thenReturn(repo1);
		when(c.getRevision()).thenReturn(rev1);
		
		s.start();
		
		XmlSourceElement e1 = new XmlSourceElement("figure",
				Arrays.asList(new XmlSourceAttribute("cms:component", "xz0")),
				"<figure cms:component=\"xz0\"><title>Title</title>Figure</figure>")
				.setDepth(1, null).setPosition(1, null);
		
		XmlSourceElement e2 = new XmlSourceElement("title",
				new LinkedList<XmlSourceAttribute>(),
				"<title>Title</title>")
				.setDepth(2, e1).setPosition(1, null);		
		
		assertEquals("repo1^/docs/a.xml?p=77#0", s.getElementId(e1));
		assertEquals("repo1^/docs/a.xml?p=77#1", s.getElementId(e2));
		
		when(c.getItemPath()).thenReturn(new CmsItemPath("/f.xml"));
		
		s.start();
		
		assertEquals("repo1^/f.xml?p=77#0", s.getElementId(e1));
	}
	
	@Test(expected=IllegalStateException.class)
	public void testNotStarted() {
		new IdStrategyRepoRevisionItemElem(mock(IndexingContext.class)).getElementId(mock(XmlSourceElement.class));
	}

}
