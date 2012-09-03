package se.simonsoft.xmltracking.index.add;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.junit.BeforeClass;
import org.junit.Test;

import se.simonsoft.xmltracking.source.XmlSourceAttribute;
import se.simonsoft.xmltracking.source.XmlSourceElement;

public class XmlSourceHandlerSolrjIntegrationTest extends SolrTestCaseJ4 {
	
	static SolrServer solrServer;
	
	@BeforeClass
	public static void beforeTests() throws Exception {
		initCore("se/simonsoft/cms/indexing/xml/solr/reposxml/conf/solrconfig.xml",
				"se/simonsoft/cms/indexing/xml/solr/reposxml/conf/schema.xml",
				"src/test/resources/se/simonsoft/cms/indexing/xml/solr"); // has to be in classpath because "collection1" is hardcoded in TestHarness initCore/createCore
		solrServer = new EmbeddedSolrServer(h.getCoreContainer(), h.getCore().getName());
	}
	
	@Test
	public void testIntegration() throws MalformedURLException {
		// like the test above but with real server
		
		XmlSourceElement e1 = new XmlSourceElement("document",
				Arrays.asList(new XmlSourceAttribute("cms:status", "In_Work"),
						new XmlSourceAttribute("xml:lang", "en")), 
				"<document cms:status=\"In_Work\" xml:lang=\"en\">\n" +
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
		
		XmlSourceHandlerSolrj handler = new XmlSourceHandlerSolrj(solrServer, idStrategy);

		Set<IndexFieldExtraction> extraction = new HashSet<IndexFieldExtraction>();
		handler.setFieldExtraction(extraction);
		
		handler.startDocument();
		verify(idStrategy).start();
		handler.begin(e1);
		handler.begin(e2);
		handler.begin(e3);
		handler.begin(e4);
		handler.endDocument();
		
		// TODO assertions, currently manual
	}

}
