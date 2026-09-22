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
package se.simonsoft.cms.indexing.xml;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import net.sf.saxon.s9api.Processor;
import se.repos.indexing.IndexingHandlerException;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldXslPipeline;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceReaderS9api;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceDocumentS9api;
import se.simonsoft.cms.xmlsource.transform.TransformerService;
import se.simonsoft.cms.xmlsource.transform.TransformerServiceFactory;

public class HandlerXmlHeadTest {
    private final XmlIndexWriter writer = mock(XmlIndexWriter.class);
    private final XmlIndexAddSession session = mock(XmlIndexAddSession.class);
    private final TransformerService normalize = mock(TransformerService.class);
    private final XmlIndexFieldXslPipeline pipeline = mock(XmlIndexFieldXslPipeline.class);
    private final CmsChangesetItem item = mock(CmsChangesetItem.class);
    private final CmsRepository repository = mock(CmsRepository.class);
    private final RepoRevision revision = new RepoRevision(2, null);
    private final IndexingItemProgress progress = mock(IndexingItemProgress.class);
    private final IndexingDocIncrementalSolrj fields = new IndexingDocIncrementalSolrj();
    private final AtomicBoolean current = new AtomicBoolean(true);
    private XmlSourceReaderS9api reader;
    private HandlerXml handler;

    @Before
    public void setUp() {
        XmlIndexingHandlersProducer producer = new XmlIndexingHandlersProducer();
        Processor processor = producer.createProcessor();
        reader = spy(producer.createXmlSourceReader(processor));
        TransformerServiceFactory factory = mock(TransformerServiceFactory.class);
        when(factory.buildTransformerService("reuse-normalize.xsl")).thenReturn(normalize);
        when(normalize.transform(any(XmlSourceDocumentS9api.class), any())).thenAnswer(call -> call.getArgument(0));
        when(pipeline.doTransformPipeline(any(), any())).thenAnswer(call -> call.getArgument(0));
        when(writer.get(any(XmlIndexingGuard.class))).thenReturn(session);
        handler = new HandlerXml(processor, reader, factory);
        handler.setDependenciesIndexing(writer);
        handler.setXslPipeline(pipeline);
        handler.setFieldExtraction(Set.of());
        handler.setConfigIndexing(10 * 1048576, "");
        when(item.isFile()).thenReturn(true);
        when(item.isAdd()).thenReturn(true);
        when(item.getFilesize()).thenReturn(100L);
        when(item.getPath()).thenReturn(new CmsItemPath("/test.xml"));
        when(progress.getRepository()).thenReturn(repository);
        when(progress.getRevision()).thenReturn(revision);
        when(progress.getItem()).thenReturn(item);
        when(progress.getFields()).thenReturn(fields);
        when(progress.getContents()).thenReturn(new ByteArrayInputStream("<doc><p>text</p></doc>".getBytes()));
        fields.setField("id", "item-r2");
        fields.setField("pathext", "xml");
        fields.setField("embd_Content-Type", "application/xml");
    }

    @Test
    public void staleBeforeReadKeepsHistoricalMetadataWithoutXmlWrites() {
        current.set(false);
        handler.handle(progress, current::get);
        assertMetadataOnly();
        verifyNoInteractions(writer, normalize, pipeline);
    }

    @Test
    public void stopsBeforeNormalizationIfHeadChangesDuringRead() {
        doAnswer(call -> {
            Object result = call.callRealMethod();
            current.set(false);
            return result;
        }).when(reader).read(any(java.io.InputStream.class));
        handler.handle(progress, current::get);
        assertMetadataOnly();
        verifyNoInteractions(writer, normalize, pipeline);
    }

    @Test
    public void stopsBeforeXmlTransformAndCleansOnlyCandidateAfterAbort() {
        when(normalize.transform(any(XmlSourceDocumentS9api.class), any())).thenAnswer(call -> {
            current.set(false);
            return call.getArgument(0);
        });
        handler.handle(progress, current::get);
        assertMetadataOnly();
        verifyNoInteractions(pipeline);
        verify(session, never()).add(any());
        assertAbortBeforeCleanup();
    }

    @Test
    public void stopsBeforeElementExtractionIfHeadChangesDuringTransform() {
        when(pipeline.doTransformPipeline(any(), any())).thenAnswer(call -> {
            current.set(false);
            return call.getArgument(0);
        });
        handler.handle(progress, current::get);
        assertMetadataOnly();
        verify(session, never()).add(any());
        assertAbortBeforeCleanup();
    }

    @Test
    public void batchFailureAbortsCleansAndPropagates() {
        doThrow(new IllegalStateException("Solr batch failed")).when(session).end();
        IndexingHandlerException failure = assertThrows(IndexingHandlerException.class,
                () -> handler.handle(progress, current::get));
        assertEquals("Solr batch failed", failure.getCause().getMessage());
        assertTrue(fields.getFieldValues("flag").contains(HandlerXml.FLAG_XML_ERROR));
        assertFalse(fields.getFieldValues("flag").contains(HandlerXml.FLAG_XML));
        assertAbortBeforeCleanup();
    }

    @Test
    public void lookupFailureIsNotTreatedAsAnObsoleteItem() {
        IllegalStateException failure = new IllegalStateException("SVN unavailable");
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> handler.handle(progress, () -> { throw failure; })));
        verifyNoInteractions(writer, normalize, pipeline);
    }

    @Test
    public void abortFailurePreventsCleanupWhileWritesMayStillBeRunning() {
        when(normalize.transform(any(XmlSourceDocumentS9api.class), any())).thenAnswer(call -> {
            current.set(false);
            return call.getArgument(0);
        });
        doThrow(new IllegalStateException("writer did not terminate")).when(session).abort();
        assertThrows(IllegalStateException.class, () -> handler.handle(progress, current::get));
        verify(writer, never()).deleteRevision(any(), any(), any());
    }

    @Test
    public void cleanupFailureMustNotBecomeSuccessfulSkip() {
        when(normalize.transform(any(XmlSourceDocumentS9api.class), any())).thenAnswer(call -> {
            current.set(false);
            return call.getArgument(0);
        });
        IllegalStateException failure = new IllegalStateException("cleanup failed");
        doThrow(failure).when(writer).deleteRevision(repository, item, revision);
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> handler.handle(progress, current::get)));
    }

    private void assertMetadataOnly() {
        assertTrue(fields.getFieldValues("flag").contains(HandlerXml.FLAG_XML_REPOSITEM));
        assertFalse(fields.getFieldValues("flag").contains(HandlerXml.FLAG_XML));
        assertFalse(fields.getFieldValues("flag").contains(HandlerXml.FLAG_XML_ERROR));
    }

    private void assertAbortBeforeCleanup() {
        InOrder order = inOrder(session, writer);
        order.verify(session).abort();
        order.verify(writer).deleteRevision(repository, item, revision);
        verify(writer, never()).deletePath(any(), any());
    }
}
