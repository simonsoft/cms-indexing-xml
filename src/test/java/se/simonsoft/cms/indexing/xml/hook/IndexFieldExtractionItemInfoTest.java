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
import java.util.Date;

import org.junit.Test;

import se.simonsoft.cms.admin.CmsRepositoryInspection;
import se.simonsoft.cms.indexing.xml.hook.IndexFieldExtractionItemInfo;
import se.simonsoft.cms.indexing.xml.hook.IndexingContext;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.xmltracking.index.add.IndexFieldExtraction;
import se.simonsoft.xmltracking.index.add.IndexFields;

public class IndexFieldExtractionItemInfoTest {

	@Test
	public void testDateValue() {
		// http://lucene.apache.org/solr/4_0_0/solr-core/org/apache/solr/schema/DateField.html
		assertEquals("1970-01-01T00:00:01.000Z", new IndexFieldExtractionItemInfo(null).getDateValue(new Date(1000)));
	}
	
	@Test
	public void testExtract() {
		CmsItemPath item1 = new CmsItemPath("/docs/a.xml");
		CmsItemPath item2 = new CmsItemPath("/docs/b.xml");
		CmsRepository repoa = new CmsRepository("http://localhost:88/svn1/r1");
		CmsRepository repob = new CmsRepositoryInspection("/svn2", "r3", new File("."));
		assertFalse(repob.isHostKnown());
		Date d2 = new Date(2000);
		Date d1 = new Date(d2.getTime() - 1000);
		RepoRevision rev1 = new RepoRevision(77, d1);
		RepoRevision rev2 = new RepoRevision(78, d2);
		
		IndexingContext context = mock(IndexingContext.class);
		when(context.getRepository()).thenReturn(repoa, repoa, repob);
		when(context.getRevision()).thenReturn(rev1, rev2, rev1);
		when(context.getItemPath()).thenReturn(item1, item1, item2);
		
		IndexFieldExtraction x = new IndexFieldExtractionItemInfo(context);
		
		IndexFields f1 = mock(IndexFields.class);
		x.extract(f1, null);
		verify(f1).addField("path", "/docs/a.xml");
		verify(f1).addField("pathname", "a.xml");
		verify(f1).addField("pathdir", "/docs");
		verify(f1).addField("pathext", "xml");
		verify(f1).addField("rev", 77L);
		verify(f1).addField("revt", "1970-01-01T00:00:01.000Z");
		verify(f1).addField("repo", "r1");
		verify(f1).addField("repoparent", "/svn1"); // if needed, can be undefined in solr schema
		verify(f1).addField("repohost", "localhost:88"); // if needed, can be undefined in solr schema
		
		IndexFields f2 = mock(IndexFields.class);
		x.extract(f2, null);
		verify(f2).addField("path", "/docs/a.xml");
		verify(f2).addField("rev", 78L);
		verify(f2).addField("revt", "1970-01-01T00:00:02.000Z");
		verify(f2).addField("repo", "r1");
		
		IndexFields f3 = mock(IndexFields.class);
		x.extract(f3, null);
		verify(f3).addField("path", "/docs/b.xml");
		verify(f3).addField("rev", 77L);
		verify(f3).addField("repo", "r3");
		verify(f3).addField("repoparent", "/svn2");
		// host not known
		verify(f3, times(0)).addField(eq("repohost"), anyString());
	}

	@Test
	public void testFileInRoot() {
		IndexingContext context = mock(IndexingContext.class);
		when(context.getRepository()).thenReturn(new CmsRepository("http://localhost:88/svn1/r1"));
		when(context.getRevision()).thenReturn(new RepoRevision(77, new Date()));
		when(context.getItemPath()).thenReturn(new CmsItemPath("/f.html"));
		
		IndexFieldExtraction x = new IndexFieldExtractionItemInfo(context);
		IndexFields extracted = mock(IndexFields.class);
		x.extract(extracted, null);
		// Subversion uses slash in root but that is inconsistent when all other paths lack traling slash. Let's be consistent.
		verify(extracted).addField("pathdir", "");
	}
	
}
