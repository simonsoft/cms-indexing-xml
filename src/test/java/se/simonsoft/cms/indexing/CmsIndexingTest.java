/**
 * Copyright (C) 2009-2012 Simonsoft Nordic AB
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
	public void testAPIWebappPostCommitHook() {
		// empty repository
		// run hook twice
		// changeset should be shared with other post commit handlers, but indexing could also need access to earlier changesets
	}
	
	@Test
	public void testAPIResyncPreconfigured() {
		// configure indexing with solr instance/provider for repositem
		// call with repository
	}
	
	@Test
	public void testAPIResyncWithPlugins() {
		// plugins that add or edit fields in repositem
		//  - core is basic element indexing, browseable data, before returning
		//  - metadata and fulltext extraction is such a plugin, running after return
		// plugins that index to different cores by cloning or just listening to repositem docs, including history/rev docs
	}
	
	@Test
	public void testAPICommandLine() {
		String repository = "http://testhost/svn/repo1";
		String solr = "http://testhost/solr"; // indexing must assume the cores exist
		// additional arguments could
		// fail("TODO call a main method and expect resync repositem as default operation");
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
