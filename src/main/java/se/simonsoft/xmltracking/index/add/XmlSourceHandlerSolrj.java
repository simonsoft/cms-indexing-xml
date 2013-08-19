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
import se.simonsoft.xmltracking.source.XmlSourceDoctype;
import se.simonsoft.xmltracking.source.XmlSourceElement;
import se.simonsoft.xmltracking.source.XmlSourceHandler;
import se.simonsoft.xmltracking.source.XmlSourceNamespace;

/**
 * Extracts source fields internally and using {@link IndexFieldExtraction}s (the former being legacy)
 * and sends batches to solr using javabin format.
 * 
 * Does not do solr commit as that is up to the caller, which also does deletion of entries for missing files etc.
 */
public class XmlSourceHandlerSolrj implements XmlSourceHandler {

	private static final Logger logger = LoggerFactory.getLogger(XmlSourceHandlerSolrj.class);
	
	// probably no need to configure
	public static final SchemaFieldNames DEFAULT_FIELD_NAMES = new SchemaFieldNamesReposxml();
	private SchemaFieldNames fieldNames = DEFAULT_FIELD_NAMES;

	private IdStrategy idStrategy;
	private SolrServer solrServer;
	private Set<IndexFieldExtraction> extraction;

	private XmlSourceDoctype doctype;
	
	private transient List<String> sent = null;
	private transient List<String> failed = null;
	
	public static final int BATCH_TEXT_MAX_SIZE = 1000;
	private List<SolrInputDocument> pending = null;
	private boolean batchReady = false; // trigger send before batch max size is reached
	
	/**
	 * The text field was meant for searches on terms, words, values, not even paragraphs.
	 * Thus we start with a very restrictive limit on when we keep the text field values.
	 * We will most likely raise this when we start adding use cases for xml text search.
	 * Text field is currently string, while analyzed text could be kept for much larger chunks. 
	 */
	private static final int TEXT_FIELD_KEEP_LENGTH = 1000;
	
	/**
	 * As element size varies a lot due to source and text indexing we can
	 * try to keep reasonably small batches by also checking total text+source length,
	 * triggering batchReady if above a certain limit instead of waiting for the number of elements.
	 */
	private static final long BATCH_TEXT_TOTAL_MAX = 1000000;
	private long batchTextTotal = 0; // used for optimization in field cleanup
	
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
	public void startDocument(XmlSourceDoctype doctype) {
		this.doctype = doctype;
		sent = new LinkedList<String>();
		failed = new LinkedList<String>();
		idStrategy.start();
		newBatch();
		if (extraction == null) {
			logger.warn("Index extractors list not set. Is indexing properly configured?");
		}
	}

	private void newBatch() {
		pending = new ArrayList<SolrInputDocument>(BATCH_TEXT_MAX_SIZE);
		batchReady = false;
	}

	@Override
	public void endDocument() {
		logger.debug("Sending remaining {} updates at end of document", pending.size());
		batchCheck(true);
		if (sent.size() == 0) {
			logger.warn("No elements were indexed");
		} else {
			logger.debug("Sent {} elems to solr, from {} to {}", new Object[]{sent.size(), sent.get(0), sent.get(sent.size() - 1)});
		}
		if (failed.size() > 0) {
			logger.error("Indexing operations failed for ids {}", failed);
		}		
	}
	
	/**
	 * @return new doc with fields that are the same for all elements for the current document
	 */
	private IndexFieldsSolrj getInitialDoc() {
		IndexFieldsSolrj doc = new IndexFieldsSolrj();
		if (doctype != null) {
			doc.setField("typename", doctype.getElementName());
			doc.setField("typepublic", doctype.getPublicID());
			doc.setField("typesystem", doctype.getSystemID());
		}
		return doc;
	}
	
	@Override
	public void begin(XmlSourceElement element) {
		String id = idStrategy.getElementId(element);
		IndexFieldsSolrj doc = getInitialDoc();
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
		doc.addField("position", element.getLocation().getOrdinal());
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
		if (extraction != null) {
			for (IndexFieldExtraction e : extraction) {
				e.extract(doc, null);
			}
		}
		fieldCleanupBeforeIndexAdd(element, doc);
		add(doc);
	}

	protected void add(IndexFieldsSolrj doc) {
		pending.add(doc);
		batchCheck(false);
	}

	protected void batchCheck(boolean forceSend) {
		if (forceSend || batchReady || pending.size() == BATCH_TEXT_MAX_SIZE) {
			try {
				batchSend();
			} finally {
				// we don't know how the list is handled while sending so clearing it would be unwise
				newBatch();
			}
		}
	}
	
	protected void batchSend() {
		if (pending.size() == 0) {
			logger.warn("Send to solr attempted with empty document list");
			return;
		}
		logger.info("Sending {} elements to Solr starting with id {}", pending.size(), pending.get(0).getFieldValue("id"));
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
		
		// remove fields that we are likely to exclude in the future
		// TODO read ignored fields from schema and skip sending those
		doc.removeField("prop_abx:Dependencies");
		// we index doctype so the properties for that are not needed
		doc.removeField("prop_abx:DoctypeName");
		doc.removeField("prop_abx:PublicId");
		doc.removeField("prop_abx:SystemId");
		// cleanup based on what we know about use cases for now
		fieldCleanupTemporary(doc);
	}

	/**
	 * To be overridden when testing, so we can still assert that these fields are set.
	 */
	protected void fieldCleanupTemporary(IndexFieldsSolrj doc) {
		// heavy save, until we have element level reuse we only need source of elements with rlogicalid
		if (doc.containsKey("source")) {
			if (!doc.containsKey("a_cms:rlogicalid")) {
				doc.removeField("source");
			} else {
				int sourcelen = ((String) doc.getFieldValue("source")).length();
				batchTextTotal += sourcelen;
			}
		}
		
		// Ideally we'd only index text for elements that should contain character data but we have no dtd awareness so we'll guess a max text length for such elements
		// Search for text should ideally hit the element where it is contained, not the parents.
		// We'll remove these fields before first release so that no one starts using them.
		if (doc.containsKey("text")) {
			int textlen = ((String) doc.getFieldValue("text")).length();
			if (textlen > TEXT_FIELD_KEEP_LENGTH) {
				logger.trace("Removing text field size {} from {}", textlen, doc.getFieldValue("id"));
				doc.removeField("text");
			} else {
				batchTextTotal += textlen;
			}
		}
		// we have a rough measurement of total field size here and can trigger batch send to reduce risk of hitting memory limitations in webapp
		if (batchTextTotal > BATCH_TEXT_TOTAL_MAX) {
			logger.info("Sending batch because total source+text size {} indicates large update", batchTextTotal);
			batchReady = true; // send batch
			batchTextTotal = 0;
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
		pos.append('.').append(element.getLocation().getOrdinal());
		if (isSelf) {
			doc.addField("pos", pos.substring(1));
		} else {
			doc.addField("aname", element.getName());
			doc.addField("id_a", idStrategy.getElementId(element));
		}
	}

}
