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

import java.time.Duration;
import java.time.Instant;
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
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.solrj.SolrAdd;
import se.repos.indexing.solrj.SolrDelete;
import se.repos.indexing.solrj.SolrDeleteByQuery;
import se.repos.indexing.solrj.SolrQueryOp;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.indexing.xml.XmlIndexAddSession;
import se.simonsoft.cms.indexing.xml.XmlIndexWriter;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexIdAppendDepthFirstPosition;
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
	
	private static final Logger logger = LoggerFactory.getLogger(XmlIndexWriterSolrj.class);
	
	private SolrClient solrServer;

	private static final int ELEMENT_ID_LENGTH = XmlIndexIdAppendDepthFirstPosition.getElementId(1).length();
	private static final int DELETE_PAGE_SIZE = 1000;
	public static boolean deleteByQueryAllowed = true; // Used by testing to ensure the efficient delete is used.
	
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
	
		
	public void deletePath(CmsRepository repository, CmsChangesetItem c) {
		// Query for the id as well as number of elements.
		SolrQuery query = getDeleteQuery(repository, c);
		QueryResponse existing = new SolrQueryOp(solrServer, query).run();
		
		long count = existing.getResults().getNumFound();
		if (count == 0) {
			// TODO: Consider adding placeholder item for overwritten items and deletePathByQuery().
			logger.warn("No previous docs to delete (normal during batch indexing): {}", c);
			return;
		}
		
		String id1Base = getIdBase(existing.getResults().get(0), c); 
		
		if (count > 1) {
			// Should have 2 rows in the response.
			// Would be a strange situation if they are from different revisions (using sort on depth).
			// Indicates that earlier delete operation has failed.
			String id2Base = getIdBase(existing.getResults().get(1), c); 
			if (!id2Base.equals(id1Base)) {
				logger.warn("Delete query provided multiple revisions in reposxml: {}.. - {}..", id1Base, id2Base);
				deletePathByQuery(repository, c);
				return;
			}
		}
		logger.info("Deleting previous revision ({} docs): {}", count, id1Base);
		deleteIds(id1Base, count);
		logger.info("Deleted previous revision ({} docs): {}", count, id1Base);
	}
	
	/**
	 * @param idBase with separator '|'
	 * @param count number of elements, starting at 1.
	 */
	private void deleteIds(String idBase, long count) {
		// Paged delete for large documents, reverse order.
		// Not bothering with partial last page.
		Instant start = Instant.now(); 
		long pages = (count / DELETE_PAGE_SIZE) + 1; // Adding one page for the division remainder.
		for (long i = (pages-1); i >= 0 ; i--) { // Reverse to ensure that depth=1 is deleted last.
			deleteIdPage(idBase, i);
		}
		// TODO: Change to debug level
		Instant end = Instant.now(); 
		logger.info("Deleted previous revision ({} pages) in {} ms: {}", pages, Duration.between(start, end).toMillis(), idBase);
	}
	
	private void deleteIdPage(String idBase, long page) {
		LinkedList<String> ids = new LinkedList<>();
		for (long i = DELETE_PAGE_SIZE * page; i <= DELETE_PAGE_SIZE * (page+1) ; i++) { // overlap one
			ids.add(idBase + XmlIndexIdAppendDepthFirstPosition.getElementId(i));
		}
		new SolrDelete(solrServer, ids).run();
	}
	
	public static SolrQuery getDeleteQuery(CmsRepository repository, CmsChangesetItem c) {
		String pathfull = getPathFull(repository, c);
		SolrQuery query = new SolrQuery("pathfull:"+ quote(pathfull));
		// Ensure that the first element of multiple revisions are returned (if there are previous delete failures).
		query = query.addSort("depth", ORDER.asc); // Do NOT add a revision sort with higher priority.
		query = query.setFields("id");
		query = query.setRows(2); // Validating that 2 IDs relate to the same document revision.
		return query;
	}
	
	public static String getIdBase(SolrDocument fields, CmsChangesetItem c) {
		String id = (String) fields.getFieldValue("id");
		if (id == null || id.charAt(id.length() - ELEMENT_ID_LENGTH) == '|') {
			String msg = MessageFormatter.format("Delete query provided an illegal response: {} - {}", id, c).getMessage();
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
		return id.substring(0, id.length() - ELEMENT_ID_LENGTH); // Keep the '|'.
	}
	
	
	private void deletePathByQuery(CmsRepository repository, CmsChangesetItem c) {
		// Keeping this method as fallback.
		logger.warn("Deleting previous revision using fallback to 'deleteByQuery' (slow): {}", c);
		if (!deleteByQueryAllowed) {
			throw new IllegalStateException("deleteByQuery is disabled by configuration");
		}
		
		// we can't use id to delete because it may contain revision, we could probably delete an exact item by hooking into the head=false update in item indexing
		// reposxml generates an unknown number of docs per cmsitem (at least for Release / Assist). Can not be deleted by a single ID.
		
		// DeleteByQuery turns out to be a significant performance issue, potentially more so in SolR 8 than SolR 4.
		// https://www.od-bits.com/2018/03/dbq-or-delete-by-query.html
		String pathfull = getPathFull(repository, c);
		String query = "pathfull:"+ quote(pathfull);
		logger.debug("Deleting previous revision of {} using query {}", c, query);
		new SolrDeleteByQuery(solrServer, query).run();	
	}
	
	private static String getPathFull(CmsRepository repository, CmsChangesetItem c) {
		return repository.getPath() + c.getPath().toString();
	}
	
	@Override
	public void commit(boolean expungeDeletes) {
		
		// Unable to use retry in SolrOp unless this interface is changed.
		// Alternatively if the consumer of this interface would only use expungeDeletes after pure-delete changes.
		
		logger.warn("The per-document commit is now disabled.");
		//new SolrCommitExpunge(solrServer, expungeDeletes, false);
	}
	
	// Copied from QueryEscapeDefault in cms-reporting.
	public static String quote(String fieldValue) {
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
