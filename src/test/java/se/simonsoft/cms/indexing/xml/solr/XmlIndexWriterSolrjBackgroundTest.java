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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collection;

import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;

import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.indexing.xml.XmlIndexAddSession;

/**
 * CMS-1892: a batch send failure in the background executor must not be silently
 * discarded when the caller awaits completion via {@link XmlIndexAddSession#end()}.
 */
public class XmlIndexWriterSolrjBackgroundTest {

	@Test
	public void testBatchSendFailurePropagates() {
		XmlIndexWriterSolrjBackground writer = new XmlIndexWriterSolrjBackground(null) {
			@Override
			protected void doBatchSend(Collection<SolrInputDocument> pending) {
				throw new RuntimeException("simulated Solr failure");
			}
		};

		XmlIndexAddSession session = writer.get();
		IndexingDocIncrementalSolrj doc = new IndexingDocIncrementalSolrj();
		doc.addField("id", "some-id|00000001");
		session.add(doc);

		try {
			session.end();
			fail("A failed background batch send must be surfaced, not silently discarded");
		} catch (RuntimeException e) {
			assertEquals("simulated Solr failure", e.getCause().getMessage());
		}
	}

	@Test
	public void testBatchSendSuccessDoesNotThrow() {
		final Collection<SolrInputDocument>[] sent = new Collection[1];
		XmlIndexWriterSolrjBackground writer = new XmlIndexWriterSolrjBackground(null) {
			@Override
			protected void doBatchSend(Collection<SolrInputDocument> pending) {
				sent[0] = pending;
			}
		};

		XmlIndexAddSession session = writer.get();
		IndexingDocIncrementalSolrj doc = new IndexingDocIncrementalSolrj();
		doc.addField("id", "some-id|00000001");
		session.add(doc);

		session.end(); // should not throw

		assertEquals(1, sent[0].size());
	}

}
