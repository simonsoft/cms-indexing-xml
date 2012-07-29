package se.simonsoft.cms.admin;

import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangeset;

public interface CmsChangesetReader {

	CmsChangeset read(CmsRepositoryInspection repository, RepoRevision revision);
	
}
