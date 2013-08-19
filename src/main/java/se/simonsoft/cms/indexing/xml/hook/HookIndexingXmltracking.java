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
package se.simonsoft.cms.indexing.xml.hook;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.PostCommitEventListener;
import se.simonsoft.cms.item.inspection.CmsRepositoryInspection;

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
