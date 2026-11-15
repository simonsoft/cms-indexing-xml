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
package se.simonsoft.cms.indexing.xml.solr;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.junit.Test;

import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.indexing.xml.XmlIndexAddSession;
import se.simonsoft.cms.indexing.xml.XmlIndexingGuard;

public class XmlIndexWriterHeadTest {
    @Test
    public void rechecksOnBackgroundThreadAfterScheduling() throws Exception {
        SolrClient solr = mock(SolrClient.class);
        Thread caller = Thread.currentThread();
        XmlIndexAddSession session = new XmlIndexWriterSolrjBackground(solr)
                .get(new XmlIndexingGuard(() -> Thread.currentThread() == caller));
        session.add(document(false));
        try {
            assertThrows(XmlIndexingGuard.StaleItemException.class, session::end);
        } finally {
            session.abort();
        }
        verifyNoInteractions(solr);
    }

    @Test
    public void backgroundLookupFailurePropagates() {
        SolrClient solr = mock(SolrClient.class);
        Thread caller = Thread.currentThread();
        IllegalStateException lookupFailure = new IllegalStateException("SVN lookup failed");
        XmlIndexAddSession session = new XmlIndexWriterSolrjBackground(solr)
                .get(new XmlIndexingGuard(() -> {
                    if (Thread.currentThread() != caller) {
                        throw lookupFailure;
                    }
                    return true;
                }));
        session.add(document(false));
        try {
            assertSame(lookupFailure, assertThrows(RuntimeException.class, session::end).getCause());
        } finally {
            session.abort();
        }
        verifyNoInteractions(solr);
    }

    @Test
    public void staleBeforeSchedulingSendsNothing() {
        SolrClient solr = mock(SolrClient.class);
        XmlIndexAddSession session = new XmlIndexWriterSolrjBackground(solr)
                .get(new XmlIndexingGuard(() -> false));
        session.add(document(false));
        try {
            assertThrows(XmlIndexingGuard.StaleItemException.class, session::end);
        } finally {
            session.abort();
        }
        verifyNoInteractions(solr);
    }

    @Test
    public void synchronousWriterAlsoChecksHead() {
        SolrClient solr = mock(SolrClient.class);
        XmlIndexAddSession session = new XmlIndexWriterSolrj(solr)
                .get(new XmlIndexingGuard(() -> false));
        session.add(document(false));
        assertThrows(XmlIndexingGuard.StaleItemException.class, session::end);
        session.abort();
        verifyNoInteractions(solr);
    }

    @Test
    public void retryRechecksHeadBeforeSendingAgain() throws Exception {
        SolrClient solr = mock(SolrClient.class);
        AtomicBoolean current = new AtomicBoolean(true);
        when(solr.add(anyCollection())).thenAnswer(call -> {
            current.set(false);
            throw new IOException("first send failed");
        });
        XmlIndexAddSession session = new XmlIndexWriterSolrjBackground(solr)
                .get(new XmlIndexingGuard(current::get));
        session.add(document(false));
        try {
            assertThrows(XmlIndexingGuard.StaleItemException.class, session::end);
        } finally {
            session.abort();
        }
        verify(solr, times(1)).add(anyCollection());
    }

    @Test
    public void abortWaitsForActiveWriteAndCancelsQueuedBatch() throws Exception {
        assertAbortDrains(false);
    }

    @Test
    public void abortDoesNotHideFailureFromActiveWrite() throws Exception {
        assertAbortDrains(true);
    }

    private void assertAbortDrains(boolean failWrite) throws Exception {
        SolrClient solr = mock(SolrClient.class);
        CountDownLatch writing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(solr.add(anyCollection())).thenAnswer(call -> {
            writing.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            if (failWrite) {
                throw new IllegalStateException("active batch failed");
            }
            return new UpdateResponse();
        });
        XmlIndexAddSession session = new XmlIndexWriterSolrjBackground(solr)
                .get(new XmlIndexingGuard(() -> true));
        ExecutorService caller = Executors.newSingleThreadExecutor();
        try {
            session.add(document(true));
            assertTrue(writing.await(5, TimeUnit.SECONDS));
            session.add(document(true));
            Future<?> abort = caller.submit(session::abort);
            assertThrows(TimeoutException.class, () -> abort.get(100, TimeUnit.MILLISECONDS));
            release.countDown();
            if (failWrite) {
                java.util.concurrent.ExecutionException failure = assertThrows(
                        java.util.concurrent.ExecutionException.class, () -> abort.get(5, TimeUnit.SECONDS));
                assertEquals("active batch failed", failure.getCause().getCause().getMessage());
            } else {
                abort.get(5, TimeUnit.SECONDS);
            }
            verify(solr, times(1)).add(anyCollection());
        } finally {
            release.countDown();
            caller.shutdownNow();
            session.abort();
        }
    }

    private IndexingDocIncrementalSolrj document(boolean batch) {
        IndexingDocIncrementalSolrj doc = new IndexingDocIncrementalSolrj();
        doc.setField("id", "item-r2|00000001");
        doc.setField("depth", 1);
        if (batch) {
            doc.setField("source", "x".repeat(500_000));
        }
        return doc;
    }
}
