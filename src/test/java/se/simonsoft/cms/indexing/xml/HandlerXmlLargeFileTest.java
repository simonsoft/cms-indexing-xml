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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import se.repos.testing.indexing.ReposTestIndexing;
import se.repos.testing.indexing.TestIndexOptions;
import se.simonsoft.cms.backend.filexml.CmsRepositoryFilexml;
import se.simonsoft.cms.backend.filexml.FilexmlRepositoryReadonly;
import se.simonsoft.cms.backend.filexml.FilexmlSource;
import se.simonsoft.cms.backend.filexml.FilexmlSourceClasspath;
import se.simonsoft.cms.backend.filexml.testing.ReposTestBackendFilexml;
import se.simonsoft.cms.indexing.xml.testconfig.IndexingConfigXmlBase;
import se.simonsoft.cms.indexing.xml.testconfig.IndexingConfigXmlDefault;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceDocumentS9api;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceReaderS9api;
import se.simonsoft.cms.xmlsource.transform.TransformerService;
import se.simonsoft.cms.xmlsource.transform.TransformerServiceFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class HandlerXmlLargeFileTest {

	private Injector injector;
	private Processor p;
	private ReposTestIndexing indexing;
	private TransformerServiceFactory tf;
	private XmlSourceReaderS9api sourceReader;

	private long startTime = 0;

	private static HashMap<String, String> tests = new HashMap<String, String>();

	@BeforeClass
	public static void setUpClass() {

		// Asserts are executed in hashmap order (out of our control)
		tests.put("p", "c30f06122daa3fde28755ea85f59c14d0d5ac073");
		tests.put("title", "b5aa8764d806e08f75b3face83d742115fad7a05");
		//tests.put("itemlist", "..");
		tests.put("entry", "ae614be4301722538d5efcf071e92f536113996c");
		tests.put("row", "c275cb2a3e59784bce03478d189cc251646374d2");
		tests.put("table", "598b6e604ec60130e91534701fb4694413daca38");

		tests.put("section", "7c8faaeac624d13b9ea17b65353c5e823cc2bdd3");
		tests.put("document", "a90bbfb8ea5aaac6b406959f9d998903c1722106");
	}

	@Before
	public void setUp() {

	}

	/**
	 * Manual dependency injection.
	 */
	@Before
	public void setUpIndexing() {
		startTime = System.currentTimeMillis();

		injector = Guice.createInjector(new IndexingConfigXmlBase());

		TestIndexOptions indexOptions = new TestIndexOptions().itemDefaultServices()
				.addCore("reposxml", "se/simonsoft/cms/indexing/xml/solr/reposxml/**")
				.addModule(new IndexingConfigXmlDefault());
		indexing = ReposTestIndexing.getInstance(indexOptions);

		p = injector.getInstance(Processor.class);
		sourceReader = new XmlSourceReaderS9api(p);
		tf = new TransformerServiceFactory(p, sourceReader);
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

		indexing.enable(new ReposTestBackendFilexml(filexml), injector);

		SolrServer reposxml = indexing.getCore("reposxml");
		SolrDocumentList all = reposxml.query(new SolrQuery("*:*").setRows(1)/*.addSort("depth", ORDER.asc)*/).getResults();
		assertEquals(11488, all.getNumFound()); // haven't verified this number, got it from first test

		SolrDocument e1 = all.get(0);

		assertEquals(80, e1.getFieldNames().size());
		//assertEquals("...", e1.getFieldValue("pathname"));
		/* Can not assert on props since repositem is not involved.
		assertEquals("xml", e1.getFieldValue("prop_abx.ContentType"));
		assertNull(e1.getFieldValue("prop_abx.Dependencies"));
		*/

		assertChecksums(reposxml);
	}

	private void assertChecksums(SolrServer reposxml) {

		// We are comparing checksum calculation in Indexing (for object itself) with XSL filter. 
		String FIELDNAME = "c_sha1_source_reuse";

		try {
			for (Entry<String, String> t : tests.entrySet()) {

				SolrDocumentList e;

				String q = "name:" + t.getKey(); // Query for first element with current tagname.
				e = reposxml.query(new SolrQuery(q).setRows(1).addSort("pos", ORDER.asc)).getResults();

				assertEquals("checksum for first " + t.getKey(), t.getValue(), e.get(0).getFieldValue(FIELDNAME));
			}
		} catch (SolrServerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	@Test
	public void testSingle860kReuseNormalize() throws Exception {

		InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
				"se/simonsoft/cms/xmlsource/transform/reuse-normalize.xsl");
		Source xslt = new StreamSource(xsl);

		TransformerService t = tf.buildTransformerService(xslt);

		InputStream xml = this.getClass().getClassLoader().getResourceAsStream(
				"se/simonsoft/cms/indexing/xml/datasets/single-860k/T501007.xml");

		XmlSourceDocumentS9api sDoc = sourceReader.read(xml);

		XmlSourceDocumentS9api rDoc = t.transform(sDoc.getDocumentElement(), null);

		assertChecksums(rDoc);
	}

	private void assertChecksums(XmlSourceDocumentS9api doc) {

		XdmNode root = doc.getDocumentNodeXdm(); // Not sure...
		XPathCompiler xpath = root.getProcessor().newXPathCompiler(); // Getting exception here, one that Saxon author did not expect to ever happen.
		xpath.declareNamespace("cms", "http://www.simonsoft.se/namespace/cms");

		String ATTRNAME = "cms:c_sha1_source_reuse";

		try {
			XPathExecutable xe1 = xpath.compile("/document/@docno");
			XPathSelector xs1 = xe1.load();
			xs1.setContextItem(root);
			XdmValue r1 = xs1.evaluate();
			assertEquals("Basic test of XPath", "docno=\"T559600\"", r1.toString());

			for (Entry<String, String> t : tests.entrySet()) {
				XPathExecutable xe = xpath.compile("//" + t.getKey() + "[1]" + "/@" + ATTRNAME);
				XPathSelector xs = xe.load();
				xs.setContextItem(root);
				XdmItem xr = xs.evaluateSingle();
				assertEquals("checksum for first " + t.getKey(), t.getValue(), xr.getStringValue());
			}

		} catch (SaxonApiException e) {

			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
