package se.simonsoft.xmltracking.index.add;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.xmltracking.index.SchemaFieldNames;
import se.simonsoft.xmltracking.index.SchemaFieldNamesReposxml;
import se.simonsoft.xmltracking.source.XmlSourceAttribute;
import se.simonsoft.xmltracking.source.XmlSourceElement;
import se.simonsoft.xmltracking.source.XmlSourceHandler;

/**
 * Sends each element to Solr directly.
 * 
 * Might be slow compared to indexing that makes batch updates,
 * but on the other hand element source for top level elements is big
 * so batch updates could be impractical.
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
		doc.addField("source", element.getSource());
		for (XmlSourceAttribute a : element.getAttributes()) {
			doc.addField(fieldNames.getAttribute(a.getName()), a.getValue());
		}
		doc.addField("depth", element.getDepth());
		doc.addField("position", element.getPosition());
		addAncestorData(element, doc);
		XmlSourceElement sp = element.getSiblingPreceding();
		if (sp != null) {
			doc.addField("sname", sp.getName());
			for (XmlSourceAttribute a : sp.getAttributes()) {
				doc.addField(fieldNames.getAttributeSiblingPreceding(a.getName()), a.getValue());
			}
		}
		// could be merged with OfflineElementAnalysis
		for (IndexFieldExtraction e : extraction) {
			e.extract(doc, null);
		}
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
	 * Recursive from the actual element and up to root, aggregating field values.
	 * @param element Initial call with the element from {@link #begin(XmlSourceElement)}
	 * @param doc Field value holder
	 */
	protected void addAncestorData(XmlSourceElement element, IndexFieldsSolrj doc) {
		boolean isSelf = !doc.containsKey("pname");
		// bottom first
		for (XmlSourceAttribute a : element.getAttributes()) {
			String f = fieldNames.getAttributeInherited(a.getName());
			if (!doc.containsKey(f)) {
				doc.addField(f, a.getValue());
			}
		}
		// handle root or recurse
		if (element.isRoot()) {
			doc.addField("rname", element.getName());
			for (XmlSourceAttribute a : element.getAttributes()) {
				doc.addField(fieldNames.getAttributeRoot(a.getName()), a.getValue());
			}
		} else {
			XmlSourceElement parent = element.getParent();
			if (isSelf) {
				doc.addField("pname", parent.getName());
			}
			addAncestorData(parent, doc);
		}
		// top first
		if (!isSelf) {
			doc.addField("aname", element.getName());
		}
	}

}
