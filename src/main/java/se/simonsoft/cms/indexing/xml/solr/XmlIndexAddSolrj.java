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

public class XmlIndexAddSolrj implements Provider<XmlIndexAddSession> {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private SolrServer solrServer;

	@Inject
	public XmlIndexAddSolrj(@Named("reposxml") SolrServer core) {
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
			return pending.add(getSolrDoc(e));
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
