package se.simonsoft.cms.indexing.xml.hook;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.admin.CmsRepositoryInspection;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.PostCommitEventListener;

/**
 * Ties xmltracking indexing into the hook framework.
 * Currently easy because there are no other hook users.
 */
public class HookIndexingXmltracking implements PostCommitEventListener {

	private static final Logger logger = LoggerFactory.getLogger(HookIndexingXmltracking.class);
	
	private ChangesetIteration changesetHandler;

	@Inject
	public void setChangesetHandler(ChangesetIteration changesetHandler) {
		this.changesetHandler = changesetHandler;
	}
	
	@Override
	public void onPostCommit(CmsRepository repository, RepoRevision revision) {
		if (repository instanceof CmsRepositoryInspection) {
			onPostCommit((CmsRepositoryInspection) repository, revision);
		} else {
			throw new IllegalArgumentException("Expected repository admin instance");
		}
	}
	
	public void onPostCommit(CmsRepositoryInspection repository, RepoRevision revision) {
		logger.info("Starting xmltracking indexing for {}@{}", repository, revision.getNumber());
		changesetHandler.onHook(repository, revision);
	}

}
