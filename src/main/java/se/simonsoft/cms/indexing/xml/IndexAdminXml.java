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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexAdmin;
import se.repos.indexing.IndexAdminNotification;
import se.repos.indexing.solrj.SolrCommit;
import se.repos.indexing.solrj.SolrDeleteByQuery;
import se.repos.indexing.solrj.SolrOptimize;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.indexing.IdStrategy;

/**
 * Don't forget to bind (<i>bind(IndexAdminXml.class).asEagerSingleton();</i>) this one, or clear won't affect reposxml core.
 */
//@Singleton // only one should be bound as listener to central IndexAdmin
// Causes issues with Quarkus CDI when declaring Singleton.
// Using direct curl for clear since introduction of chef installer (potentially earlier).
// Not sure how this can be a Singleton in a multi-repo executable.
public class IndexAdminXml extends IndexAdminNotification implements IndexAdmin {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private SolrClient reposxml;
	private String query;

	@Inject
	public IndexAdminXml(CmsRepository repository, IdStrategy idStrategy, @Named("reposxml") SolrClient core) {
		this.query = "repoid:\"" + idStrategy.getIdRepository(repository).replace("\"", "\\\"") + '"';
		this.reposxml = core;
	}

	@Override
	public void clear() {
		logger.info("Clearing xml using query {} in {}", query, reposxml);
		new SolrDeleteByQuery(reposxml, query).run();
		new SolrCommit(reposxml, false).run();
		new SolrOptimize(reposxml).run();
	}

}
