/**
 * Copyright (C) 2009-2017 Simonsoft Nordic AB
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
package se.simonsoft.cms.indexing.properties;

import net.sf.saxon.s9api.Processor;
import org.junit.Test;
import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.ItemContentBuffer;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.item.*;
import se.simonsoft.cms.item.info.CmsItemNotFoundException;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.properties.CmsItemPropertiesMap;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceReaderS9api;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HandlerSubjectSchemeTest {

	private final Processor processor = new Processor(false);
	private final XmlSourceReaderS9api sourceReader = new XmlSourceReaderS9api(processor);
    private final HandlerSubjectScheme handler = new HandlerSubjectScheme(sourceReader, processor);
	private String xml = 	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
							"<?xml-model href=\"urn:oasis:names:tc:dita:spec:classification:rng:subjectScheme.rng\" schematypens=\"http://relaxng.org/ns/structure/1.0\"?>\n" +
							"<subjectScheme>\n" +
							"    <hasInstance>\n" +
							"        <subjectdef keys=\"productname\">\n" +
							"            <topicmeta>\n" +
							"                <shortdesc>Product Name</shortdesc>\n" +
							"            </topicmeta>\n" +
							"            <subjectdef keys=\"Tempo_T_6\">\n" +
							"                <topicmeta>\n" +
							"                    <navtitle>Tempo T 6</navtitle>\n" +
							"                </topicmeta>\n" +
							"            </subjectdef>\n" +
							"            <subjectdef keys=\"Rapid_A_600J\"/>\n" +
							"            <subjectdef keys=\"Inspire_1200S\">\n" +
							"                <topicmeta>\n" +
							"                    <navtitle>Inspire 1200S</navtitle>\n" +
							"                </topicmeta>\n" +
							"            </subjectdef>\n" +
							"        </subjectdef>\n" +
							"    </hasInstance>\n" +
							"    \n" +
							"    <enumerationdef>\n" +
							"        <elementdef name=\"cms:property\"/>\n" +
							"        <attributedef name=\"cds:productname\"/>\n" +
							"        <subjectdef keyref=\"productname\"/>\n" +
							"    </enumerationdef>\n" +
							"</subjectScheme>";

	@Test
	public void testHandlerProjectLevel() {
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		when(item.isFile()).thenReturn(true);
		when(item.getPath()).thenReturn(new CmsItemPath("/dita/xml/bookmap1.ditamap"));
		IndexingItemProgress progress = mock(IndexingItemProgress.class);
		CmsItemPropertiesMap properties = new CmsItemPropertiesMap();
		when(progress.getProperties()).thenReturn(properties);
		when(progress.getFields()).thenReturn(doc);
		CmsRepository repo1 = new CmsRepository("http://cmshostname/svn/keydefmap1");
		when(progress.getRepository()).thenReturn(repo1);
		when(progress.getItem()).thenReturn(item);
		properties.and("cds:productname", "Inspire_1200S Tempo_T_6 Rapid_A_600J");
		ItemContentBufferStrategy cbs = mock(ItemContentBufferStrategy.class);
		ItemContentBuffer buffer = mock(ItemContentBuffer.class);
		when(buffer.getContents()).thenReturn(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		when(cbs.getBuffer(any(RepoRevision.class), eq(new CmsItemPath("/.cms/dita/properties.ditamap")), any(IndexingDoc.class))).thenReturn(buffer);
		handler.setItemContentBufferStrategy(cbs);
		handler.handle(progress);
		assertEquals(3, doc.getFieldValues("meta_s_m_prop_cds.productname").size());
		assertTrue(doc.getFieldValues("meta_s_m_prop_cds.productname").contains("Tempo T 6"));
		assertTrue(doc.getFieldValues("meta_s_m_prop_cds.productname").contains("Inspire 1200S"));
		assertTrue(doc.getFieldValues("meta_s_m_prop_cds.productname").contains("Rapid_A_600J"));
	}

	@Test
	public void testHandlerRepositoryLevel() {
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		when(item.isFile()).thenReturn(true);
		when(item.getPath()).thenReturn(new CmsItemPath("/dita/xml/bookmap1.ditamap"));
		IndexingItemProgress progress = mock(IndexingItemProgress.class);
		CmsItemPropertiesMap properties = new CmsItemPropertiesMap();
		when(progress.getProperties()).thenReturn(properties);
		when(progress.getFields()).thenReturn(doc);
		CmsRepository repo1 = new CmsRepository("http://cmshostname/svn/keydefmap1");
		when(progress.getRepository()).thenReturn(repo1);
		when(progress.getItem()).thenReturn(item);
		properties.and("cds:productname", "Inspire_1200S Tempo_T_6 Rapid_A_600J");
		ItemContentBufferStrategy cbs = mock(ItemContentBufferStrategy.class);
		ItemContentBuffer buffer = mock(ItemContentBuffer.class);
		ItemContentBuffer fallbackBuffer = mock(ItemContentBuffer.class);
		when(fallbackBuffer.getContents()).thenReturn(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		CmsItemId itemId = repo1.getItemId().withRelPath(new CmsItemPath("/.cms/dita/properties.ditamap"));
		when(buffer.getContents()).thenThrow(new CmsItemNotFoundException(itemId));
		when(cbs.getBuffer(any(RepoRevision.class), eq(new CmsItemPath("/.cms/dita/properties.ditamap")), any(IndexingDoc.class))).thenReturn(buffer);
		when(cbs.getBuffer(any(RepoRevision.class), eq(new CmsItemPath("/.cms/properties.ditamap")), any(IndexingDoc.class))).thenReturn(fallbackBuffer);
		handler.setItemContentBufferStrategy(cbs);
		handler.handle(progress);
		assertEquals(3, doc.getFieldValues("meta_s_m_prop_cds.productname").size());
		assertTrue(doc.getFieldValues("meta_s_m_prop_cds.productname").contains("Tempo T 6"));
		assertTrue(doc.getFieldValues("meta_s_m_prop_cds.productname").contains("Inspire 1200S"));
		assertTrue(doc.getFieldValues("meta_s_m_prop_cds.productname").contains("Rapid_A_600J"));
	}
}
