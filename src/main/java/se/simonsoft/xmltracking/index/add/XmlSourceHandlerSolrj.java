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
package se.simonsoft.xmltracking.index.add;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.xmltracking.index.SchemaFieldNames;
import se.simonsoft.xmltracking.index.SchemaFieldNamesReposxml;
import se.simonsoft.xmltracking.source.XmlSourceAttribute;
import se.simonsoft.xmltracking.source.XmlSourceElement;
import se.simonsoft.xmltracking.source.XmlSourceHandler;
import se.simonsoft.xmltracking.source.XmlSourceNamespace;

/**
 * Extracts source fields internally and using {@link IndexFieldExtraction}s (the former being legacy)
 * and sends batches to solr using javabin format.
 */
public class XmlSourceHandlerSolrj implements XmlSourceHandler {

	private static final Logger logger = LoggerFactory.getLogger(XmlSourceHandlerSolrj.class);
	
	// probably no need to configure
	public static final SchemaFieldNames DEFAULT_FIELD_NAMES = new SchemaFieldNamesReposxml();
	private SchemaFieldNames fieldNames = DEFAULT_FIELD_NAMES;

	private IdStrategy idStrategy;
	private SolrServer solrServer;
	private Set<IndexFieldExtraction> extraction;
	
	private transient List<String> sent = null;
	private transient List<String> failed = null;
	
	public static final int UPDATE_BATCH_MAX_SIZE = 100;
	private List<SolrInputDocument> pending = new ArrayList<SolrInputDocument>(UPDATE_BATCH_MAX_SIZE);
	private boolean updateBatchReady = false; // trigger send before batch max size is reached
	
	@Inject
	public XmlSourceHandlerSolrj(@Named("reposxml") SolrServer solrServer, IdStrategy idStrategy) {
		this.solrServer = solrServer;
		this.idStrategy = idStrategy;
	}
	
	/**
	 * Specifies the extractors that convert the xml elements and 
	 * fields from preceding extractors in the list to new or updated fields.
	 * 
	 * Note that this class was originally implemented with extraction
	 * of attributes and aggregated attributes so no such extractor is needed.
	 * Attributes extraction must be refactored out of the solrj impl if the
	 * aggregation is to be reused without indexing.
	 * 
	 * @param fieldExtraction Extractors, iterable in the order they should be applied
	 *  (guice multibinder binding order becomes iteration order)
	 */
	@Inject
	public void setFieldExtraction(Set<IndexFieldExtraction> fieldExtraction) {
		this.extraction = fieldExtraction;
	}
	
	/**
	 * @param fieldNames optional extra config
	 */
	public void setFieldNames(SchemaFieldNames fieldNames) {
		this.fieldNames = fieldNames;
	}
	
	@Override
	public void startDocument() {
		sent = new LinkedList<String>();
		failed = new LinkedList<String>();
		idStrategy.start();
	}

