/**
 * Copyright (C) 2009-2013 Simonsoft Nordic AB
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
package se.simonsoft.cms.indexing.xml;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrDocumentList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import se.repos.testing.indexing.ReposTestIndexing;
import se.repos.testing.indexing.TestIndexOptions;
import se.simonsoft.cms.backend.filexml.CmsRepositoryFilexml;
import se.simonsoft.cms.backend.filexml.FilexmlRepositoryReadonly;
import se.simonsoft.cms.backend.filexml.FilexmlSource;
import se.simonsoft.cms.backend.filexml.FilexmlSourceClasspath;
import se.simonsoft.cms.backend.filexml.testing.ReposTestBackendFilexml;
import se.simonsoft.cms.indexing.xml.testconfig.IndexingConfigXml;
import se.simonsoft.cms.item.CmsItemPath;

public class HandlerXmlLargeFileTest {

	private ReposTestIndexing indexing;
	
	private long startTime = 0;
	
	/**
	 * Manual dependency injection.
	 */
	@Before
	public void setUpIndexing() {
		startTime = System.currentTimeMillis();
		
		TestIndexOptions indexOptions = new TestIndexOptions().itemDefaultServices()
				.addCore("reposxml", "se/simonsoft/cms/indexing/xml/solr/reposxml/**")
				.addModule(new IndexingConfigXml());
		indexing = ReposTestIndexing.getInstance(indexOptions);
	}
	
	@After
	public void tearDown() throws IOException {
		long time = System.currentTimeMillis() - startTime;
		System.out.println("Test took " + time + " millisecondss");
		
		ReposTestIndexing.getInstance().tearDown();
	}
	
	// filexml backend could expose a https://github.com/hamcrest/JavaHamcrest matcher
	protected void assumeResourceExists(FilexmlSource source, String cmsItemPath) {
		assumeNotNull("Test skipped until large file " + cmsItemPath + " is exported",
				source.getFile(new CmsItemPath(cmsItemPath)));
	}
	
	@Test
	public void testSingle860k() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/single-860k");
		assumeResourceExists(repoSource, "/T501007.xml");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/flir", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		indexing.enable(new ReposTestBackendFilexml(filexml));

		SolrServer reposxml = indexing.getCore("reposxml");
		SolrDocumentList all = reposxml.query(new SolrQuery("*:*").setRows(1)).getResults();
		assertEquals(11488, all.getNumFound()); // haven't verified this number, got it from first test
	}

}
