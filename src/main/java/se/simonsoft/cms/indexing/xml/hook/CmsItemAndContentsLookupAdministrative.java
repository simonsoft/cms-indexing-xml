package se.simonsoft.cms.indexing.xml.hook;

import javax.inject.Inject;

import se.simonsoft.cms.admin.CmsContentsReader;
import se.simonsoft.cms.admin.CmsRepositoryInspection;
import se.simonsoft.cms.item.CmsConnectionException;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemLookup;
import se.simonsoft.cms.item.CmsItemNotFoundException;
import se.simonsoft.cms.item.RepoRevision;

/**
 * Uses {@link CmsRepositoryInspection} and {@link CmsContentsReader} to read {@link CmsItemAndContents}.
 */
class CmsItemAndContentsLookupAdministrative implements CmsItemLookup {
	
	private CmsRepositoryInspection repository;
	private RepoRevision revision;
	private CmsContentsReader reader;

	@Inject
	public CmsItemAndContentsLookupAdministrative(
			CmsRepositoryInspection repository,
			RepoRevision revision,
			CmsContentsReader contentsReader) {
		this.repository = repository;
		this.revision = revision;
		this.reader = contentsReader;
	}

	@Override
	public CmsItem getItem(CmsItemId idWithPath) throws CmsConnectionException,
			CmsItemNotFoundException {
		return new CmsItemAndContents(idWithPath.getRelPath(), repository, revision, reader);
	}
	
}
