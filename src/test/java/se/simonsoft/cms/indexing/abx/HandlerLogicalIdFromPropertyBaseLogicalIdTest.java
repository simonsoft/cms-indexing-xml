package se.simonsoft.cms.indexing.abx;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.impl.CmsItemIdArg;

public class HandlerLogicalIdFromPropertyBaseLogicalIdTest {

	@Test
	public void testHandle() {
		String baseLogicalId = "x-svn:///svn/documentation^/xml/reference/cms/User%20interface.xml?p=123";
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		CmsChangesetItem pi = mock(CmsChangesetItem.class);
		when(pi.getRevisionChanged()).thenReturn(new RepoRevision(145, null));
		when(p.getItem()).thenReturn(pi);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		when(p.getFields()).thenReturn(doc);
		when(p.getRepository()).thenReturn(new CmsRepository("http://host:123/svn/documentation"));
		doc.addField("repohost", "host:123");
		doc.addField("prop_abx.BaseLogicalId", baseLogicalId);
		
		IndexingItemHandler handler = new HandlerLogicalIdFromProperty();
		handler.handle(p);
		String urlid = (String) doc.getFieldValue("urlid");
		assertNotNull("Should set field", urlid);
		CmsItemIdArg id = new CmsItemIdArg(urlid);
		assertEquals("/xml/reference/cms/User interface.xml", id.getRelPath().getPath());
		assertEquals("Revision should not be from base but from commit rev", new Long(145), id.getPegRev());
	}

}
