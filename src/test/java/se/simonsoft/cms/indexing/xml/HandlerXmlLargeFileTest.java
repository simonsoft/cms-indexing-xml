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

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;

import se.repos.testing.indexing.ReposTestIndexing;
import se.repos.testing.indexing.TestIndexOptions;
import se.simonsoft.cms.backend.filexml.CmsRepositoryFilexml;
import se.simonsoft.cms.backend.filexml.FilexmlRepositoryReadonly;
import se.simonsoft.cms.backend.filexml.FilexmlSourceClasspath;
import se.simonsoft.cms.backend.filexml.testing.ReposTestBackendFilexml;
import se.simonsoft.cms.indexing.xml.testconfig.IndexingConfigXml;

public class HandlerXmlLargeFileTest {

	private long startTime = 0;
	
	/**
	 * Manual dependency injection.
	 */
	@Before
	public void setUpIndexing() {
		startTime = System.currentTimeMillis();
	}
	
	@After
	public void tearDown() throws IOException {
		long time = System.currentTimeMillis() - startTime;
		System.out.println("Test took " + time + " millisecondss");
		
		ReposTestIndexing.getInstance().tearDown();
	}
	
	@Test
	public void testSingle860k() throws Exception {
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath("se/simonsoft/cms/indexing/xml/datasets/single-860k");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/flir", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);
		
		// set up repos-testing so we can get a SolrServer instance for reposxml
		TestIndexOptions indexOptions = new TestIndexOptions().itemDefaults();
		indexOptions.addCore("reposxml", "se/simonsoft/cms/indexing/xml/solr/reposxml/**");
                ReposTestIndexing indexing = ReposTestIndexing.getInstance(indexOptions);
		final SolrServer reposxml = indexing.getCore("reposxml");
                
        // with the SolrServer instance, set up XML indexing context so we can add the XML handler to indexing before we actually index the test backend
		Module configTesting = new AbstractModule() { @Override protected void configure() {
			bind(SolrServer.class).annotatedWith(Names.named("reposxml")).toInstance(reposxml);
		}};	
		Injector context = Guice.createInjector(configTesting, new IndexingConfigXml());
		indexOptions.addHandler(context.getInstance(HandlerXml.class));
		indexOptions.addHandler(context.getInstance(MarkerXmlCommit.class)); // unlike runtime this gets inserted right after handlerXml, another reason to switch to a config Module here
		
		// enable repos-testing, enable hooks and build a context that includes backend services
		ReposTestBackendFilexml testBackend = new ReposTestBackendFilexml(filexml);
		indexing.enable(testBackend, context);

		reposxml.commit(); // TODO why doesn't this happen in the handler chain?
	}

}
