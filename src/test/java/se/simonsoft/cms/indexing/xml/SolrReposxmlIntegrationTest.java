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
 * Lucene test framework requires "assertions"
 *  - http://docs.oracle.com/javase/1.4.2/docs/guide/lang/assert.html#enable-disable
 *  - http://java.sun.com/developer/technicalArticles/JavaLP/assertions/
 *  - http://stackoverflow.com/questions/5509082/eclipse-enable-assertions
 *  - http://maven.apache.org/plugins/maven-surefire-plugin/test-mojo.html#enableAssertions
 *  - Eclipse > Preferences > Java > Junit > Append -ea to JVM arguments ...
 */
public class SolrReposxmlIntegrationTest extends SolrTestCaseJ4 {

	static {
		System.setProperty("solr.solr.home", "./src/main/solr");
	}

	@BeforeClass
	public static void beforeTests() throws Exception {
		SolrTestCaseJ4.initCore("reposxml/conf/solrconfig.xml", "reposxml/conf/schema.xml", "reposxml");
	}

	@Test
	public void testIndexEmptyFromStart() {
		assertQ("test query on empty index", req("*:*"), "//result[@numFound='0']");
	}

}
