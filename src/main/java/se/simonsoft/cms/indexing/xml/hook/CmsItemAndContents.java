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

import java.io.InputStream;
import java.io.OutputStream;

import se.simonsoft.cms.item.Checksum;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemKind;
import se.simonsoft.cms.item.CmsItemLock;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.inspection.CmsContentsReader;
import se.simonsoft.cms.item.inspection.CmsRepositoryInspection;
import se.simonsoft.cms.item.properties.CmsItemProperties;

/**
 * Used to isolate content fetching from indexing logic.
 * 
 * Transitional until we decide on the API for reading item's contents.
 * Could be merged either to CmsItem or to CmsChangesetItem,
 * or {@link CmsContentsReader} could be kept.
 * 
 * The benefit of this compared to {@link CmsContentsReader} is that
 * implementation don't need to require {@link CmsRepositoryInspection} availability.
 * 
 * For normal, authenticated, repository access this is not necessary
 * because a simple REST/HTTP client can fetch content from svn URLs, any rev.
 * 
 * Caches properties so they are only read once per item.
 * Contents are not cached, for now, because stream reuse
 * is complex and contents may be too big to be buffered.
 * 
 * @deprecated CmsItem is now updated to support contents reading so we can change signatures for indexing
 */
class CmsItemAndContents implements CmsItem {

	private CmsItemPath path;
	private CmsRepositoryInspection repo;
	private CmsContentsReader reader;
	private RepoRevision revision;
	
	private Id id;
	
	// We always have revision when reading props so results can be cached
	private transient CmsItemProperties props = null;

	public CmsItemAndContents(CmsItemPath path, CmsRepositoryInspection repo, RepoRevision revision, CmsContentsReader reader) {
		this.path = path;
		this.repo = repo;
		this.revision = revision;
		this.reader = reader;
		this.id = new Id();
	}
	
	@Override
	public CmsItemId getId() {
		return id;
	}
	
	@Override
	public void getContents(OutputStream output) {
		reader.getContents(repo, revision, path, output);
	}
	
	@Override
	public CmsItemProperties getProperties() {
		if (props == null) {
			props = reader.getProperties(repo, revision, path);
		} 
		return props;
	}
	
	@Override
	public Checksum getChecksum() {
		throw new UnsupportedOperationException("Method not implemented");
	}

	@Override
	public String getStatus() {
		throw new UnsupportedOperationException("Method not implemented");
	}

	@Override
	public CmsItemLock getLock() {
		throw new UnsupportedOperationException("Method not implemented");
	}
	
	class Id implements CmsItemId {

		@Override
		public CmsRepository getRepository() {
			return repo;
		}
		
		@Override
		public Long getPegRev() {
			return revision.getNumber();
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

	@Override
	public CmsItemKind getKind() {
		throw new UnsupportedOperationException("Method not implemented");
	}

	@Override
	public RepoRevision getRevisionChanged() {
		throw new UnsupportedOperationException("Method not implemented");
	}

}
