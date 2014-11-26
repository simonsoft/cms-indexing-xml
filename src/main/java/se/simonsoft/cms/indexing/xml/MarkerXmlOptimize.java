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
package se.simonsoft.cms.indexing.xml;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrServer;

import se.repos.indexing.solrj.MarkerOptimizeSolrj;

public class MarkerXmlOptimize extends MarkerOptimizeSolrj {

	@Inject
	public MarkerXmlOptimize(@Named("reposxml") SolrServer core) {
		// Optimize can take significant time (have seen above 30min).
		// Ideally perform optimize during night time.
		// Currently uses the same interval as repositem core.
		super(core);
	}
	
}
