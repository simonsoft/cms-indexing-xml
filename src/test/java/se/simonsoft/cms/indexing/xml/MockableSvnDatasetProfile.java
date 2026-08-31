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

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.quarkus.test.junit.QuarkusTestProfile;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import se.simonsoft.svn.runtime.RepoId;
import se.simonsoft.svn.runtime.SvnDumpConfig;

public class MockableSvnDatasetProfile implements QuarkusTestProfile {

	@Override
	public Map<String, String> getConfigOverrides() {
		// A configured dataset creates the normal-scoped bean that tests replace with QuarkusMock.
		return Map.of(
				SvnDumpConfig.DATASET_PATH, "se/simonsoft/cms/indexing/xml/datasets/tiny-inline");
	}

	@Override
	public Set<Class<?>> getEnabledAlternatives() {
		return Set.of(MockableDatasetRepoIdProducer.class);
	}
}

@Alternative
@RequestScoped
class MockableDatasetRepoIdProducer {

	// Tests sharing this profile reuse Dev Services but load their dataset into separate repositories.
	private final String repoId = "test-" + UUID.randomUUID();

	@Produces
	@RepoId
	public String produceRepoId() {
		return repoId;
	}
}
