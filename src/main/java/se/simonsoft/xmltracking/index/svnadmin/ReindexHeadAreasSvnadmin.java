package se.simonsoft.xmltracking.index.svnadmin;

import java.io.File;

import org.apache.solr.client.solrj.SolrServer;

import se.simonsoft.xmltracking.index.add.IdStrategy;
import se.simonsoft.xmltracking.index.add.XmlSourceHandlerSolrj;
import se.simonsoft.xmltracking.source.XmlSourceHandler;
import se.simonsoft.xmltracking.source.XmlSourceReader;
import se.simonsoft.xmltracking.source.jdom.XmlSourceReaderJdom;

public class ReindexHeadAreasSvnadmin {

	private File repository;
	private String releasePath;
	private String translationPath;
	
	private IdStrategy idStrategy;
	private XmlSourceReader reader = new XmlSourceReaderJdom();
	private SolrServer server;
	
	public ReindexHeadAreasSvnadmin(File repository, String releasePath, String translationPath) {
		
	}
	
	XmlSourceHandler getSourceHandler() {
		return new XmlSourceHandlerSolrj(server, idStrategy);
	}
	
	public void reindex() {
		
	}
	
	public void reindex(String repositoryPath, XmlSourceHandler handler) {
		
	}
	
}
