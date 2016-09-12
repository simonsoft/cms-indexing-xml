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
import static org.junit.Assume.assumeNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

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
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

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
import se.simonsoft.cms.xmlsource.transform.NullEntityResolver;
import se.simonsoft.cms.xmlsource.transform.TransformerService;
import se.simonsoft.cms.xmlsource.transform.TransformerServiceFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * These tests require test data that is large and non-Open-Source.
 * 
 * The tests will be skipped unless T501007.xml is exported into the dataset single-860k, see scenario-testing.
 * 
 * TODO: The tests should also be executed in scenario-testing. Ensures that the shipped combinations of jars passes and provides consistent checksums.
 */
public class HandlerXmlLargeFileTest {

	private Injector injector;
	private Processor p;
	private ReposTestIndexing indexing;
	private TransformerServiceFactory tf;
	private XmlSourceReaderS9api sourceReader;

	private String classPath = "se/simonsoft/cms/indexing/xml/datasets/";
	
	private long startTime = 0;

	private static HashMap<String, String> tests = new HashMap<String, String>();

	@BeforeClass
	public static void setUpClass() {

		System.out.println("Version Saxon: " + net.sf.saxon.Version.getProductVersion());
		
		// Asserts are executed in hashmap order (out of our control)
		tests.put("p", "c30f06122daa3fde28755ea85f59c14d0d5ac073");
		tests.put("title", "b5aa8764d806e08f75b3face83d742115fad7a05");
		//tests.put("itemlist", "..");
		tests.put("entry", "ae614be4301722538d5efcf071e92f536113996c");
		tests.put("row", "c275cb2a3e59784bce03478d189cc251646374d2");
		tests.put("table", "598b6e604ec60130e91534701fb4694413daca38");

		tests.put("section", "80248e3cf0f8353d952b00fad0e5e79bb0e4050f");
		tests.put("document", "f0ef0ba39cc5a7baedefc5fbaef3e29adb759279");
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
		
		// NOTE: The test will be skipped if T501007.xml is not provided.
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath(classPath.concat("single-860k"));
		assumeResourceExists(repoSource, "/T501007.xml");
		CmsRepositoryFilexml repo = new CmsRepositoryFilexml("http://localtesthost/svn/flir", repoSource);
		FilexmlRepositoryReadonly filexml = new FilexmlRepositoryReadonly(repo);

		indexing.enable(new ReposTestBackendFilexml(filexml), injector);

		SolrServer reposxml = indexing.getCore("reposxml");
		SolrDocumentList all = reposxml.query(new SolrQuery("*:*").setRows(1)/*.addSort("depth", ORDER.asc)*/).getResults();
		assertEquals(11488, all.getNumFound()); // haven't verified this number, got it from first test

		SolrDocument e1 = all.get(0);

		//assertEquals(80, e1.getFieldNames().size());
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
	// Some 700 ms slower in 9.7
	public void testSingle860kReuseNormalize() throws Exception {

		// NOTE: The test will be skipped if T501007.xml is not provided.
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath(classPath.concat("single-860k"));
		assumeResourceExists(repoSource, "/T501007.xml");
				
		InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
				"se/simonsoft/cms/xmlsource/transform/reuse-normalize.xsl");
		Source xslt = new StreamSource(xsl);

		TransformerService t = tf.buildTransformerService(xslt);

		InputStream xml = this.getClass().getClassLoader().getResourceAsStream(
				classPath.concat("single-860k/T501007.xml"));

		XmlSourceDocumentS9api sDoc = sourceReader.read(xml); // This line is failing on build server when dataset resource is missing.

		XmlSourceDocumentS9api rDoc = t.transform(sDoc.getDocumentElement(), null);

		assertChecksums(rDoc);
	}

	private void assertChecksums(XmlSourceDocumentS9api doc) {

		XdmNode root = doc.getDocumentNodeXdm(); // Not sure...
		XPathCompiler xpath = p.newXPathCompiler();
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
	
	
	@Test // Some 300 ms slower in 9.7
	public void testSingle860kIdentity() throws Exception {

		// NOTE: The test will be skipped if T501007.xml is not provided.
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath(classPath.concat("single-860k"));
		assumeResourceExists(repoSource, "/T501007.xml");
				
		InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
				"se/simonsoft/cms/indexing/xml/transform/identity.xsl");
		Source xslt = new StreamSource(xsl);

		TransformerService t = tf.buildTransformerService(xslt);

		InputStream xml = this.getClass().getClassLoader().getResourceAsStream(
				classPath.concat("single-860k/T501007.xml"));

		XmlSourceDocumentS9api sDoc = sourceReader.read(xml); // This line is failing on build server when dataset resource is missing.

		XmlSourceDocumentS9api rDoc = t.transform(sDoc.getDocumentElement(), null);
	}
	
	@Test // Some 300 ms slower in 9.7
	public void testSingle860kIdentityNoFramework() throws Exception {

		// NOTE: The test will be skipped if T501007.xml is not provided.
		FilexmlSourceClasspath repoSource = new FilexmlSourceClasspath(classPath.concat("single-860k"));
		assumeResourceExists(repoSource, "/T501007.xml");
				
		InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
				"se/simonsoft/cms/indexing/xml/transform/identity-strip-space.xsl");
		Source xslt = new StreamSource(xsl);

		Processor p = new Processor(false);
		XsltExecutable e = p.newXsltCompiler().compile(xslt);

		InputStream xml = this.getClass().getClassLoader().getResourceAsStream(
				classPath.concat("single-860k/T501007.xml"));
		
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		XMLReader xmlReader = spf.newSAXParser().getXMLReader();
		xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		xmlReader.setEntityResolver(new NullEntityResolver());

		SAXSource source = new SAXSource(xmlReader, new InputSource(new InputStreamReader(xml)));

		XdmDestination dest = new XdmDestination();
		XsltTransformer t = e.load();
		t.setSource(source);
		t.setDestination(dest);
		t.transform();
	}
	
	/**
	 * Intended to flag that 'T501007.xml' is not provided.
	 * 
	 */
	@Test
	public void testSingle860kDatasetAvailable() {
		
		// TODO Use assume in Before instead?
		String username = System.getProperty("user.name");
		org.junit.Assume.assumeFalse("jenkins".equals(username));
		
		InputStream xml = this.getClass().getClassLoader().getResourceAsStream(
				classPath.concat("single-860k/T501007.xml"));
		
		assertNotNull("The dataset file 'T501007.xml' is required in order to execute all tests.'", xml);

	}

}
