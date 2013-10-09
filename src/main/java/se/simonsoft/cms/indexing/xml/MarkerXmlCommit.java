package se.simonsoft.cms.indexing.xml;

import javax.inject.Named;

import org.apache.solr.client.solrj.SolrServer;

import se.repos.indexing.solrj.MarkerCommitSolrj;

public class MarkerXmlCommit extends MarkerCommitSolrj {

	public MarkerXmlCommit(@Named("reposxml") SolrServer core) {
		super(core);
	}
	
}
