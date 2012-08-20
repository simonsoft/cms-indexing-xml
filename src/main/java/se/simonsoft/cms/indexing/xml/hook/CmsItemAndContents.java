package se.simonsoft.cms.indexing.xml.hook;

import java.io.OutputStream;

import se.simonsoft.cms.admin.CmsContentsReader;
import se.simonsoft.cms.admin.CmsRepositoryInspection;
import se.simonsoft.cms.item.Checksum;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemKind;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
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
 * TODO {@link #getContents(OutputStream)} should really declare exceptions.
 * 
 * Caches properties so they are only read once per item.
 * Contents are not cached, for now, because stream reuse
 * is complex and contents may be too big to be buffered.
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

}
