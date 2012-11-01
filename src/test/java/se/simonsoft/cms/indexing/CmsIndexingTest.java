package se.simonsoft.cms.indexing;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.apache.solr.client.solrj.SolrServer;
import org.junit.After;
import org.junit.Test;

import se.simonsoft.cms.testing.svn.CmsTestRepository;
import se.simonsoft.cms.testing.svn.SvnTestSetup;

public class CmsIndexingTest {

	SvnTestSetup setup = SvnTestSetup.getInstance();
	
	@After
	public void tearDown() {
		setup.tearDown();
	}
	
	@Test
	public void testEmptyFileAndRevprop() {
		InputStream dumpfile = this.getClass().getClassLoader().getResourceAsStream(
				"se/simonsoft/cms/indexing/emptyfileextrarevprop.svndump");
		assertNotNull("Should find dumpfile in test resources", dumpfile);
		CmsTestRepository repo = setup.getRepository("indexingtest").load(dumpfile);
		
		// TODO run embedded sorl
		SolrServer solrItemCore = null;
		
		CmsIndexing indexing = new CmsIndexingSelfConfigured(repo, solrItemCore);
	}

}
