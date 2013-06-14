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
package se.simonsoft.cms.indexing.xml.solr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * Complements our solr cores that comes bundled with jars,
 * so that they can be extracted to arbitrary "solr home" for testing.
 * 
 * The reason we distribute solr cores with the jars is that dependent modules
 * may need them for integration testing etc.
 * 
 * Extracts only a minimal subset of the core, typically for use with solr-test-framework:
 * <pre>
 * testhome = File.createTempFile("test", MyIntegrationTest.getClassName());
 * Core core = new SolrCoreSetup(testhome).getCore("reposxml");
 * SolrTestCaseJ4.initCore(core.getSolrconfig(), core.getSchema(), testhome.getPath(), core.getName());
 * </pre>
 * 
 * Requires Solr 4.3.0+ for any core that isn't named "collection1".
 * 
 * Production environments should get the full core from a source distribution.
 */
public class SolrCoreSetup {

	private File home;

	/**
	 * @param solrhome folder or empty temp file
	 */
	public SolrCoreSetup(File solrhome) {
		this.home = solrhome;
		if (solrhome.isFile() && solrhome.length() == 0) {
			solrhome.delete();
			solrhome.mkdir();
		}
	}
	
	protected void extractClasspathFile(String uri, File destination) {
		InputStream r = this.getClass().getClassLoader().getResourceAsStream(uri);
		if (r == null) {
			throw new IllegalArgumentException("Failed to locate classpath resource " + uri);
		}
		FileOutputStream out;
		try {
			out = new FileOutputStream(destination);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Failed to open resource destination " + destination, e);
		}
		try {
			IOUtils.copy(r, out);
		} catch (IOException e) {
			throw new RuntimeException("Failed to copy resource " + uri + " to " + destination, e);
		}
		try {
			out.close();
		} catch (IOException e) {
			throw new RuntimeException("Failed to close output " + destination, e);
		}
		if (!destination.exists()) {
			throw new RuntimeException("Resource extraction failed for " + destination);
		}
	}
	
	public Core getCore(final String name) {
		if (name == "reposxml") {
			final String classpath = "se/simonsoft/cms/indexing/xml/solr/" + name + "/";
			final File core = new File(home, name);
			core.mkdir();
			File conf = new File(core, "conf");
			conf.mkdir();
			extractClasspathFile(classpath + "conf/stopwords.txt", new File(conf, "stopwords.txt"));
			extractClasspathFile(classpath + "conf/synonyms.txt", new File(conf, "synonyms.txt"));
			return new Core() {
				@Override
				public String getSolrconfig() {
					return classpath + "conf/solrconfig.xml";
				}
				@Override
				public String getSchema() {
					return classpath + "conf/schema.xml";
				}
				@Override
				public File getCore() {
					return core;
				}
				@Override
				public String getName() {
					return name;
				}
			};
		}
		throw new IllegalArgumentException("Unknown core " + name);
	}
	
	public interface Core {
		
		public String getName();
		
		/**
		 * @return classpath uri to solrconfig.xml
		 */
		public String getSolrconfig();
		
		/**
		 * @return classpath uri to schema.xml
		 */
		public String getSchema();
		
		public File getCore();
		
	}
	
}
