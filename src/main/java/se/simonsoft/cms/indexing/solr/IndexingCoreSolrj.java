package se.simonsoft.cms.indexing.solr;

import java.util.Collection;

import org.apache.solr.client.solrj.SolrServer;

import se.simonsoft.cms.indexing.IndexItem;
import se.simonsoft.cms.indexing.IndexItemFollower;
import se.simonsoft.cms.indexing.IndexingCore;

/**
 * 
 * 
 * Ensure thread safety for additions,
 * coordinate additions from different {@link IndexItemFollower}s,
 * prioritize additions.
 */
public class IndexingCoreSolrj implements IndexingCore {

	private SolrServer solr;

	public IndexingCoreSolrj(SolrServer solrCore) {
		this.solr = solrCore;
	}
	
	@Override
	public void add(Collection<IndexItem> docs) {
		
	}

	@Override
	public void add(IndexItem doc) {
		
	}

	@Override
	public SolrServer getSolr() {
		return solr;
	}

}
