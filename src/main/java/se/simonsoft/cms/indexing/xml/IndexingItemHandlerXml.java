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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.ItemPathinfo;
import se.repos.indexing.item.ItemProperties;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.properties.CmsItemProperties;
import se.simonsoft.xmltracking.index.XmlIndexFieldExtraction;
import se.simonsoft.xmltracking.source.XmlSourceElement;
import se.simonsoft.xmltracking.source.XmlSourceHandler;
import se.simonsoft.xmltracking.source.XmlSourceReader;

public class IndexingItemHandlerXml implements IndexingItemHandler {

	public final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private XmlFileFilter xmlFileFilter = new XmlFileFilterExtensionAndSvnMimeType(); // TODO inject, mportant customization point
	
	private SupportLegacySchema supportLegacySchema = new SupportLegacySchema(); // TODO do away with gradually
	
	private XmlSourceReader sourceReader = null;
	
	private Set<XmlIndexFieldExtraction> fieldExtraction = null;

	private Provider<XmlIndexAddSession> indexAddProvider;
	
	/**
	 * @param fieldExtraction a sequence of pluggable extractors that add fields
	 * @param xmlSourceReader that processes the XML into {@link XmlSourceElement}s for the extractors
	 */
	@Inject
	public void setDependenciesIndexing(
			Set<XmlIndexFieldExtraction> fieldExtraction,
			XmlSourceReader xmlSourceReader
			) {
		this.fieldExtraction = fieldExtraction;
		this.sourceReader = xmlSourceReader;
	}

	@Inject
	public void setDependenciesIndexing(
			Provider<XmlIndexAddSession> indexAddProvider) {
		this.indexAddProvider = indexAddProvider;
	}	
	
	@Override
	public void handle(IndexingItemProgress progress) {
		CmsChangesetItem c = progress.getItem();
		if (c.isOverwritten()) {
			logger.debug("XML index only contains HEAD so skipping later overwritten {}", c.getPath());
			return;
		}
		if (c.isContent()) {
			// TODO here we should probably read mime type too, or probably after conversion to CmsItem
			if (xmlFileFilter.isXml(c, progress.getFields())) {
				logger.debug("Changeset content update item {} found", c);
				if (c.isDelete()) {
					indexDeletePath(progress.getRepository(), c);
				} else {
					indexDeletePath(progress.getRepository(), c);
					index(progress);
				}
			} else {
				logger.debug("Ignoring content update item {}, not an XML candidate file type", c);
			}
		} else {
			logger.debug("Ignoring changeset item {}, not a file", c);
		}
		// TODO until we have notification on revision end we commit always
		onRevisionEnd(progress.getRevision());
	}

	protected void index(IndexingItemProgress progress) {
		IndexingDoc itemDoc = cloneItemFields(progress.getFields());
		supportLegacySchema.handle(itemDoc);
		XmlIndexAddSession docHandler = indexAddProvider.get();
		XmlSourceHandler sourceHandler = new XmlSourceHandlerFieldExtractors(itemDoc, fieldExtraction, docHandler);
		sourceReader.read(progress.getContents(), sourceHandler);
	}
	
	private IndexingDoc cloneItemFields(IndexingDoc fields) {
		IndexingDoc doc = fields.deepCopy();
		return doc;
	}

	// TODO implement an indexing event interface
	//@Override
	public void onRevisionEnd(RepoRevision revision) {
		commit();
		if (revision.getNumber() % 1000 == 0) {
			logger.info("Optimizing index at revision {}", revision);
			optimize();
		}
	}

	private void commit() {
		try {
			solrServer.commit();
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException("Error not handled", e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException("Error not handled", e);
		}
	}

	private void optimize() {
		try {
			solrServer.optimize();
		} catch (SolrServerException e) {
			logger.error("Index optimize failed: {}", e.getMessage(), e);
			// we can live without optimized index, could fail because optimize needs lots of free disk
		} catch (IOException e) {
			logger.error("Solr connection issues at optimize: ", e.getMessage(), e);
			throw new RuntimeException("Optimize failed", e);
		}
	}
	
	protected void indexDeletePath(CmsRepository repository, CmsChangesetItem item) {
		// we can't use id to delete because it may contain revision, we could probably delete an exact item by hooking into the head=false update in item indexing
		String query = "pathfull:\"" + repository.getPath() + item.getPath().toString() + '"';
		logger.debug("Deleting previous revision of {} using query {}", item, query);
		try {
			solrServer.deleteByQuery(query);
		} catch (SolrServerException e) {
			throw new RuntimeException("not handled", e);
		} catch (IOException e) {
			throw new RuntimeException("not handled", e);
		}
		
	}
	
	@SuppressWarnings("serial")
	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return new HashSet<Class<? extends IndexingItemHandler>>() {{
				add(ItemPathinfo.class);
				add(ItemProperties.class);
			}};
	}	

}
