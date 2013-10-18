package se.simonsoft.cms.indexing.xml;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.solr.client.solrj.SolrServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import se.repos.testing.indexing.ReposTestIndexing;
import se.repos.testing.indexing.TestIndexOptions;
import se.simonsoft.cms.backend.filexml.CmsRepositoryFilexml;
import se.simonsoft.cms.backend.filexml.FilexmlRepositoryReadonly;
import se.simonsoft.cms.backend.filexml.FilexmlSourceClasspath;
import se.simonsoft.cms.backend.filexml.testing.ReposTestBackendFilexml;
import se.simonsoft.cms.indexing.xml.custom.IndexFieldExtractionCustomXsl;
import se.simonsoft.cms.indexing.xml.custom.XmlMatchingFieldExtractionSourceDefault;
import se.simonsoft.cms.indexing.xml.fields.IndexFieldDeletionsToSaveSpace;
import se.simonsoft.cms.indexing.xml.fields.IndexReuseJoinFields;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldElement;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldExtractionChecksum;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexIdAppendTreeLocation;
import se.simonsoft.cms.indexing.xml.solr.XmlIndexWriterSolrj;
import se.simonsoft.cms.xmlsource.handler.XmlSourceReader;
import se.simonsoft.cms.xmlsource.handler.jdom.XmlSourceReaderJdom;

public class HandlerXmlLargeFileTest {

	private ReposTestIndexing indexing = null;

	private long startTime = 0;
	
	/**
	 * Manual dependency injection.
	 */
	@Before
	public void setUpIndexing() {
		TestIndexOptions indexOptions = new TestIndexOptions().itemDefaults();
		indexOptions.addCore("reposxml", "se/simonsoft/cms/indexing/xml/solr/reposxml/**");
		indexing = ReposTestIndexing.getInstance(indexOptions);
		SolrServer reposxml = indexing.getCore("reposxml");
		
		XmlSourceReader xmlReader = new XmlSourceReaderJdom();
		XmlIndexWriter indexWriter = new XmlIndexWriterSolrj(reposxml);
		Set<XmlIndexFieldExtraction> fe = new LinkedHashSet<XmlIndexFieldExtraction>();
		fe.add(new XmlIndexIdAppendTreeLocation());
		fe.add(new XmlIndexFieldElement());
		fe.add(new IndexFieldExtractionCustomXsl(new XmlMatchingFieldExtractionSourceDefault()));
		fe.add(new XmlIndexFieldExtractionChecksum());
		fe.add(new IndexReuseJoinFields());
		fe.add(new IndexFieldDeletionsToSaveSpace());
		
		HandlerXml handlerXml = new HandlerXml();
		handlerXml.setDependenciesIndexing(indexWriter);
		handlerXml.setDependenciesXml(fe, xmlReader);
		
		MarkerXmlCommit commit = new MarkerXmlCommit(reposxml);
		
		indexOptions.addHandler(handlerXml);
		indexOptions.addHandler(commit); // unlike runtime this gets inserted right after handlerXml, another reason to switch to a config Module here
		
		startTime = System.currentTimeMillis();
	}
	
	@After
	public void tearDown() throws IOException {
		long time = System.currentTimeMillis() - startTime;
		System.out.println("Indexing took " + time + " millisecondss");
		
		indexing.tearDown();
	}
	
	@Test
	public void testTinyInline() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/single-860k");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/flir", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));
		
		SolrServer reposxml = indexing.getCore("reposxml");
		
	}

}