	@Override
	public void endDocument() {
		logger.debug("Sending remaining {} updates", pending.size());
		updateBatchReady = true;
		batchSend();
		logger.debug("Doing Solr commit");
		try {
			solrServer.commit();
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException("Error not handled", e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException("Error not handled", e);
		}
		if (sent.size() == 0) {
			logger.warn("No elements were indexed");
		} else {
			logger.debug("Sent {} elems to solr, from {} to {}", new Object[]{sent.size(), sent.get(0), sent.get(sent.size() - 1)});
		}
		if (failed.size() > 0) {
			logger.error("Indexing operations failed for ids {}", failed);
		}		
	}
	
	@Override
	public void begin(XmlSourceElement element) {
		String id = idStrategy.getElementId(element);
		IndexFieldsSolrj doc = new IndexFieldsSolrj();
		doc.addField("id", id);
		doc.addField("name", element.getName());
		doc.addField("source", getSource(element));
		for (XmlSourceNamespace n : element.getNamespaces()) {
			doc.addField("ns_" + n.getName(), n.getUri());
		}
		for (XmlSourceAttribute a : element.getAttributes()) {
			doc.addField(fieldNames.getAttribute(a.getName()), a.getValue());
		}
		doc.addField("depth", element.getDepth());
		doc.addField("position", element.getPosition());
		addAncestorData(element, doc);
		XmlSourceElement sp = element.getSiblingPreceding();
		if (sp != null) {
			doc.addField("id_s", idStrategy.getElementId(sp));
			doc.addField("sname", sp.getName());
			for (XmlSourceAttribute a : sp.getAttributes()) {
				doc.addField(fieldNames.getAttributeSiblingPreceding(a.getName()), a.getValue());
			}
		}
		// could be merged with OfflineElementAnalysis
		for (IndexFieldExtraction e : extraction) {
			e.extract(doc, null);
		}
		fieldCleanupBeforeIndexAdd(element, doc);
		add(doc);
	}

	protected void add(IndexFieldsSolrj doc) {
		pending.add(doc);
		batchCheck();
	}

	protected void batchCheck() {
		if (updateBatchReady || pending.size() == UPDATE_BATCH_MAX_SIZE) {
			try {
				batchSend();
			} finally {
				pending.clear();
				updateBatchReady = false;
			}
		}
	}
	
	protected void batchSend() {
		if (pending.size() == 0) {
			logger.warn("Send to solr attempted with empty document list");
			return;
		}
		logger.trace("Sending {} elements to Solr starting with id {}", pending.size(), pending.get(0).getFieldValue("id"));
		try {
			solrServer.add(pending);
		} catch (SolrServerException e) {
			for (SolrInputDocument d : pending) failed.add((String) d.getFieldValue("id")); // legacy logging
			// TODO Auto-generated catch block
			throw new RuntimeException("Error not handled", e);
		} catch (IOException e) {
			for (SolrInputDocument d : pending) failed.add((String) d.getFieldValue("id")); // legacy logging
			// TODO Auto-generated catch block
			throw new RuntimeException("Error not handled", e);
		}
		for (SolrInputDocument d : pending) sent.add((String) d.getFieldValue("id")); // legacy logging
	}
	
	@Deprecated // produces too much logging, is too slow
	protected void send(String id, IndexFieldsSolrj doc) {		
		logger.trace("Sending elem to Solr, id {}, fields {}", id, doc.getFieldNames());
		try {
			solrServer.add(doc);
		} catch (SolrServerException e) {
			failed.add(id);
			// TODO Auto-generated catch block
			throw new RuntimeException("Error not handled", e);
		} catch (IOException e) {
			failed.add(id);
			// TODO Auto-generated catch block
			throw new RuntimeException("Error not handled", e);
		}
		sent.add(id);
	}

	/**
	 * Remove stuff that was needed for extractors but shouldn't be sent to index
	 * for storage or security reasons.
	 * @param element The element that's being indexed
	 * @param doc The document that has been through the extractors chain
	 */	
	protected void fieldCleanupBeforeIndexAdd(XmlSourceElement element,
			IndexFieldsSolrj doc) {
// can be done after we implement use of an original XML stream for supporting translation-of-translation		
//		if (element.isRoot()) {
//			doc.removeField("source");
//		}
			
			// remove fields that we are likely to exclude in the future
			// TODO read ignored fields from schema and skip sending those
			doc.removeField("prop_abx:Dependencies");
			fieldCleanupTemporary(doc);
	}

	/**
	 * To be overridden when testing, so we can still assert that these fields are set.
	 */
	protected void fieldCleanupTemporary(IndexFieldsSolrj doc) {
		// heavy save, until we have element level reuse we only need source of elements with rlogicalid
		if (!doc.containsKey("a_cms:rlogicalid")) {
			doc.removeField("source");
		}
	}

	/**
	 * Storing all source makes the index very large.
	 * @param element
	 * @param doc
	 */
	protected void addSource(XmlSourceElement element, IndexFieldsSolrj doc) {
		if (!element.isRoot()) {
			
		}
	}

	/**
	 * Source is currently stored in index but could be very large xml chunks.
	 * @param element
	 * @return
	 */
	private String getSource(XmlSourceElement element) {
		Reader s = element.getSource();
		StringBuffer b = new StringBuffer();
		int c;
		try {
			while ((c = s.read()) > -1) {
				b.append((char) c);
			}
		} catch (IOException e) {
			throw new RuntimeException("Error reading XML source for indexing", e);
		}
		return b.toString();
	}

	/**
	 * Recursive from the actual element and up to root, aggregating field values.
	 * @param element Initial call with the element from {@link #begin(XmlSourceElement)}
	 * @param doc Field value holder
	 */
	protected void addAncestorData(XmlSourceElement element, IndexFieldsSolrj doc) {
		addAncestorData(element, doc, new StringBuffer());
	}
	
	protected void addAncestorData(XmlSourceElement element, IndexFieldsSolrj doc, StringBuffer pos) {
		boolean isSelf = !doc.containsKey("pname");
		// bottom first
		for (XmlSourceNamespace n : element.getNamespaces()) {
			String f = "ins_" + n.getName();
			if (!doc.containsKey(f)) {
				doc.addField(f, n.getUri());
			}
		}
		for (XmlSourceAttribute a : element.getAttributes()) {
			String f = fieldNames.getAttributeInherited(a.getName());
			if (!doc.containsKey(f)) {
				doc.addField(f, a.getValue());
			}
		}
		// handle root or recurse
		if (element.isRoot()) {
			doc.addField("id_r", idStrategy.getElementId(element));
			doc.addField("rname", element.getName());
			for (XmlSourceAttribute a : element.getAttributes()) {
				doc.addField(fieldNames.getAttributeRoot(a.getName()), a.getValue());
			}
		} else {
			XmlSourceElement parent = element.getParent();
			if (isSelf) {
				doc.addField("id_p", idStrategy.getElementId(parent));
				doc.addField("pname", parent.getName());
			}
			addAncestorData(parent, doc, pos);
		}
		pos.append('.').append(element.getPosition());
		if (isSelf) {
			doc.addField("pos", pos.substring(1));
		} else {
			doc.addField("aname", element.getName());
			doc.addField("id_a", idStrategy.getElementId(element));
		}
	}

}
