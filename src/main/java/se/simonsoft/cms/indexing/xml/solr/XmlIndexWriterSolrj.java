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
package se.simonsoft.cms.indexing.xml.solr;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.indexing.xml.XmlIndexAddSession;
import se.simonsoft.cms.indexing.xml.XmlIndexWriter;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;

public class XmlIndexWriterSolrj implements Provider<XmlIndexAddSession>, XmlIndexWriter {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private SolrServer solrServer;

	@Inject
	public XmlIndexWriterSolrj(@Named("reposxml") SolrServer core) {
		this.solrServer = core;
	}
	
	@Override
	public XmlIndexAddSession get() {
		return new Session();
	}
	
	protected void batchSend(Session session) {
		Collection<SolrInputDocument> pending = session.pending;
		if (pending.size() == 0) {
			logger.warn("Send to solr attempted with empty document list");
			return;
		}
		logger.info("Sending {} elements to Solr starting with id {}", pending.size(), pending.iterator().next().getFieldValue("id"));
		try {
			solrServer.add(pending);
		} catch (SolrServerException e) {
			throw new RuntimeException("Error not handled", e);
		} catch (IOException e) {
			throw new RuntimeException("Error not handled", e);
		}
		pending.clear();
	}
	
	protected void sessionEnd(Session session) {
		batchSend(session);
	}
	
	@Override
	public void deletePath(CmsRepository repository, CmsChangesetItem c) {
		// we can't use id to delete because it may contain revision, we could probably delete an exact item by hooking into the head=false update in item indexing
		String query = "pathfull:\"" + repository.getPath() + c.getPath().toString() + '"';
		logger.debug("Deleting previous revision of {} using query {}", c, query);
		try {
			solrServer.deleteByQuery(query);
		} catch (SolrServerException e) {
			throw new RuntimeException("not handled", e);
		} catch (IOException e) {
			throw new RuntimeException("not handled", e);
		}		
	}

	@Override
	public void onRevisionEnd(RepoRevision revision) {
		commit();
		if (revision.getNumber() % 1000 == 0) {
			logger.info("Optimizing index at revision {}", revision);
			optimize();
		}
	}
	
	private void commit() {
		try {
			solrServer.commit();
		} catch (SolrServerException e) {
			throw new RuntimeException("Error not handled", e);
		} catch (IOException e) {
			throw new RuntimeException("Error not handled", e);
		}
	}

	private void optimize() {
		try {
			solrServer.optimize();
		} catch (SolrServerException e) {
			logger.error("Index optimize failed: {}", e.getMessage(), e);
			// we can live without optimized index, could fail because optimize needs lots of free disk
		} catch (IOException e) {
			logger.error("Solr connection issues at optimize: ", e.getMessage(), e);
			throw new RuntimeException("Optimize failed", e);
		}
	}	

	class Session implements XmlIndexAddSession {

		private Collection<SolrInputDocument> pending = new LinkedList<SolrInputDocument>();
		
		@Override
		public void end() {
			sessionEnd(this);
		}
		
		private SolrInputDocument getSolrDoc(IndexingDoc doc) {
			if (doc instanceof IndexingDocIncrementalSolrj) {
				return ((IndexingDocIncrementalSolrj) doc).getSolrDoc();
			}
			throw new IllegalArgumentException("Unsupported IndexingDoc type " + doc.getClass());
		}
		
		@Override
		public int size() {
			return pending.size();
		}

		@Override
		public boolean add(IndexingDoc e) {
			if (!pending.add(getSolrDoc(e))) {
				throw new IllegalArgumentException("Doc add failed for " + e);
			}
//TODO			// we have a rough measurement of total field size here and can trigger batch send to reduce risk of hitting memory limitations in webapp
//			if (batchTextTotal > BATCH_TEXT_TOTAL_MAX) {
//				logger.info("Sending batch because total source+text size {} indicates large update", batchTextTotal);
//				batchReady = true; // send batch
//				batchTextTotal = 0;
//			}
			return true;
		}

		@Override
		public boolean addAll(Collection<? extends IndexingDoc> c) {
			boolean changed = false;
			for (IndexingDoc d : c) {
				changed = add(d) || changed;
			}
			return changed;
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException("Method not implemented for Solr add batch");
		}

		@Override
		public boolean contains(Object o) {
			throw new UnsupportedOperationException("Method not implemented for Solr add batch");
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			throw new UnsupportedOperationException("Method not implemented for Solr add batch");
		}

		@Override
		public boolean isEmpty() {
			throw new UnsupportedOperationException("Method not implemented for Solr add batch");
		}

		@Override
		public Iterator<IndexingDoc> iterator() {
			throw new UnsupportedOperationException("Method not implemented for Solr add batch");
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException("Method not implemented for Solr add batch");
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException("Method not implemented for Solr add batch");
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException("Method not implemented for Solr add batch");
		}

		@Override
		public Object[] toArray() {
			throw new UnsupportedOperationException("Method not implemented for Solr add batch");
		}

		@Override
		public <T> T[] toArray(T[] a) {
			throw new UnsupportedOperationException("Method not implemented for Solr add batch");
		}
		
	}

}
