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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
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
	public void testIntegration() throws Exception {
		
		XmlSourceElement e1 = new XmlSourceElement("document",
				Arrays.asList(new XmlSourceNamespace("cms", "http://www.simonsoft.se/namespace/cms")),
				Arrays.asList(new XmlSourceAttribute("cms:status", "In_Work"),
						new XmlSourceAttribute("xml:lang", "en")), 
				"<document xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" cms:status=\"In_Work\" xml:lang=\"en\">\n" +
				"<section cms:component=\"xyz\" cms:status=\"Released\">section</section>\n" +
				"<figure cms:component=\"xz0\"><title>Title</title><byline>me</byline></figure>\n" +						
				"</document>")
				.setDepth(1, null).setPosition(1, null);
		
		XmlSourceElement e2 = new XmlSourceElement("section",
				Arrays.asList(new XmlSourceAttribute("cms:component", "xyz"),
						new XmlSourceAttribute("cms:status", "Released")),
				"<section cms:component=\"xyz\" cms:status=\"Released\">section</section>")
				.setDepth(2, e1).setPosition(1, null);

		XmlSourceElement e3 = new XmlSourceElement("figure",
				Arrays.asList(new XmlSourceAttribute("cms:component", "xz0")),
				"<figure cms:component=\"xz0\"><title>Title</title><byline>me</byline></figure>")
				.setDepth(2, e1).setPosition(2, e2);
		
		XmlSourceElement e4 = new XmlSourceElement("title",
				new LinkedList<XmlSourceAttribute>(),
				"<title>Title</title>")
				.setDepth(3, e3).setPosition(1, null);

		XmlSourceElement e5 = new XmlSourceElement("byline",
				new LinkedList<XmlSourceAttribute>(),
				"<byline>me</byline>")
				.setDepth(3, e3).setPosition(2, e4);		
		
		IdStrategy idStrategy = mock(IdStrategy.class);
		when(idStrategy.getElementId(e1)).thenReturn("testdoc1_e1");
		when(idStrategy.getElementId(e2)).thenReturn("testdoc1_e2");
		when(idStrategy.getElementId(e3)).thenReturn("testdoc1_e3");
		when(idStrategy.getElementId(e4)).thenReturn("testdoc1_e4");
		when(idStrategy.getElementId(e5)).thenReturn("testdoc1_e5");
		
		XmlSourceHandlerSolrj handler = new XmlSourceHandlerSolrj(server, idStrategy);

		Set<IndexFieldExtraction> extraction = new HashSet<IndexFieldExtraction>();
		handler.setFieldExtraction(extraction);
		
		handler.startDocument();
		verify(idStrategy).start();
		handler.begin(e1);
		handler.begin(e2);
		handler.begin(e3);
		handler.begin(e4);
		handler.begin(e5);
		handler.endDocument();
		server.commit();
		
		// We could probably do these assertions by mocking solr server, but it wouldn't be easier
		QueryResponse all = server.query(new SolrQuery("*:*").addSortField("id", ORDER.asc));
		assertEquals(5, all.getResults().getNumFound());
		
		SolrDocument d1 = all.getResults().get(0);
		assertEquals("should get id from IdStrategy", "testdoc1_e1", d1.get("id"));
		assertEquals("document", d1.get("name"));
		assertEquals(1, d1.get("position"));
		assertEquals(1, d1.get("depth"));
		assertEquals(null, d1.get("id_p"));
		assertEquals(null, d1.get("id_s"));
		assertEquals(d1.get("id"), d1.get("id_r"));
		assertEquals(null, d1.getFieldValues("id_a"));
		assertEquals("In_Work", d1.get("a_cms:status"));
		assertEquals("en", d1.get("a_xml:lang"));
		assertEquals("should index namespaces", "http://www.simonsoft.se/namespace/cms", d1.get("ns_cms"));
		assertEquals("inherited namespaces should contains self", "http://www.simonsoft.se/namespace/cms", d1.get("ins_cms"));
		assertEquals("root", "1", d1.get("pos"));
		
		SolrDocument d2 = all.getResults().get(1);
		assertEquals("section", d2.get("name"));
		assertEquals(2, d2.get("depth"));
		assertEquals(d1.get("id"), d2.get("id_p"));
		assertEquals(null, d2.get("id_s"));
		assertEquals(d1.get("id"), d2.get("id_r"));
		assertEquals("document", d2.get("pname"));
		assertEquals("ns is only those defined on the actual element", null, d2.get("ns_cms"));
		assertEquals("inherited namespaces", "http://www.simonsoft.se/namespace/cms", d2.get("ins_cms"));
		assertEquals("1.1", d2.get("pos"));
		
		assertEquals(1, d2.getFieldValues("aname").size());
		assertTrue(d2.getFieldValues("aname").contains("document"));
		assertFalse(d2.getFieldValues("aname").contains("section"));
		assertEquals(1, d2.getFieldValues("id_a").size());
		
		assertEquals(null, d2.get("a_xml:lang"));
		assertEquals("en", d2.get("ia_xml:lang"));
		
		SolrDocument d3 = all.getResults().get(2);
		assertEquals(2, d3.get("position"));
		assertEquals("1.2", d3.get("pos"));
		assertEquals(d1.get("id"), d3.get("id_p"));
		assertEquals(d2.get("id"), d3.get("id_s"));
		assertEquals(d1.get("id"), d3.get("id_r"));
		assertEquals(1, d3.getFieldValues("id_a").size());
		assertTrue(d3.getFieldValues("id_a").contains(d1.get("id")));
		assertEquals("xz0", d3.get("a_cms:component"));
		
		SolrDocument d4 = all.getResults().get(3);
		assertEquals("1.2.1", d4.get("pos"));
		assertEquals(d3.get("id"), d4.get("id_p"));
		assertEquals(null, d4.get("id_s"));
		assertEquals(d1.get("id"), d4.get("id_r"));
		assertEquals(2, d4.getFieldValues("id_a").size());
		assertTrue(d4.getFieldValues("id_a").contains(d1.get("id")));
		assertTrue(d4.getFieldValues("id_a").contains(d3.get("id")));
		
		SolrDocument d5 = all.getResults().get(4);
		assertEquals("1.2.2", d5.get("pos"));
		
		// now that we have the data in a test index, test some other queries
		reuseDataTestJoin();
	}
	
	void reuseDataTestJoin() throws Exception {
		
		// find all elements that can be joined with a parent
		assertJQ(req("q", "{!join from=id to=id_p}*:*", "fl", "id"),
				"/response=={'numFound':4,'start':0,'docs':[{'id':'testdoc1_e2'},{'id':'testdoc1_e3'},{'id':'testdoc1_e4'},{'id':'testdoc1_e5'}]}");

		// find all elements that have a child
		assertJQ(req("q", "{!join from=id_p to=id}*:*", "fl", "id"),
				"/response=={'numFound':2,'start':0,'docs':[{'id':'testdoc1_e1'},{'id':'testdoc1_e3'}]}");

		// find all elements that have a child that is a <title/>
		assertJQ(req("q", "{!join from=id_p to=id}name:title", "fl", "id"),
				"/response=={'numFound':1,'start':0,'docs':[{'id':'testdoc1_e3'}]}");		
		
		// find all children of xz0 component
		assertJQ(req("q", "{!join from=id to=id_p}a_cms\\:component:xz0", "fl", "name"),
				"/response=={'numFound':2,'start':0,'docs':[{'name':'title'}, {'name':'byline'}]}");
		
		// find all figures with a bylinew with value "me"
		assertJQ(req("q", "{!join from=id_p to=id v=$qq}",
					"qq", "name:byline AND pos:1.2.2", // we don't have text indexed in this test so we use pos instead
					//"qf", "name",
					"fl", "id",
					"debugQuery", "true"),
				"/response=={'numFound':1,'start':0,'docs':[{'id':'testdoc1_e3'}]}");
		
		// find all elements that contain a title (recursively)
		assertJQ(req("q", "{!join from=id_a to=id}name:title", "fl", "id"),
				"/response=={'numFound':2,'start':0,'docs':[{'id':'testdoc1_e1'},{'id':'testdoc1_e3'}]}");
		
	}

}
