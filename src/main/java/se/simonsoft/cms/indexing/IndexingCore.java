package se.simonsoft.cms.indexing;

import java.util.Collection;

import org.apache.solr.client.solrj.SolrServer;

/**
 * Allow add to index using our indexing data object {@link IndexItem}.
 */
public interface IndexingCore {

	public void add(Collection<IndexItem> docs);
	
	public void add(IndexItem doc);
	
	public SolrServer getSolr();
	
}
