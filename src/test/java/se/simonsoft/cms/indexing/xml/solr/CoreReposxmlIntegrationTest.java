/**
 * Copyright (C) 2009-2016 Simonsoft Nordic AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.simonsoft.cms.indexing.xml.solr;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import se.simonsoft.cms.indexing.xml.solr.SolrCoreSetup.Core;

/**
 * Verify features of the actual index, without using our abstractions.
 * This test should match stuff that we rely on for direct queries to solr from various places.
 * Create new test methods per integration case.
 * 
 * Using solr test-framework instead of http://wiki.apache.org/solr/Solrj#EmbeddedSolrServer-1
 * to get their temp file management etc.
 * 
 * http://lucene.apache.org/solr/api-4_0_0-BETA/test-framework/index.html?org/apache/solr/SolrTestCaseJ4.html
 * http://blog.florian-hopf.de/2012/06/running-and-testing-solr-with-gradle.html
 * 
 * Lucene test framework requires "assertions"
 *  - http://docs.oracle.com/javase/1.4.2/docs/guide/lang/assert.html#enable-disable
 *  - http://java.sun.com/developer/technicalArticles/JavaLP/assertions/
 *  - http://stackoverflow.com/questions/5509082/eclipse-enable-assertions
 *  - http://maven.apache.org/plugins/maven-surefire-plugin/test-mojo.html#enableAssertions
 *  - Eclipse > Preferences > Java > Junit > Append -ea to JVM arguments ...
 */
public class CoreReposxmlIntegrationTest extends SolrTestCaseJ4 {

	public static File testhome = null;

	@BeforeClass
	public static void beforeTests() throws Exception {
		testhome = File.createTempFile("test", CoreReposxmlIntegrationTest.getClassName());
		Core core = new SolrCoreSetup(testhome).getCore("reposxml");
		try {
			SolrTestCaseJ4.initCore(core.getSolrconfig(), core.getSchema(), testhome.getPath(), core.getName());
		} catch (Exception e) {
			System.out.println("getSolrConfigFile()=" + getSolrConfigFile());
			System.out.println("testSolrHome=" + testSolrHome);
			throw e;
		}
	}
	
	@AfterClass
	public static void afterTests() throws Exception {
		FileUtils.deleteDirectory(testhome);
	}
	
	/**
	 * @return instance for injection when integration testing our logic with solr, for index testing we do fine with SolrTestCaseJ4 helper methods
	 */
	public SolrClient getTestServer() {
		return new EmbeddedSolrServer(h.getCoreContainer(), h.getCore().getName()); // cache between tests?
	}
	
	@Test
	public void testCommon() throws SolrServerException, IOException {
		// verify test setup
		assertQ("index should be empty on each test run", req("*:*"), "//result[@numFound='0']");
		// reference use of solr api
		SolrClient solr = getTestServer();
		SolrInputDocument doc1 = new SolrInputDocument();
		doc1.addField("id", "x");
		doc1.addField("name", "x");
		doc1.addField("pos", "1");
		solr.add(doc1);
		solr.commit();
		QueryResponse query = solr.query(new SolrQuery("*:*"));
		assertEquals(1, query.getResults().getNumFound());
		// verify test setup
		assertQ("index should be empty on each test run", req("*:*"), "//result[@numFound='1']");
	}

}
