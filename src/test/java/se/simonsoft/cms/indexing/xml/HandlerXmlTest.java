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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingHandlerException;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.indexing.xml.testconfig.IndexingConfigXmlBase;
import se.simonsoft.cms.indexing.xml.testconfig.IndexingConfigXmlStub;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class HandlerXmlTest {

	private Injector injector;
	
	@Before
	public void setUp() {
		
		injector = Guice.createInjector(new IndexingConfigXmlBase(), new IndexingConfigXmlStub());
	}
	
	@Test
	public void testXmlSourceElementInvalid() {
		XmlIndexWriter indexWriter = mock(XmlIndexWriter.class);
		Set<XmlIndexFieldExtraction> fe = new LinkedHashSet<XmlIndexFieldExtraction>();
		final List<XmlSourceElement> calls = new LinkedList<XmlSourceElement>();
		fe.add(new XmlIndexFieldExtraction() {
			@Override
			public void begin(XmlSourceElement processedElement, XmlIndexElementId idProvider) throws XmlNotWellFormedException {
				
			}
			
			@Override
			public void end(XmlSourceElement processedElement, XmlIndexElementId idProvider, IndexingDoc fields) throws XmlNotWellFormedException {
				calls.add(processedElement);
				throw new XmlNotWellFormedException("something went wrong", new RuntimeException("some xml lib's cause"));
			}

			@Override
			public void endDocument() {
				
			}

			@Override
			public void startDocument(XmlIndexProgress xmlProgress) {

			}
		});
		
		
		HandlerXml handlerXml = injector.getInstance(HandlerXml.class);
		handlerXml.setDependenciesIndexing(indexWriter);
		handlerXml.setFieldExtraction(fe);
		
		CmsChangesetItem p1i = mock(CmsChangesetItem.class);
		when(p1i.isFile()).thenReturn(true);
		when(p1i.getFilesize()).thenReturn(1L);
		when(p1i.getPath()).thenReturn(new CmsItemPath("/some.xml"));
		IndexingDoc p1f = new IndexingDocIncrementalSolrj();
		p1f.addField("id", "base-id");
		p1f.addField("embd_Content-Type", "application/xml");
		IndexingItemProgress p1 = mock(IndexingItemProgress.class);
		when(p1.getItem()).thenReturn(p1i);
		when(p1.getFields()).thenReturn(p1f);
		when(p1.getContents()).thenReturn(new ByteArrayInputStream("<p>P</p>".getBytes()));
		try {
			handlerXml.handle(p1); // should catch and log the exception but proceed to next item
			fail("should catch and log the exception but proceed to next item");
		} catch (IndexingHandlerException e) {
			// expected
		}	
		assertEquals("Should have called the extract method", 1, calls.size());
	}

	@Test
	public void testXmlSourceElementFatalError() {

		XmlIndexWriter indexWriter = mock(XmlIndexWriter.class);
		Set<XmlIndexFieldExtraction> fe = new LinkedHashSet<XmlIndexFieldExtraction>();
		final List<XmlSourceElement> calls = new LinkedList<XmlSourceElement>();
		fe.add(new XmlIndexFieldExtraction() {
			@Override
			public void begin(XmlSourceElement processedElement, XmlIndexElementId idProvider) throws XmlNotWellFormedException {
				
			}
			
			@Override
			public void end(XmlSourceElement processedElement, XmlIndexElementId idProvider, IndexingDoc fields) throws XmlNotWellFormedException {
				calls.add(processedElement);
				throw new RuntimeException("something went badly wrong");
			}

			@Override
			public void endDocument() {
				
			}

			@Override
			public void startDocument(XmlIndexProgress xmlProgress) {
				
			}
		});
		
		HandlerXml handlerXml = injector.getInstance(HandlerXml.class);
		handlerXml.setDependenciesIndexing(indexWriter);
		handlerXml.setFieldExtraction(fe);
		
		CmsChangesetItem p1i = mock(CmsChangesetItem.class);
		when(p1i.isFile()).thenReturn(true);
		when(p1i.getFilesize()).thenReturn(1L);
		when(p1i.getPath()).thenReturn(new CmsItemPath("/some.xml"));
		IndexingDoc p1f = new IndexingDocIncrementalSolrj();
		p1f.addField("id", "base-id");
		p1f.addField("embd_Content-Type", "application/xml");
		IndexingItemProgress p1 = mock(IndexingItemProgress.class);
		when(p1.getItem()).thenReturn(p1i);
		when(p1.getFields()).thenReturn(p1f);
		when(p1.getContents()).thenReturn(new ByteArrayInputStream("<p>P</p>".getBytes()));
		try {
			handlerXml.handle(p1); // should catch and log the exception but proceed to next item
			fail("Should not proceed on unknown indexing errors, because we might unknowingly get an incomplete index");
		} catch (Exception e) {
			// expected
		}
		
		assertEquals("Should have called the extract method", 1, calls.size());
	}	
	
	
	@Test
	public void testHandlerXmlFilesize() {

		XmlIndexWriter indexWriter = mock(XmlIndexWriter.class);
		Set<XmlIndexFieldExtraction> fe = new LinkedHashSet<XmlIndexFieldExtraction>();
		final List<XmlSourceElement> calls = new LinkedList<XmlSourceElement>();
		fe.add(new XmlIndexFieldExtraction() {
			@Override
			public void begin(XmlSourceElement processedElement, XmlIndexElementId idProvider) throws XmlNotWellFormedException {
				
			}
			
			@Override
			public void end(XmlSourceElement processedElement, XmlIndexElementId idProvider, IndexingDoc fields) throws XmlNotWellFormedException {
				calls.add(processedElement);
				throw new RuntimeException("should not extract");
			}

			@Override
			public void endDocument() {
				
			}

			@Override
			public void startDocument(XmlIndexProgress xmlProgress) {
				
			}
		});
		
		HandlerXml handlerXml = injector.getInstance(HandlerXml.class);
		handlerXml.setDependenciesIndexing(indexWriter);
		handlerXml.setFieldExtraction(fe);
		handlerXml.setConfigIndexing(10 * 1048576, null);
		
		CmsChangesetItem p1i = mock(CmsChangesetItem.class);
		when(p1i.isFile()).thenReturn(true);
		when(p1i.getFilesize()).thenReturn(11 * 1048576L);
		when(p1i.getPath()).thenReturn(new CmsItemPath("/some.xml"));
		IndexingDoc p1f = new IndexingDocIncrementalSolrj();
		p1f.addField("id", "base-id");
		p1f.addField("embd_Content-Type", "application/xml");
		IndexingItemProgress p1 = mock(IndexingItemProgress.class);
		when(p1.getItem()).thenReturn(p1i);
		when(p1.getFields()).thenReturn(p1f);
		when(p1.getContents()).thenReturn(new ByteArrayInputStream("<p>P</p>".getBytes()));
		try {
			handlerXml.handle(p1); // should catch and log the exception but proceed to next item
			fail("Should not proceed on unknown indexing errors, because we might unknowingly get an incomplete index");
		} catch (IndexingHandlerException e) {
			// expected
		}
		
		assertEquals("Should NOT have called the extract method", 0, calls.size());
	}
	
	@Test
	public void testHandlerXmlStatusObsolete() {

		XmlIndexWriter indexWriter = mock(XmlIndexWriter.class);
		Set<XmlIndexFieldExtraction> fe = new LinkedHashSet<XmlIndexFieldExtraction>();
		final List<XmlSourceElement> calls = new LinkedList<XmlSourceElement>();
		fe.add(new XmlIndexFieldExtraction() {
			@Override
			public void begin(XmlSourceElement processedElement, XmlIndexElementId idProvider) throws XmlNotWellFormedException {
				
			}
			
			@Override
			public void end(XmlSourceElement processedElement, XmlIndexElementId idProvider, IndexingDoc fields) throws XmlNotWellFormedException {
				calls.add(processedElement);
			}

			@Override
			public void endDocument() {
				
			}

			@Override
			public void startDocument(XmlIndexProgress xmlProgress) {
				
			}
		});
		
		HandlerXml handlerXml = injector.getInstance(HandlerXml.class);
		handlerXml.setDependenciesIndexing(indexWriter);
		handlerXml.setFieldExtraction(fe);
		handlerXml.setConfigIndexing(10 * 1048576, null);
		
		CmsChangesetItem p1i = mock(CmsChangesetItem.class);
		when(p1i.isFile()).thenReturn(true);
		when(p1i.getFilesize()).thenReturn(1 * 1048576L);
		when(p1i.getPath()).thenReturn(new CmsItemPath("/some.xml"));
		IndexingDoc p1f = new IndexingDocIncrementalSolrj();
		p1f.addField("id", "base-id");
		p1f.addField("prop_cms.status", "Obsolete");
		p1f.addField("embd_Content-Type", "application/xml");
		IndexingItemProgress p1 = mock(IndexingItemProgress.class);
		when(p1.getItem()).thenReturn(p1i);
		when(p1.getFields()).thenReturn(p1f);
		when(p1.getContents()).thenReturn(new ByteArrayInputStream("<p>P</p>".getBytes()));
		try {
			handlerXml.handle(p1); 
		} catch (IndexingHandlerException e) {
			fail("should not throw exception, just skip reposxml.");
		}
		
		assertEquals("Should NOT have called the extract method", 0, calls.size());
		// Should preferably verify that repositem is extracted.
		assertEquals("Repositem extraction counts the elements", "1", p1f.getFieldValue("count_elements"));
	}
	
	@Test
	public void testHandlerXmlStatusContentTypeNotXml() {

		XmlIndexWriter indexWriter = mock(XmlIndexWriter.class);
		Set<XmlIndexFieldExtraction> fe = new LinkedHashSet<XmlIndexFieldExtraction>();
		final List<XmlSourceElement> calls = new LinkedList<XmlSourceElement>();
		fe.add(new XmlIndexFieldExtraction() {
			@Override
			public void begin(XmlSourceElement processedElement, XmlIndexElementId idProvider) throws XmlNotWellFormedException {
				
			}
			
			@Override
			public void end(XmlSourceElement processedElement, XmlIndexElementId idProvider, IndexingDoc fields) throws XmlNotWellFormedException {
				calls.add(processedElement);
			}

			@Override
			public void endDocument() {
				
			}

			@Override
			public void startDocument(XmlIndexProgress xmlProgress) {
				
			}
		});
		
		HandlerXml handlerXml = injector.getInstance(HandlerXml.class);
		handlerXml.setDependenciesIndexing(indexWriter);
		handlerXml.setFieldExtraction(fe);
		handlerXml.setConfigIndexing(10 * 1048576, null);
		
		CmsChangesetItem p1i = mock(CmsChangesetItem.class);
		when(p1i.isFile()).thenReturn(true);
		when(p1i.getFilesize()).thenReturn(1 * 1048576L);
		when(p1i.getPath()).thenReturn(new CmsItemPath("/some.xml"));
		IndexingDoc p1f = new IndexingDocIncrementalSolrj();
		p1f.addField("id", "base-id");
		p1f.addField("embd_Content-Type", "application/text"); // Non-XML
		IndexingItemProgress p1 = mock(IndexingItemProgress.class);
		when(p1.getItem()).thenReturn(p1i);
		when(p1.getFields()).thenReturn(p1f);
		when(p1.getContents()).thenReturn(new ByteArrayInputStream("<p>P</p>".getBytes()));
		try {
			handlerXml.handle(p1); 
		} catch (IndexingHandlerException e) {
			fail("should not throw exception, just skip reposxml.");
		}
		
		assertEquals("Should NOT have called the extract method", 0, calls.size());
		// Should preferably verify that repositem is extracted.
		assertNull("Repositem extraction should NOT perform XML extract", p1f.getFieldValue("count_elements"));
	}
	
	
	@Test
	public void testHandlerXmlSuppressRidBefore() {

		XmlIndexWriter indexWriter = mock(XmlIndexWriter.class);
		Set<XmlIndexFieldExtraction> fe = new LinkedHashSet<XmlIndexFieldExtraction>();
		final List<XmlSourceElement> calls = new LinkedList<XmlSourceElement>();
		fe.add(new XmlIndexFieldExtraction() {
			@Override
			public void begin(XmlSourceElement processedElement, XmlIndexElementId idProvider) throws XmlNotWellFormedException {
				
			}
			
			@Override
			public void end(XmlSourceElement processedElement, XmlIndexElementId idProvider, IndexingDoc fields) throws XmlNotWellFormedException {
				calls.add(processedElement);
			}

			@Override
			public void endDocument() {
				
			}

			@Override
			public void startDocument(XmlIndexProgress xmlProgress) {
				
			}
		});
		
		HandlerXml handlerXml = injector.getInstance(HandlerXml.class);
		handlerXml.setDependenciesIndexing(indexWriter);
		handlerXml.setFieldExtraction(fe);
		handlerXml.setConfigIndexing(10 * 1048576, "90tbkfc5eev0000");
		
		CmsChangesetItem p1i = mock(CmsChangesetItem.class);
		when(p1i.isFile()).thenReturn(true);
		when(p1i.getFilesize()).thenReturn(1 * 1048576L);
		when(p1i.getPath()).thenReturn(new CmsItemPath("/some.xml"));
		IndexingDoc p1f = new IndexingDocIncrementalSolrj();
		p1f.addField("id", "base-id");
		p1f.addField("prop_cms.status", "Released");
		p1f.addField("prop_abx.ReleaseId", "2gffjvemcj60000");
		p1f.addField("embd_Content-Type", "application/xml");
		IndexingItemProgress p1 = mock(IndexingItemProgress.class);
		when(p1.getItem()).thenReturn(p1i);
		when(p1.getFields()).thenReturn(p1f);
		when(p1.getContents()).thenReturn(new ByteArrayInputStream("<p>P</p>".getBytes()));
		try {
			handlerXml.handle(p1); 
		} catch (IndexingHandlerException e) {
			fail("should not throw exception, just skip reposxml.");
		}
		
		assertEquals("Should NOT have called the extract method", 0, calls.size());
		// Should preferably verify that repositem is extracted.
		assertEquals("Repositem extraction counts the elements", "1", p1f.getFieldValue("count_elements"));
	}
	
}
