/**
 * Copyright (C) 2009-2017 Simonsoft Nordic AB
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNRepository;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import se.simonsoft.svn.runtime.RepoId;
import se.simonsoft.svn.runtime.SvnDumpConfig;
import se.simonsoft.svn.runtime.SvnRevisionAvailableEvent;

@QuarkusTest
@TestProfile(SvnDatasetLoadQuarkusTest.Profile.class)
public class SvnDatasetLoadQuarkusTest {

	@Inject
	Instance<SVNRepository> repositories;

	@Inject
	SvnDatasetRevisionEvents events;

	@Test
	@ActivateRequestContext
	public void testLoadDatasetIntoSvnDevService() throws Exception {
		events.clear();

		SVNRepository repository = repositories.get();

		assertEquals(2L, repository.getLatestRevision());
		assertEquals(SVNNodeKind.FILE, repository.checkPath("test1.xml", 2));
		assertEquals(List.of(SvnDatasetRepoIdProducer.REPO_ID + " 1", SvnDatasetRepoIdProducer.REPO_ID + " 2"), events.revisions());
	}

	public static class Profile implements QuarkusTestProfile {

		@Override
		public Map<String, String> getConfigOverrides() {
			return Map.of(
					SvnDumpConfig.DATASET_PATH, "se/simonsoft/cms/indexing/xml/datasets/tiny-inline");
		}
	}
}

@ApplicationScoped
class SvnDatasetRepoIdProducer {

	static final String REPO_ID = "cms-indexing-xml-dataset";

	@Produces
	@RepoId
	public String produceRepoId() {
		return REPO_ID;
	}
}

@ApplicationScoped
class SvnDatasetRevisionEvents {

	private final List<String> revisions = new ArrayList<>();

	void onRevisionAvailable(@Observes SvnRevisionAvailableEvent event) {
		revisions.add(event.repoId() + " " + event.revision());
	}

	void clear() {
		revisions.clear();
	}

	List<String> revisions() {
		return List.copyOf(revisions);
	}
}
