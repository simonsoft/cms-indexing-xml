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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.solrj.SolrAdd;
import se.repos.indexing.solrj.SolrDelete;
import se.repos.indexing.solrj.SolrDeleteByQuery;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.indexing.xml.XmlIndexAddSession;
import se.simonsoft.cms.indexing.xml.XmlIndexWriter;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;

public class XmlIndexWriterSolrj implements Provider<XmlIndexAddSession>, XmlIndexWriter {

//	private static final long BATCH_SIZE_MAX = 500; // This is a quick fix for avoiding "java.lang.OutOfMemoryError: Java heap space" without really analyzing the problem. 1500 and above has proved too large.
//	// The occurrence of the above error might be because of text size, so resurrecting the old text length count could be a good idea.
	
	/**
	 * As element size varies a lot due to source and text indexing we can
	 * try to keep reasonably small batches by also checking total text+source length,
	 * triggering batchReady if above a certain limit instead of waiting for the number of elements.
	 */
	// Tests indicate that with a 300MB heap, the performance is optimal around 500 kB batch size.
	private static final long BATCH_TEXT_TOTAL_MAX = 500 * 1000;	
	
	// Limit triggering logging of large element. Should likely be in range 4000-10000.
	private static final long SIZE_INFO_ABOVE = 8000;
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private SolrClient solrServer;

	@Inject
	public XmlIndexWriterSolrj(@Named("reposxml") SolrClient core) {
		this.solrServer = core;
	}
	
	@Override
	public XmlIndexAddSession get() {
		return new Session();
	}
	
	protected void batchSend(Session session) {
		
		Collection<SolrInputDocument> pending = session.rotatePending();
		if (pending.size() == 0) {
			logger.warn("Send to solr attempted with empty document list");
			return;
		}
		logger.info("Sending {} elements size {} to Solr starting with id {}", pending.size(), session.sizeContentTotal(), pending.iterator().next().getFieldValue("id"));
		doBatchSend(pending);
	}
	
	protected void doBatchSend(Collection<SolrInputDocument> pending) {
		new SolrAdd(solrServer, pending).run();
	}
	
	protected void sessionEnd(Session session) {
		batchSend(session);
	}
	
	@Override
	public void deleteId(String id, boolean deep) {
		if (deep) {
			logger.info("Performing deep delete in reposxml, might be slow: {}", id);
			
			String query = "id:"+ id + "*"; // The id should be safe without escaping.
			new SolrDeleteByQuery(solrServer, query).run();	
		} else {
			new SolrDelete(solrServer, id).run();
		}
	}
	
	
	@Override
	public void deletePath(CmsRepository repository, CmsChangesetItem c) {
		// we can't use id to delete because it may contain revision, we could probably delete an exact item by hooking into the head=false update in item indexing
		// reposxml generates an unknown number of docs per cmsitem (at least for Release / Assist). Can not be deleted by a single ID.
		
		// DeleteByQuery turns out to be a significant performance issue, potentially more so in SolR 8 than SolR 4.
		// https://www.od-bits.com/2018/03/dbq-or-delete-by-query.html
		String pathfull = repository.getPath() + c.getPath().toString();
		String query = "pathfull:"+ quote(pathfull);
		logger.debug("Deleting previous revision of {} using query {}", c, query);
		new SolrDeleteByQuery(solrServer, query).run();	
	}
	
	@Override
	public void commit(boolean expungeDeletes) {
		
		// Unable to use retry in SolrOp unless this interface is changed.
		// Alternatively if the consumer of this interface would only use expungeDeletes after pure-delete changes.
		
		logger.info("The per-document commit is now disabled.");
		//new SolrCommitExpunge(solrServer, expungeDeletes, false);
	}
	
	// Copied from QueryEscapeDefault in cms-reporting.
	public String quote(String fieldValue) {
		return '"' + fieldValue.replace("\"", "\\\"") + '"';
	}

	class Session implements XmlIndexAddSession {

		private Collection<SolrInputDocument> pending = new LinkedList<SolrInputDocument>();
		
		private int contentSize = 0;
		
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
		
		public Collection<SolrInputDocument> rotatePending() {
			
			Collection<SolrInputDocument> returnPending = this.pending;
			pending = new LinkedList<SolrInputDocument>();
			return returnPending;
		}
		
		@Override
		public int size() {
			return pending.size();
		}


		@Override
		public int sizeContentTotal() {
			return contentSize;
		}
		
		private Entry<String, Integer> getLargestField(IndexingDoc e) {
			@SuppressWarnings("unused")
			final Set<String> largeCandidates = new HashSet<String>(Arrays.asList("id", "prop_abx.Dependencies", "source", "source_reuse"));
			// text is also large
			
			String name = null;
			int size = 0;
			for (String f : e.getFieldNames()) {
			//for (String f : largeCandidates) {
				int valSize = 0;
				Object val = e.getFieldValue(f);
				if (val instanceof String) {
					valSize = ((String) val).length();
				}
				if (valSize > size) {
					size = valSize;
					name = f;
				}
			}
			return new AbstractMap.SimpleEntry<String, Integer>(name, size);
		}
		
		@Override
		public boolean add(IndexingDoc e) {
			if (size() == 0) {
				contentSize = 0;
			}
			int s = e.getContentSize();
			if (s >= SIZE_INFO_ABOVE && ((Integer) e.getFieldValue("depth")) > 1) {
				Entry<String, Integer> l = getLargestField(e);
				logger.info("Large element '{}' {}, fields {}, total size {}, largest field {}:{}", 
						new Object[] {e.getFieldValue("name"), e.getFieldValue("id"), e.size(), s, l.getKey(), l.getValue()});
			}
			if (!pending.add(getSolrDoc(e))) {
				throw new IllegalArgumentException("Doc add failed for " + e);
			}
			contentSize += s;
//TODO			// we have a rough measurement of total field size here and can trigger batch send to reduce risk of hitting memory limitations in webapp
//			if (batchTextTotal > BATCH_TEXT_TOTAL_MAX) {
//				logger.info("Sending batch because total source+text size {} indicates large update", batchTextTotal);
//				batchReady = true; // send batch
//				batchTextTotal = 0;
//			}
			if (contentSize >= BATCH_TEXT_TOTAL_MAX) {
				logger.info("Reached max batch add size {} after {} elements, forcing send to solr", BATCH_TEXT_TOTAL_MAX, pending.size());
				batchSend(this);
			}
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
