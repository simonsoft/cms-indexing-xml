/**
 * Copyright (C) 2009-2017 Simonsoft Nordic AB
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Test;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNRepository;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import se.repos.indexing.ReposIndexing;
import se.repos.indexing.scheduling.IndexingSchedule;
import se.repos.indexing.solrj.SolrCommit;
import se.simonsoft.cms.indexing.xml.solr.XmlIndexWriterSolrj;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.svn.runtime.RepoId;
import se.simonsoft.svn.runtime.SvnDataset;
import se.simonsoft.svn.runtime.SvnRevisionAvailableEvent;

@QuarkusTest
@TestProfile(MockableSvnDatasetProfile.class)
public class HandlerXmlIntegrationTest extends MockableSvnDatasetTest {

	private static final SvnDataset TINY_INLINE_DATASET = new SvnDataset(
			"se/simonsoft/cms/indexing/xml/datasets/tiny-inline", 2);

	private static final SvnDataset RID_DUPLICATE_DATASET = new SvnDataset(
			"se/simonsoft/cms/indexing/xml/datasets/tiny-ridduplicate", 2);

	private static final SvnDataset INVALID_DATASET = new SvnDataset(
			"se/simonsoft/cms/indexing/xml/datasets/tiny-invalid", 2);

	private static final SvnDataset ATTRIBUTES_DATASET = new SvnDataset(
			"se/simonsoft/cms/indexing/xml/datasets/tiny-attributes", 2);

	private static final SvnDataset ATTRIBUTES_NS_DATASET = new SvnDataset(
			"se/simonsoft/cms/indexing/xml/datasets/tiny-attributes-ns", 2);

	private static final SvnDataset RELEASE_LABELS_DATASET = new SvnDataset(
			"se/simonsoft/cms/indexing/xml/datasets/releaselabels", 9);

	private static final SvnDataset RELEASE_TRANSLATION_DATASET = new SvnDataset(
			"se/simonsoft/cms/indexing/xml/datasets/releasetranslation", 9);

	@Inject
	Instance<SVNRepository> repositories;

	@Inject
	@RepoId
	Instance<String> repoIds;

	@Inject
	SvnDatasetRevisionEvents events;

	@Inject
	@Named("repositem")
	SolrClient repositem;

	@Inject
	@Named("reposxml")
	SolrClient reposxml;

	@Inject
	CmsRepository cmsRepository;

	@Inject
	IdStrategy idStrategy;

	@Inject
	XmlIndexWriter xmlIndexWriter;

	@Test
	@ActivateRequestContext
	public void testTinyInline() throws Exception {
		QuarkusMock.installMockForType(TINY_INLINE_DATASET, SvnDataset.class);
		events.clear();
		String repoId = repoIds.get();
		SVNRepository repository = repositories.get();

		assertEquals(TINY_INLINE_DATASET.revision(0), repository.getLatestRevision());
		assertEquals(SVNNodeKind.FILE, repository.checkPath("test1.xml", 2));
		assertEquals(List.of(repoId + " 1", repoId + " 2"), events.revisions());

		SolrDocumentList x1 = reposxml.query(new SolrQuery("*:*").setSort("treelocation", ORDER.asc)).getResults();
		assertEquals(4, x1.getNumFound());
		assertEquals("should get 'repoid' from repositem", idStrategy.getIdRepository(cmsRepository),
				x1.get(0).getFieldValue("repoid"));

		SolrDocumentList flagged = repositem.query(new SolrQuery("flag:hasxml AND head:true")).getResults();
		assertEquals("Documents that got added to reposxml should be flagged 'hasxml' in repositem", 1,
				flagged.getNumFound());
		Collection<Object> flags = flagged.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", flagged.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasxml'", flagged.get(0).getFieldValues("flag").contains("hasxml"));
		assertTrue("Flag 'hasxmlrepositem'", flagged.get(0).getFieldValues("flag").contains("hasxmlrepositem"));
		assertFalse("Flag 'hasridduplicate'", flagged.get(0).getFieldValues("flag").contains("hasridduplicate"));
		assertEquals("", 2, flags.size());

		// Statistics in repositem schema
		assertEquals("Should count elements", 4L, flagged.get(0).getFieldValue("count_elements"));
		assertEquals("Should count words", 3L, flagged.get(0).getFieldValue("count_words_text"));
		assertNull("not calculated, no RID", flagged.get(0).getFieldValue("count_words_translate"));

		// DOCTYPE in repositem schema
		assertEquals("repositem root element name", "document", flagged.get(0).getFieldValue("embd_xml_typename"));
		assertEquals("repositem systemid", "techdoc.dtd", flagged.get(0).getFieldValue("embd_xml_typesystem"));
		assertEquals("repositem publicid", "-//Simonsoft//DTD TechDoc Base V1.0 Techdoc//EN",
				flagged.get(0).getFieldValue("embd_xml_typepublic"));

		// Depth for reposxml
		assertEquals("null since item is not a translation", null,
				flagged.get(0).getFieldValue("count_reposxml_depth"));

		// Reposxml
		assertEquals("Should index all elements", 4, x1.size());

		assertEquals("document/root element name", "doc", x1.get(0).getFieldValue("name"));
		assertEquals("element pos", "1", x1.get(0).getFieldValue("treelocation"));
		assertEquals("all elements", 4L, x1.get(0).getFieldValue("count_elements"));
		assertEquals("word count identical to repositem (document element)", 3L,
				x1.get(0).getFieldValue("count_words_text"));
		assertEquals("word count translate", 3L, x1.get(0).getFieldValue("count_words_translate"));
		assertEquals("word count child (immediate text)", 0L, x1.get(0).getFieldValue("count_words_child"));
		//assertEquals("Currently not including 'hasxml': " + flags.toString(), 1, x1.get(0).getFieldValues("flag").size());
		assertNull("no flags in reposxml at this time", x1.get(0).getFieldValues("flag"));

		assertEquals("cms namespace", "http://www.simonsoft.se/namespace/cms", x1.get(0).getFieldValue("ns_cms"));
		assertNull("cmsrepoxml ns suppressed", x1.get(0).getFieldValue("ns_cmsreposxml"));

		assertEquals("document/root element name", "elem", x1.get(2).getFieldValue("name"));
		assertEquals("element pos", "1.2", x1.get(2).getFieldValue("treelocation"));
		assertEquals("elements below", 2L, x1.get(2).getFieldValue("count_elements"));
		assertEquals("word count", 2L, x1.get(2).getFieldValue("count_words_text"));
		assertEquals("word count child (immediate text)", 1L, x1.get(2).getFieldValue("count_words_child"));

		assertNull("no ns on element", x1.get(2).getFieldValue("ns_cms"));
		assertNull("no ns on element", x1.get(2).getFieldValue("ns_cmsreposxml"));
		assertEquals("inherited cms namespace", "http://www.simonsoft.se/namespace/cms",
				x1.get(2).getFieldValue("ins_cms"));
		assertNull("inherited cmsrepoxml ns suppressed", x1.get(2).getFieldValue("ins_cmsreposxml"));

		// The "typename" is quite debatable because the test document has an incorrect DOCTYPE declaration (root element is "doc" not "document").
		// Now keeping DOCTYPE in repositem.
		/*
		assertEquals("should set root element name", "document", x1.get(0).getFieldValue("typename"));
		assertEquals("should set systemid", "techdoc.dtd", x1.get(0).getFieldValue("typesystem"));
		assertEquals("should set publicid", "-//Simonsoft//DTD TechDoc Base V1.0 Techdoc//EN", x1.get(0).getFieldValue("typepublic"));
		*/
		assertEquals("should extract source", "<elem>text</elem>", x1.get(1).getFieldValue("source_reuse"));
	}

	@Test
	@ActivateRequestContext
	public void testTinyRidDuplicate() throws Exception {
		QuarkusMock.installMockForType(RID_DUPLICATE_DATASET, SvnDataset.class);

		assertEquals(RID_DUPLICATE_DATASET.revision(0), repositories.get().getLatestRevision());

		SolrDocumentList x1 = reposxml.query(new SolrQuery("pathname:test1.xml").addSort("treelocation", ORDER.asc)).getResults();
		assertEquals("Should index all elements", 5, x1.getNumFound());
		assertEquals("should get 'repoid' from repositem", idStrategy.getIdRepository(cmsRepository),
				x1.get(0).getFieldValue("repoid"));

		SolrDocumentList flagged = repositem.query(new SolrQuery("pathname:test1.xml AND flag:hasxml AND head:true")).getResults();
		assertEquals("Documents that got added to reposxml should be flagged 'hasxml' in repositem", 1, flagged.getNumFound());
		Collection<Object> flags = flagged.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", flagged.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasxml'", flagged.get(0).getFieldValues("flag").contains("hasxml"));
		assertTrue("Flag 'hasridduplicate'", flagged.get(0).getFieldValues("flag").contains("hasridduplicate"));
		assertEquals("3 flag(s)", 3, flags.size());

		Collection<Object> duplicates = flagged.get(0).getFieldValues("embd_xml_ridduplicate");
		assertEquals("one duplicate, mentioned once", 1, duplicates.size());
		assertEquals("List the duplicate RIDs in repositem core", "2gyvymn15kv0002", duplicates.iterator().next());

		// Back to asserting on reposxml.
		assertEquals("second element", "section", x1.get(1).getFieldValue("name"));
		assertEquals("third element", "elem", x1.get(2).getFieldValue("name"));
		// No longer providing source, has been blocked by other extractor since a few years.
		//assertEquals("should extract source", "<elem xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" name=\"ch1\" cms:rid=\"2gyvymn15kv0002\">text</elem>", x1.get(2).getFieldValue("source"));
		assertEquals("should extract source_reuse", "<elem>text</elem>", x1.get(2).getFieldValue("source_reuse"));
	}

	@Test
	@ActivateRequestContext
	public void testTinyRidDuplicateTsuppress() throws Exception {
		QuarkusMock.installMockForType(RID_DUPLICATE_DATASET, SvnDataset.class);

		assertEquals(RID_DUPLICATE_DATASET.revision(0), repositories.get().getLatestRevision());

		SolrDocumentList x1 = reposxml.query(new SolrQuery("pathname:test1-tsuppress.xml").addSort("treelocation", ORDER.asc)).getResults();
		assertEquals("Should index all elements", 5, x1.getNumFound());
		assertEquals("should get 'repoid' from repositem", idStrategy.getIdRepository(cmsRepository),
				x1.get(0).getFieldValue("repoid"));

		SolrDocumentList flagged = repositem.query(new SolrQuery("pathname:test1-tsuppress.xml AND flag:hasxml AND head:true")).getResults();
		assertEquals("Documents that got added to reposxml should be flagged 'hasxml' in repositem", 1, flagged.getNumFound());
		Collection<Object> flags = flagged.get(0).getFieldValues("flag");
		assertFalse("Flag - not empty string", flagged.get(0).getFieldValues("flag").contains(""));
		assertTrue("Flag 'hasxml'", flagged.get(0).getFieldValues("flag").contains("hasxml"));
		assertFalse("Flag 'hasridduplicate'", flagged.get(0).getFieldValues("flag").contains("hasridduplicate"));
		assertTrue("Flag 'hastsuppress'", flagged.get(0).getFieldValues("flag").contains("hastsuppress"));
		assertEquals("only hasxml, hasxmlrepositem, hastsuppress flag", 3, flags.size());

		// Back to asserting on reposxml.
		assertEquals("second element", "section", x1.get(1).getFieldValue("name"));
		assertEquals("third element", "elem", x1.get(2).getFieldValue("name"));
		assertEquals("should extract source_reuse", "<elem>text</elem>", x1.get(2).getFieldValue("source_reuse"));
	}

	@Test
	@ActivateRequestContext
	public void testNextRevisionDeletesElement() throws Exception {
		QuarkusMock.installMockForType(TINY_INLINE_DATASET, SvnDataset.class);

		assertEquals(TINY_INLINE_DATASET.revision(0), repositories.get().getLatestRevision());

		SolrDocumentList x1 = reposxml.query(new SolrQuery("*:*").setSort("treelocation", ORDER.asc)).getResults();
		assertEquals(4, x1.getNumFound());
		assertEquals("should get 'repoid' from repositem", idStrategy.getIdRepository(cmsRepository),
				x1.get(0).getFieldValue("repoid"));
		assertEquals("should get 'pathfull' from repositem", cmsRepository.getPath() + "/test1.xml",
				x1.get(0).getFieldValue("pathfull"));

		SolrDocumentList flagged = repositem.query(new SolrQuery("flag:hasxml AND head:true")).getResults();
		assertEquals("Documents that got added to reposxml should be flagged 'hasxml' in repositem", 1,
				flagged.getNumFound());

		// Basic tests related to the deletePath implementation (avoiding the use of deleteByQuery due to performance).
		String idBase = idStrategy.getId(cmsRepository, new RepoRevision(2, null), new CmsItemPath("/test1.xml")) + "|";
		SolrDocument idDocument = x1.stream()
				.filter(document -> (idBase + "00000002").equals(document.getFieldValue("id")))
				.findFirst().orElseThrow();
		String idReposxml = (String) idDocument.getFieldValue("id");
		assertEquals("reposxml id format is vital for delete", idBase + "00000002", idReposxml);
		assertEquals("remove the element part of id", idBase, XmlIndexWriterSolrj.getIdBase(idDocument, null));

		// TODO delete one of the elements and make sure it is not there after indexing next revision, would indicate reliance on id overwrite

		// At least managed to test a faked delete.
		CmsChangesetItem c = mock(CmsChangesetItem.class);
		when(c.getPath()).thenReturn(new CmsItemPath("/test1.xml"));

		// Test the query
		SolrQuery qD = XmlIndexWriterSolrj.getDeleteQuery(cmsRepository, c);
		SolrDocumentList xD = reposxml.query(qD).getResults();
		assertEquals(4, xD.getNumFound());

		// Test actual delete
		xmlIndexWriter.deletePath(cmsRepository, c);
		new SolrCommit(reposxml, true).run();

		SolrDocumentList xDeleted = reposxml.query(new SolrQuery("*:*")).getResults();
		assertEquals(0, xDeleted.getNumFound());
	}

	@Test
	@ActivateRequestContext
	public void testInvalidXml() throws Exception {
		QuarkusMock.installMockForType(INVALID_DATASET, SvnDataset.class);

		assertEquals(INVALID_DATASET.revision(0), repositories.get().getLatestRevision());

		SolrDocumentList x1 = reposxml.query(new SolrQuery("*:*")).getResults();
		assertEquals("Should skip the document because it is not parseable as XML. Thus we can try formats that may be XML, such as html, without breaking indexing.",
				0, x1.getNumFound());

		SolrDocumentList flagged = repositem.query(new SolrQuery("flag:hasxmlerror AND head:true")).getResults();
		assertEquals("Should be flagged as error in repositem", 1, flagged.getNumFound());
	}

	@Test
	@ActivateRequestContext
	public void testJoin() throws SolrServerException, IOException, SVNException {
		QuarkusMock.installMockForType(TINY_INLINE_DATASET, SvnDataset.class);

		assertEquals(TINY_INLINE_DATASET.revision(0), repositories.get().getLatestRevision());

		SolrDocumentList j1 = reposxml.query(new SolrQuery("{!join from=id to=id_p}*:*")).getResults();
		assertEquals("all elements that have a parent, got " + j1, 3, j1.getNumFound());
		for (SolrDocument e : j1) {
			assertNotEquals("root does not have a parent", "doc", e.getFieldValue("name"));
		}

		SolrDocumentList j2 = reposxml.query(new SolrQuery("{!join from=id_p to=id}*:*")).getResults();
		assertEquals("all elements that have a child, got " + j2, 2, j2.getNumFound());

		SolrDocumentList j3 = reposxml.query(new SolrQuery("{!join from=id_p to=id}name:inline")).getResults();
		assertEquals("all elements that have a child which is an <inline/>, got " + j3, 1, j3.getNumFound());
		assertEquals("elem", j3.get(0).getFieldValue("name"));
		String expectedElementId = idStrategy.getId(cmsRepository, new RepoRevision(2, null),
				new CmsItemPath("/test1.xml")) + "|00000003";
		assertEquals(expectedElementId, j3.get(0).getFieldValue("id"));

		SolrDocumentList j4 = reposxml.query(new SolrQuery("name:elem AND {!join from=id_p to=id}*:*")).getResults();
		assertEquals("all elements that are an elem and have a child, got " + j4, 1, j4.getNumFound());
		assertEquals(expectedElementId, j4.get(0).getFieldValue("id"));

		SolrDocumentList j5 = reposxml.query(new SolrQuery("{!join from=id_p to=id}(name:elem OR name:inline)")).getResults();
		assertEquals("all elements that have a child which is either <elem/> or <inline/>" + j5, 2, j5.getNumFound());

		// why doesn't this run? instead use Parameter dereferencing?
		//SolrDocumentList j6 = reposxml.query(new SolrQuery("repo:tiny-inline AND {!join from=id_p to=id}(name:elem OR name:inline)")).getResults();
		//assertEquals("all elements that have a child which is either <elem/> or <inline/>, in the test repo" + j6, 2, j6.getNumFound());

		SolrDocumentList j7 = reposxml.query(new SolrQuery("{!join from=id_p to=id}(text:\"elem text\" AND name:elem)")).getResults();
		assertEquals("elements that have a child which matches two criterias" + j7, 1, j7.getNumFound());

		SolrDocumentList j8 = reposxml.query(new SolrQuery("{!join from=id_a to=id}name:inline")).getResults();
		assertEquals("elements with a descendat which is an <inline/>, got " + j8, 2, j8.getNumFound());

		// "Parameter dereferencing", http://wiki.apache.org/solr/LocalParams#parameter_dereferencing, but how to do "qq" in solrj?
//		// find all figures with a bylinew with value "me"
//		assertJQ(req("q", "{!join from=id_p to=id v=$qq}",
//					"qq", "name:byline AND pos:1.2.2", // we don't have text indexed in this test so we use pos instead
//					//"qf", "name",
//					"fl", "id",
//					"debugQuery", "true"),
//				"/response=={'numFound':1,'start':0,'docs':[{'id':'testdoc1_e3'}]}");
	}


	@Test
	@ActivateRequestContext
	public void testTinyAttributes() throws Exception {
		QuarkusMock.installMockForType(ATTRIBUTES_DATASET, SvnDataset.class);

		assertEquals(ATTRIBUTES_DATASET.revision(0), repositories.get().getLatestRevision());

		SolrQuery q1 = new SolrQuery("*:*").addSort("treelocation", SolrQuery.ORDER.asc);
		SolrDocumentList x1 = reposxml.query(q1).getResults();
		assertEquals(4, x1.getNumFound());

		assertEquals("get name of root", "root", x1.get(0).getFieldValue("a_name"));
		assertEquals("get depth of root", 1, x1.get(0).getFieldValue("depth"));
		assertEquals("get pos/treeloc of root", "1", x1.get(0).getFieldValue("treelocation"));
		assertEquals("get name of e1", "ch1", x1.get(1).getFieldValue("a_name"));

		assertNull("get name of e2", x1.get(2).getFieldValue("a_name"));
		assertEquals("get id of e2", "e2", x1.get(2).getFieldValue("a_id"));

		assertEquals("get ancestor name of e1 - tests that inherited attr is not overridden by local attr", "root", x1.get(1).getFieldValue("aa_name"));
		assertEquals("get inherited name of e1 - overridden by local attr", "ch1", x1.get(1).getFieldValue("ia_name"));

		assertEquals("get ancestor name of e2", "root", x1.get(2).getFieldValue("aa_name"));
		assertEquals("get inherited name of e2", "root", x1.get(2).getFieldValue("ia_name"));

		assertEquals("get p-sibling name of e2", "ch1", x1.get(2).getFieldValue("sa_name"));

		assertEquals("get element name of inline", "inline", x1.get(3).getFieldValue("name"));
		assertEquals("get inherited name of inline", "root", x1.get(3).getFieldValue("ia_name"));
		assertEquals("get depth of inline", 3, x1.get(3).getFieldValue("depth"));
		assertEquals("get pos/treeloc of inline", "1.2.1", x1.get(3).getFieldValue("treelocation"));
		assertNull("get p-sibling name of inline", x1.get(3).getFieldValue("sa_name"));
	}

	@Test
	@ActivateRequestContext
	public void testTinyAttributesNs() throws Exception {
		QuarkusMock.installMockForType(ATTRIBUTES_NS_DATASET, SvnDataset.class);

		assertEquals(ATTRIBUTES_NS_DATASET.revision(0), repositories.get().getLatestRevision());

		SolrQuery q1 = new SolrQuery("*:*").addSort("treelocation", SolrQuery.ORDER.asc);
		SolrDocumentList x1 = reposxml.query(q1).getResults();
		assertEquals(4, x1.getNumFound());

		assertEquals("get name of root", "root", x1.get(0).getFieldValue("a_name"));
		assertEquals("get depth of root", 1, x1.get(0).getFieldValue("depth"));
		assertEquals("get pos/treeloc of root", "1", x1.get(0).getFieldValue("treelocation"));
		assertEquals("get RID of root", "2gyvymn15kv0000", x1.get(0).getFieldValue("a_cms.rid"));
		assertEquals("get doc.code of root", "period", x1.get(0).getFieldValue("a_doc,code"));

		assertEquals("get name of e1", "ch1", x1.get(1).getFieldValue("a_name"));
		assertEquals("get RID of e1", "2gyvymn15kv0001", x1.get(1).getFieldValue("a_cms.rid"));
		assertEquals("get doc.code of e1", "period-child", x1.get(1).getFieldValue("a_doc,code"));

		assertNull("get name of e2", x1.get(2).getFieldValue("a_name"));
		assertEquals("get id of e2", "e2", x1.get(2).getFieldValue("a_id"));
		assertEquals("get RID of e2", "2gyvymn15kv0002", x1.get(2).getFieldValue("a_cms.rid"));
		assertEquals("get doc.code of e2, empty", "", x1.get(2).getFieldValue("a_doc,code"));
		assertNull("Non-existant attributes are null in schema", x1.get(2).getFieldValue("a_nonexist"));

		// Also test ancestor attributes
		assertEquals("get ancestor RID of e1", "2gyvymn15kv0000", x1.get(1).getFieldValue("aa_cms.rid"));
		assertEquals("get inherited RID of e1", "2gyvymn15kv0001", x1.get(1).getFieldValue("ia_cms.rid"));
		assertEquals("get ancestor doc.code of e1", "period", x1.get(1).getFieldValue("aa_doc,code"));
		assertEquals("get inherited doc.code of e1", "period-child", x1.get(1).getFieldValue("ia_doc,code"));

		assertEquals("get ancestor RID of e2", "2gyvymn15kv0000", x1.get(2).getFieldValue("aa_cms.rid"));
		assertEquals("get inherited RID of e2", "2gyvymn15kv0002", x1.get(2).getFieldValue("ia_cms.rid"));
		assertEquals("get ancestor doc.code of e2", "period", x1.get(2).getFieldValue("aa_doc,code"));
		assertEquals("get inherited doc.code of e2", "", x1.get(2).getFieldValue("ia_doc,code"));
	}

	@Test
	@ActivateRequestContext
	public void testAttributesReleasetranslationRelease() throws Exception {
		QuarkusMock.installMockForType(RELEASE_TRANSLATION_DATASET, SvnDataset.class);

		assertEquals(RELEASE_TRANSLATION_DATASET.revision(0), repositories.get().getLatestRevision());

		SolrDocument elem;
		// search for the first title
		SolrDocumentList findUsingRid = reposxml.query(new SolrQuery("a_cms.rid:2gyvymn15kv0001 AND -prop_abx.TranslationLocale:*")).getResults();
		assertEquals("Should find the first title in the release (though actually a future one)", 1, findUsingRid.getNumFound());
		elem = findUsingRid.get(0);
		assertEquals("get the rid attribute", "2gyvymn15kv0001", elem.getFieldValue("a_cms.rid"));
		assertEquals("get the parent rlogicalid", "x-svn:///svn/testaut1^/tms/xml/Docs/My%20First%20Novel.xml?p=5", elem.getFieldValue("ia_cms.rlogicalid"));

		findUsingRid = reposxml.query(new SolrQuery("a_cms.rid:2gyvymn15kv0006 AND -prop_abx.TranslationLocale:*")).getResults();
		assertEquals("Should find a para", 1, findUsingRid.getNumFound());
		elem = findUsingRid.get(0);
		assertEquals("verify it is a para", "p", elem.getFieldValue("name"));
		assertEquals("get the rid attribute", "2gyvymn15kv0006", elem.getFieldValue("a_cms.rid"));
		assertEquals("get the ancestor rid attribute (in this case parent rid)", "2gyvymn15kv0004", elem.getFieldValue("aa_cms.rid"));
		assertEquals("get the inherited rid attribute (in this case context element rid)", "2gyvymn15kv0006", elem.getFieldValue("ia_cms.rid"));
		assertEquals("get the root rid attribute", "2gyvymn15kv0000", elem.getFieldValue("ra_cms.rid"));
		assertEquals("get the preceding sibling rid attribute", "2gyvymn15kv0005", elem.getFieldValue("sa_cms.rid"));
		assertNull("get the project id attribute", elem.getFieldValue("a_cms.translation-project"));

		assertEquals("get the inherited rlogicalid attribute", "x-svn:///svn/testaut1^/tms/xml/Secs/First%20chapter.xml?p=4", elem.getFieldValue("ia_cms.rlogicalid"));

		assertEquals("assist depends on patharea", Arrays.asList(new String[] {"release"}), elem.getFieldValue("patharea"));
		assertEquals("assist depends on reusevalue even for a Release", 1, elem.getFieldValue("reusevalue"));
	}

	@SuppressWarnings("unchecked")
	@Test
	@ActivateRequestContext
	public void testAttributesReleasetranslationTranslation() throws Exception {
		QuarkusMock.installMockForType(RELEASE_TRANSLATION_DATASET, SvnDataset.class);

		assertEquals(RELEASE_TRANSLATION_DATASET.revision(0), repositories.get().getLatestRevision());

		SolrDocumentList flagged = repositem.query(new SolrQuery("flag:hasxml AND head:true")).getResults();
		assertEquals("Documents that got added to reposxml should be flagged 'hasxml' in repositem", 2, flagged.getNumFound());
		assertNull("Should NOT limit depth of Release", flagged.get(0).getFieldValue("count_reposxml_depth"));
		assertEquals("Should limit depth of Translation", 1L, flagged.get(1).getFieldValue("count_reposxml_depth"));
		assertEquals("no of topics - 3 techdoc sections", 3L, flagged.get(1).getFieldValue("count_elements_topic"));

		SolrDocumentList findAll = reposxml.query(new SolrQuery("prop_abx.TranslationLocale:*")).getResults();
		assertEquals("Should find all elements in the single translation", 1, findAll.getNumFound());
		assertEquals("Should limit reposxml extraction depth", 1L, findAll.get(0).getFieldValue("count_reposxml_depth"));

		SolrDocumentList findUsingRid0 = reposxml.query(new SolrQuery("a_cms.rid:2gyvymn15kv0000 AND prop_abx.TranslationLocale:*")).getResults();
		assertEquals("Should find root element in the Translation", 1, findUsingRid0.getNumFound());
		SolrDocument elem0 = findUsingRid0.get(0);

		String ridStr = (String) elem0.getFieldValue("reuseridreusevalue");
		assertEquals("number of elements is 13, verified",  13, ridStr.split(" ").length);
		assertEquals("RIDs with reusevalue > 0", "2gyvymn15kv0000 2gyvymn15kv0001 2gyvymn15kv0002 2gyvymn15kv0003 2gyvymn15kv0004 2gyvymn15kv0005 2gyvymn15kv0006 2gyvymn15kv0007 2gyvymn15kv0008 2gyvymn15kv0009 2gyvymn15kv000a 2gyvymn15kv000b 2gyvymn15kv000c ", ridStr);

		List<String> cList = (List<String>) elem0.getFieldValue("reuse_c_sha1_release_descendants");
		//assertEquals("debug contents", "...", cList);
		assertTrue("should contain Release checksum", cList.contains("c5fed03ed1304cecce75d63aee2ada2b0f2326af"));
		Collection<Object> shard = elem0.getFieldValues("reuse_rid_c5");
		assertNotNull(shard);
		assertEquals("number of RIDs in shard 'c5'", 1, shard.size());
		assertEquals("get RID by checksum", "c5fed03ed1304cecce75d63aee2ada2b0f2326af 2gyvymn15kv0006", shard.iterator().next());
	}

	@Test
	@ActivateRequestContext
	public void testReleaseLabelSort1() throws Exception {
		QuarkusMock.installMockForType(RELEASE_LABELS_DATASET, SvnDataset.class);

		assertEquals(RELEASE_LABELS_DATASET.revision(0), repositories.get().getLatestRevision());

		SolrDocumentList rlLegacy = repositem.query(new SolrQuery("patharea:release AND head:true").setSort("prop_abx.ReleaseLabel", ORDER.asc).setFields("*")).getResults();
		assertEquals("no of releases", 8, rlLegacy.getNumFound());

		// The Legacy string based sorting
		// Case variants have the same lowercased sort key, so their relative order is undefined.
		Iterator<SolrDocument> itLegacy = rlLegacy.iterator();
		assertEquals("10", itLegacy.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals("2", itLegacy.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals(Set.of("ab", "AB"), Set.of(
				itLegacy.next().getFieldValue("prop_abx.ReleaseLabel"),
				itLegacy.next().getFieldValue("prop_abx.ReleaseLabel")));
		assertEquals("AB-beta", itLegacy.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals("AB.1", itLegacy.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals(Set.of("b", "B"), Set.of(
				itLegacy.next().getFieldValue("prop_abx.ReleaseLabel"),
				itLegacy.next().getFieldValue("prop_abx.ReleaseLabel")));

		SolrDocumentList rlSort = repositem.query(new SolrQuery("patharea:release AND head:true").setSort("meta_s_s_releaselabel_sort", ORDER.asc).setFields("*")).getResults();
		assertEquals("no of releases", 8, rlSort.getNumFound());

		// The correct SemVer sorting (via String in SolR)
		Iterator<SolrDocument> itSort = rlSort.iterator();
		assertEquals("2", itSort.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals("10", itSort.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals("B", itSort.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals("AB-beta", itSort.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals("AB", itSort.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals("AB.1", itSort.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals("b", itSort.next().getFieldValue("prop_abx.ReleaseLabel"));
		assertEquals("ab", itSort.next().getFieldValue("prop_abx.ReleaseLabel"));
	}

	@Test
	@ActivateRequestContext
	public void testJoinReleasetranslationNoExtraFields() throws Exception {
		QuarkusMock.installMockForType(RELEASE_TRANSLATION_DATASET, SvnDataset.class);

		assertEquals(RELEASE_TRANSLATION_DATASET.revision(0), repositories.get().getLatestRevision());

		// search for the first title
		SolrDocumentList findUsingRid = reposxml.query(new SolrQuery("a_cms.rid:2gyvymn15kv0001 AND -prop_abx.TranslationLocale:*")).getResults();
		assertEquals("Should find the first title in the release (though actually a future one)", 1, findUsingRid.getNumFound());
		String wantedReleaseSha1 = (String) findUsingRid.get(0).getFieldValue("c_sha1_source_reuse");

		SolrDocumentList findAllMatchesWithoutJoin = reposxml.query(new SolrQuery("c_sha1_source_reuse:" + wantedReleaseSha1)).getResults();
		assertEquals("Could search for the checksum in all xml", 1, findAllMatchesWithoutJoin.getNumFound());

		SolrQuery q = new SolrQuery("c_sha1_source_reuse:" + wantedReleaseSha1
				// Because we join on the same filed name we must explicitly state that the hit should be a release, or In_Translation items would join with themselves and match
				// Do we have a release specific field that is not copied to translations? For now just exclude translations.
				+ " AND -prop_abx.TranslationLocale:*"
				// Haven't found how to combine two criterias on the join into a single join, when there's also criteria on the actual match (see join test above)
				+ " AND {!join from=prop_abx.AuthorMaster to=prop_abx.AuthorMaster}prop_abx.TranslationLocale:sv-SE"
				+ " AND {!join from=prop_abx.AuthorMaster to=prop_abx.AuthorMaster}reusevalue:1");
		SolrDocumentList findReusevalue = reposxml.query(q).getResults();
		assertEquals(1, findReusevalue.getNumFound());
		// TODO with the current data set it is impossible to assert that we don't get false positives with the above query
		// Would need another release with an Obsolete sv-SE translation and a reusevalue=1 de-DE one, which probably would match falsely
	}
}

@ApplicationScoped
class SvnDatasetRepoIdProducer {

	static final String REPO_ID = "cms-indexing-xml-dataset";

	@Produces
	@RepoId
	public String produceRepoId() {
		return REPO_ID;
	}
}

@ApplicationScoped
class SvnDatasetRevisionEvents {

	private final List<String> revisions = new ArrayList<>();

	@Inject
	ReposIndexing indexing;

	@Inject
	IndexingSchedule schedule;

	void onRevisionAvailable(@Observes SvnRevisionAvailableEvent event) {
		revisions.add(event.repoId() + " " + event.revision());
		schedule.start();
		try {
			indexing.sync(new RepoRevision(event.revision(), null));
		} finally {
			schedule.stop();
		}
	}

	List<String> revisions() {
		return List.copyOf(revisions);
	}

	void clear() {
		revisions.clear();
	}
}
