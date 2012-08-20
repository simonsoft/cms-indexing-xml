package se.simonsoft.cms.indexing.xml.hook;

import javax.inject.Singleton;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.properties.CmsItemProperties;

/**
 * Provides access to current item during chamgeset processing.
 * 
 * A single instance could be reused if all indexing operations
 * are done from the same changeset iteration; a challenging design
 * but a reasonable goal of hook execution when considering performance.
 * 
 * This impl is compatible with single thread execution for hook,
 * for parallell execution or indexing from other places than hooks
 * (such as reindexing using service layer) thread local storage would be needed.
 * 
 * Values must be set as start of each item's processing.
 */
@Singleton
public class IndexingContext {

	private CmsRepository repository;
	private RepoRevision revision;
	private CmsItem item;

	public RepoRevision getRevision() {
		return revision;
	}
	
	/**
	 * @return quite possibly with only name and parent path known
	 */
	public CmsRepository getRepository() {
		return repository;
	}
	
	/**
	 * @return when needed we can probably get a CmsItemId here because it will be needed anyway during indexing
	 */
	public CmsItemPath getItemPath() {
		return item.getId().getRelPath();
	}

	/**
	 * @return properties backed by fast or cached access, called from multiple indexing handlers
	 */
	public CmsItemProperties getItemProperties() {
		return item.getProperties();
	}
	
	void setRevision(RepoRevision revision) {
		this.revision = revision;
	}
	
	void setRepository(CmsRepository repository) {
		// TODO convert from CmsRepositoryInspection to CmsRepository to enforce security
		// and at the same time change the hook interface to pass CmsRepositoryInspection
		this.repository = repository;
	}
	
	/**
	 * @param item Should maybe be a CmsItemId
	 */
	void setItem(CmsItem item) {
		this.item = item;
	}
	
}
