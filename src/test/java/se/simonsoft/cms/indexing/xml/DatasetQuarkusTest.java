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

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.solr.client.solrj.SolrClient;
import org.tmatesoft.svn.core.io.SVNRepository;

import se.simonsoft.svn.runtime.SvnDatasetLoader;

public abstract class DatasetQuarkusTest {

	@Inject
	Instance<SVNRepository> repositories;

	@Inject
	SvnDatasetLoader datasetLoader;

	@Inject
	SvnDatasetRevisionEvents events;

	@Inject
	@Named("repositem")
	SolrClient repositem;

	@Inject
	@Named("reposxml")
	SolrClient reposxml;

	protected void loadDataset(String datasetPath) {
		try {
			events.clear();
			repositem.deleteByQuery("*:*");
			repositem.commit();
			reposxml.deleteByQuery("*:*");
			reposxml.commit();
			repositories.get();
			datasetLoader.load(datasetPath);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load test dataset '" + datasetPath + "'", e);
		}
	}
}
