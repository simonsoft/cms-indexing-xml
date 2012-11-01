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
package se.simonsoft.cms.indexing.xml.hook;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.admin.CmsChangesetReader;
import se.simonsoft.cms.admin.CmsContentsReader;
import se.simonsoft.cms.admin.CmsRepositoryInspection;
import se.simonsoft.cms.item.CmsConnectionException;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemLookup;
import se.simonsoft.cms.item.CmsItemNotFoundException;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangeset;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.xmltracking.source.XmlSourceHandler;
import se.simonsoft.xmltracking.source.XmlSourceReader;

public class ChangesetIterationTms implements ChangesetIteration {

	private static final Logger logger = LoggerFactory.getLogger(ChangesetIterationTms.class);
	
	private Set<String> extensionsToTry = new HashSet<String>(Arrays.asList("xml"));
	
	private CmsChangesetReader changesetReader = null;
	private CmsContentsReader contentsReader = null;

	private IndexingContext indexingContext = null;
	
	private XmlSourceHandler sourceHandler = null;
	private XmlSourceReader sourceReader = null;
	
	@Inject
	public void setDependenciesHook(
			CmsChangesetReader changesetReader,
			CmsContentsReader contentsReader) {
		this.changesetReader = changesetReader;
		this.contentsReader = contentsReader;
	}
	
	@Inject
	public void setDependenciesIndexing(
			IndexingContext indexingContext,
			@Named("indexing") XmlSourceHandler xmlSourceHandler,
			XmlSourceReader xmlSourceReader
			) {
		this.indexingContext  = indexingContext;
		this.sourceHandler = xmlSourceHandler;
		this.sourceReader = xmlSourceReader;
	}
	
	@Override
	public void onHook(CmsRepositoryInspection repository, RepoRevision revision) {
		indexingContext.setRepository(repository);
		indexingContext.setRevision(revision);
		CmsItemLookup itemAndContentsReader = new CmsItemAndContentsLookupAdministrative(repository, revision, contentsReader);
		CmsChangeset changeset = changesetReader.read(repository, revision);
		onChangeset(changeset, itemAndContentsReader);
	}
	
	@Override
	public void onChangeset(CmsChangeset changeset,
			CmsItemLookup itemAndContentsReader) {
		if (!changeset.isDeriveEnabled()) {
			logger.warn("Changeset excludes implicit items, index will be incomplete");
		}
		List<String> indexed = new LinkedList<String>();
		for (CmsChangesetItem c : changeset.getItems()) {
			if (c.isFile()) {
				// TODO here we should probably read mime type too, or probably after conversion to CmsItem
				if (extensionsToTry.contains(c.getPath().getExtension())) {
					logger.debug("Changeset content update item {} found", c);
					onUpdate(c, itemAndContentsReader);
					indexed.add(c.getPath().toString());
				} else {
					logger.debug("Ignoring content update item {}, not an XML candidate file type", c);
				}
			} else {
				logger.debug("Ignoring changeset item {}, not a file", c);
			}
		}
		logger.info("Indexing, reposxml, attempted for {} files {}", indexed.size(), indexed);
	}
	
	/**
	 * TODO How and where should "svnlook changed" entries be converted to CmsItemId?
	 * @param c Content that is a file likely to be XML, after contents and/or property modification
	 */
	protected void onUpdate(CmsChangesetItem c,
			CmsItemLookup itemAndContentsReader) {
		CmsItemId id = new ChangesetItemToContentsReaderId(c.getPath());
		CmsItem item;
		try {
			item = itemAndContentsReader.getItem(id);
		} catch (CmsConnectionException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException("Error not handled", e);
		} catch (CmsItemNotFoundException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException("Error not handled", e);
		}
		if (item instanceof CmsItemAndContents) {
			onUpdate((CmsItemAndContents) item);
		} else {
			throw new RuntimeException("Expecting lookup to return specific item impl for adimistrative reading");
		}
	}

	@Override
	public void onUpdate(CmsItemAndContents item) {
		indexingContext.setItem(item);
		CmsItemContentsBuffer contents = new CmsItemContentsBuffer(item);
		sourceReader.read(contents.getContents(), sourceHandler);
	}
	
	/**
	 * Another part of the bridge from CmsChangeset to generic CmsItem+contents,
	 * implemented on a need-to-use basis.
	 * 
	 * We might have an impl somewhere that takes only repository and path, but where?
	 */
	static class ChangesetItemToContentsReaderId implements CmsItemId {

		private CmsItemPath path;

		public ChangesetItemToContentsReaderId(CmsItemPath path) {
			this.path = path;
		}

		@Override
		public CmsRepository getRepository() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("Method not implemented");
		}		
		
		@Override
		public Long getPegRev() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("Method not implemented");
		}

		@Override
		public CmsItemPath getRelPath() {
			return path;
		}

		@Override
		public String getLogicalId() {
			throw new UnsupportedOperationException("Method not implemented");
		}

		@Override
		public String getLogicalIdFull() {
			throw new UnsupportedOperationException("Method not implemented");
		}

		@Override
		public String getRepositoryUrl() {
			throw new UnsupportedOperationException("Method not implemented");
		}

		@Override
		public String getUrl() {
			throw new UnsupportedOperationException("Method not implemented");
		}

		@Override
		public String getUrlAtHost() {
			throw new UnsupportedOperationException("Method not implemented");
		}		
		
		@Override
		public CmsItemId withPegRev(Long arg0) {
			throw new UnsupportedOperationException("Method not implemented");
		}

		@Override
		public CmsItemId withRelPath(CmsItemPath arg0) {
			throw new UnsupportedOperationException("Method not implemented");
		}
		
	}
	
}
