package se.simonsoft.cms.indexing.xml;

import javax.inject.Provider;

import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;

public interface XmlIndexWriter extends Provider<XmlIndexAddSession> {

	void deletePath(CmsRepository repository, CmsChangesetItem c);

	void onRevisionEnd(RepoRevision revision);

}
