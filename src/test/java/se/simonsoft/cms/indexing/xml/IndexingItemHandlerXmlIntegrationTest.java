package se.simonsoft.cms.indexing.xml;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrDocumentList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCopySource;
import org.tmatesoft.svn.core.wc.SVNRevision;

import se.repos.testing.indexing.SvnTestIndexing;
import se.repos.testing.indexing.TestIndexOptions;
import se.simonsoft.cms.indexing.xml.fields.IndexFieldDeletionsToSaveSpace;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldElement;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexFieldExtractionChecksum;
import se.simonsoft.cms.indexing.xml.fields.XmlIndexIdAppendTreeLocation;
import se.simonsoft.cms.indexing.xml.solr.XmlIndexWriterSolrj;
import se.simonsoft.cms.testing.svn.CmsTestRepository;
import se.simonsoft.cms.testing.svn.SvnTestSetup;
import se.simonsoft.xmltracking.source.XmlSourceReader;
import se.simonsoft.xmltracking.source.jdom.XmlSourceReaderJdom;
import se.simonsoft.xmltracking.source.saxon.IndexFieldExtractionCustomXsl;
import se.simonsoft.xmltracking.source.saxon.XmlMatchingFieldExtractionSource;

public class IndexingItemHandlerXmlIntegrationTest {

	/// test framework from cms-backend-svnkit
	
	private File wc = null;
	
	private SVNClientManager clientManager = null;
	
	private CmsTestRepository repo = null;
	
	private SvnTestSetup setup = SvnTestSetup.getInstance();
	private SvnTestIndexing indexing = null;
	
	@Before
	public void setUp() throws IOException, SVNException {
		repo = setup.getRepository();
		wc = new File(repo.getAdminPath(), "wc"); // inside repository, same cleanup

		clientManager = SVNClientManager.newInstance();
		
		setUpIndexing(repo);
	}

	private void setUpIndexing(CmsTestRepository repo) {
		TestIndexOptions indexOptions = new TestIndexOptions().itemDefaults();
		indexOptions.addCore("reposxml", "se/simonsoft/cms/indexing/xml/solr/reposxml/**");
		indexing = SvnTestIndexing.getInstance(indexOptions);
		SolrServer reposxml = indexing.getCore("reposxml");
		
		XmlSourceReader xmlReader = new XmlSourceReaderJdom();
		XmlIndexWriter indexWriter = new XmlIndexWriterSolrj(reposxml);
		Set<XmlIndexFieldExtraction> fe = new LinkedHashSet<XmlIndexFieldExtraction>();
		fe.add(new XmlIndexIdAppendTreeLocation());
		fe.add(new XmlIndexFieldElement());
		XmlMatchingFieldExtractionSource xslSource = new XmlMatchingFieldExtractionSource() {
			@Override
			public Source getXslt() {
				InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
						"se/simonsoft/cms/indexing/xml/source/xml-indexing-fields.xsl");
				return new StreamSource(xsl);
			}
		};
		fe.add(new IndexFieldExtractionCustomXsl(xslSource));
		fe.add(new XmlIndexFieldExtractionChecksum());
		fe.add(new IndexFieldDeletionsToSaveSpace());
		
		IndexingItemHandlerXml handlerXml = new IndexingItemHandlerXml();
		handlerXml.setDependenciesIndexing(indexWriter);
		handlerXml.setDependenciesXml(fe, xmlReader);
		
		indexOptions.addHandler(handlerXml);
	}
	
	@After
	public void tearDown() throws IOException {
		setup.tearDown();
		indexing.tearDown();
	}
	
	// test helpers from cms-backend-svnkit, using the file URL, might need porting to use http for hooks to get executed properly
	
	private void svncheckout() throws SVNException {
		clientManager.getUpdateClient().doCheckout(repo.getUrlSvnkit(), wc, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, false);
	}
	
	private void svnupdate() throws SVNException {
		clientManager.getUpdateClient().doUpdate(wc, SVNRevision.HEAD, SVNDepth.INFINITY, false, true);
	}
	
	private long svncommit(String comment) throws SVNException {
		return clientManager.getCommitClient().doCommit(
				new File[]{wc}, false, comment, null, null, false, false, SVNDepth.INFINITY).getNewRevision();
	}
	
	private long svncommit() throws SVNException {
		return svncommit("");
	}
	
	private void svnpropset(File path, String propname, String propval) throws SVNException {
		clientManager.getWCClient().doSetProperty(path, propname, SVNPropertyValue.create(propval), false, SVNDepth.EMPTY, null, null);
	}

	private void svnadd(File... paths) throws SVNException {
		clientManager.getWCClient().doAdd(
				paths, true, false, false, SVNDepth.INFINITY, true, true, true);
	}
	
	private void svnrm(File path) throws SVNException {
		if (!path.exists()) {
			throw new IllegalArgumentException("For tests the deletion target must exist");
		}
		clientManager.getWCClient().doDelete(path, false, true, false);
	}
	
	private void svncopy(File from, File to, boolean deleteOriginal) throws SVNException {
		svncopy(from, SVNRevision.HEAD, to, deleteOriginal);
	}
	
	private void svncopy(File from, SVNRevision fromRevision, File to, boolean deleteOriginal) throws SVNException {
		SVNCopySource source = new SVNCopySource(fromRevision, fromRevision, from);
		clientManager.getCopyClient().doCopy(new SVNCopySource[]{source}, to, deleteOriginal, false, true);
	}
	
	private void svncopy(File from, File to) throws SVNException {
		svncopy(from, to, false);
	}

	private void svncopy(File from, long fromRevision, File to) throws SVNException {
		svncopy(from, SVNRevision.create(fromRevision), to, false);
	}
	
	private void svnmove(File from, File to) throws SVNException {
		//svncopy(from, to, true);
		clientManager.getMoveClient().doMove(from, to);
	}	
	
	@Test
	public void test() throws Exception {

		svncheckout();

		// first produce some revisions so we see that isOverwritten handling works
		File f1 = new File(wc, "test1.xml");
		FileUtils.writeStringToFile(f1, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<doc><elem>text</elem><elem id=\"e2\">elem <inline>text</inline></elem></doc>");
		svnadd(f1);
		svncommit();
		
		// now sync
		indexing.enable(repo);
		
		SolrServer reposxml = indexing.getCore("reposxml");
		SolrDocumentList x1 = reposxml.query(new SolrQuery("*:*")).getResults();
		assertEquals(4, x1.getNumFound());
	
		// now produce more revisions
		
		
	}

}
