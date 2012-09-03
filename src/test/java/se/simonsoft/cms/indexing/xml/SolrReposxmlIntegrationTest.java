package se.simonsoft.cms.indexing.xml;

import static org.junit.Assert.*;

import org.apache.solr.SolrTestCaseJ4;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * 
 * http://blog.florian-hopf.de/2012/06/running-and-testing-solr-with-gradle.html
 * 
 * 
 *
 */
public class SolrReposxmlIntegrationTest {

	static {
		System.setProperty("solr.solr.home", "./src/main/solr");
	}
	
	@BeforeClass
	public static void beforeTests() throws Exception {
		System.out.println("hello");
		SolrTestCaseJ4.initCore("reposxml/conf/solrconfig.xml", "reposxml/conf/schema.xml", "reposxml/");
	}
	
	@Test
	public void testSomething() {
		System.out.println("here");
		fail("hepp");
	}

}
