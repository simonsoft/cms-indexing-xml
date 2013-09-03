package se.simonsoft.cms.indexing.abx;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collection;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.item.CmsRepository;

public class IndexingItemHandlerAbxDependenciesTest {

	@Test
	public void test() {
		String abxdeps = "x-svn:///svn/documentation^/graphics/cms/process/2.0/op-edit.png\n" +
				"x-svn:///svn/documentation^/xml/reference/cms/adapter/Introduction%20to%20CMS.xml\n" +
				"x-svn:///svn/documentation^/xml/reference/cms/User%20interface.xml?p=123";
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		when(p.getFields()).thenReturn(doc);
		when(p.getRepository()).thenReturn(new CmsRepository("http://host:123/svn/documentation"));
		doc.addField("prop_abx.Dependencies", abxdeps);
		doc.addField("ref", "/existing/url");
		doc.addField("refurl", "http://host:123/existing/url");
		
		IndexingItemHandler handler = new IndexingItemHandlerAbxDependencies();
		handler.handle(p);
		Collection<Object> ref = doc.getFieldValues("ref");
		Collection<Object> refid = doc.getFieldValues("refid");
		Collection<Object> refurl = doc.getFieldValues("refurl");
		assertEquals("Should have added the three dependencies as refid", 3, refid.size());
		assertTrue(refid.contains("x-svn:///svn/documentation^/graphics/cms/process/2.0/op-edit.png"));
		assertTrue(refid.contains("x-svn:///svn/documentation^/xml/reference/cms/adapter/Introduction%20to%20CMS.xml"));
		assertTrue(refid.contains("x-svn:///svn/documentation^/xml/reference/cms/User%20interface.xml?p=123"));
		assertEquals("refurl should contain the already added url and one extra per dependency", 4, refurl.size());
		assertTrue(refurl.contains("http://host:123/svn/documentation/graphics/cms/process/2.0/op-edit.png"));
		assertTrue(refurl.contains("http://host:123/svn/documentation/xml/reference/cms/adapter/Introduction%20to%20CMS.xml"));
		assertTrue(refurl.contains("http://host:123/svn/documentation/xml/reference/cms/User%20interface.xml?p=123"));
		// TODO how to handle revision, extra refres field with plain resource URL (no query, no hash), maybe without protocol
	}

}
