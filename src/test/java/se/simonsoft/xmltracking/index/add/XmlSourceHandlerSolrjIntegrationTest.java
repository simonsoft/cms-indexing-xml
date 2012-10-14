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
package se.simonsoft.xmltracking.index.add;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.junit.BeforeClass;
import org.junit.Test;

import se.simonsoft.xmltracking.source.XmlSourceAttribute;
import se.simonsoft.xmltracking.source.XmlSourceElement;
import se.simonsoft.xmltracking.source.XmlSourceNamespace;

public class XmlSourceHandlerSolrjIntegrationTest extends SolrTestCaseJ4 {
	
	static SolrServer server;
	
	@BeforeClass
	public static void beforeTests() throws Exception {
		initCore("se/simonsoft/cms/indexing/xml/solr/reposxml/conf/solrconfig.xml",
				"se/simonsoft/cms/indexing/xml/solr/reposxml/conf/schema.xml",
				"src/test/resources/se/simonsoft/cms/indexing/xml/solr"); // has to be in classpath because "collection1" is hardcoded in TestHarness initCore/createCore
		server = new EmbeddedSolrServer(h.getCoreContainer(), h.getCore().getName());
	}
	
	@Test
	public void testIntegration() throws MalformedURLException, SolrServerException {
		
		XmlSourceElement e1 = new XmlSourceElement("document",
				Arrays.asList(new XmlSourceNamespace("cms", "http://www.simonsoft.se/namespace/cms")),
				Arrays.asList(new XmlSourceAttribute("cms:status", "In_Work"),
						new XmlSourceAttribute("xml:lang", "en")), 
				"<document xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:status=\"In_Work\" xml:lang=\"en\">\n" +
				"<section cms:component=\"xyz\" cms:status=\"Released\">section</section>\n" +
				"<figure cms:component=\"xz0\"><title>Title</title>Figure</figure>\n" +						
				"</document>")
				.setDepth(1, null).setPosition(1, null);
		
		XmlSourceElement e2 = new XmlSourceElement("section",
				Arrays.asList(new XmlSourceAttribute("cms:component", "xyz"),
						new XmlSourceAttribute("cms:status", "Released")),
				"<section cms:component=\"xyz\" cms:status=\"Released\">section</section>")
				.setDepth(2, e1).setPosition(1, null);

		XmlSourceElement e3 = new XmlSourceElement("figure",
				Arrays.asList(new XmlSourceAttribute("cms:component", "xz0")),
				"<figure cms:component=\"xz0\"><title>Title</title>Figure</figure>")
				.setDepth(2, e1).setPosition(2, e2);
		
		XmlSourceElement e4 = new XmlSourceElement("title",
				new LinkedList<XmlSourceAttribute>(),
				"<title>Title</title>")
				.setDepth(3, e3).setPosition(1, null);
		
		IdStrategy idStrategy = mock(IdStrategy.class);
		when(idStrategy.getElementId(e1)).thenReturn("testdoc1_e1");
		when(idStrategy.getElementId(e2)).thenReturn("testdoc1_e2");
		when(idStrategy.getElementId(e3)).thenReturn("testdoc1_e3");
		when(idStrategy.getElementId(e4)).thenReturn("testdoc1_e4");
		
		XmlSourceHandlerSolrj handler = new XmlSourceHandlerSolrj(server, idStrategy);

		Set<IndexFieldExtraction> extraction = new HashSet<IndexFieldExtraction>();
		handler.setFieldExtraction(extraction);
		
		handler.startDocument();
		verify(idStrategy).start();
		handler.begin(e1);
		handler.begin(e2);
		handler.begin(e3);
		handler.begin(e4);
		handler.endDocument();
		
		// We could probably do these assertions by mocking solr server, but it wouldn't be easier
		QueryResponse all = server.query(new SolrQuery("*:*").addSortField("id", ORDER.asc));
		assertEquals(4, all.getResults().getNumFound());
		
		SolrDocument d1 = all.getResults().get(0);
		assertEquals("document", d1.get("name"));
		assertEquals(1, d1.get("position"));
		assertEquals(1, d1.get("depth"));
		assertEquals("In_Work", d1.get("a_cms:status"));
		assertEquals("en", d1.get("a_xml:lang"));
		assertEquals("should index namespaces", "http://www.simonsoft.se/namespace/cms", d1.get("ns_cms"));
		assertEquals("inherited namespaces should contains self", "http://www.simonsoft.se/namespace/cms", d1.get("ins_cms"));
		
		SolrDocument d2 = all.getResults().get(1);
		assertEquals("section", d2.get("name"));
		assertEquals(2, d2.get("depth"));
		assertEquals("document", d2.get("pname"));
		assertEquals("ns is only those defined on the actual element", null, d2.get("ns_cms"));
		assertEquals("inherited namespaces", "http://www.simonsoft.se/namespace/cms", d2.get("ins_cms"));
		
		assertEquals(1, d2.getFieldValues("aname").size());
		assertTrue(d2.getFieldValues("aname").contains("document"));
		assertFalse(d2.getFieldValues("aname").contains("section"));
		
		assertEquals(null, d2.get("a_xml:lang"));
		assertEquals("en", d2.get("ia_xml:lang"));
	}

}
