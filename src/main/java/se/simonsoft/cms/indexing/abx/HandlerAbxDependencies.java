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
package se.simonsoft.cms.indexing.abx;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IdStrategy;
import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.HandlerProperties;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.impl.CmsItemIdArg;

/**
 * Uses the abx:Dependencies property, splitting on newline, to add fields ref + refid + refurl.
 */
public class HandlerAbxDependencies implements IndexingItemHandler {

	private static final Logger logger = LoggerFactory.getLogger(HandlerAbxDependencies.class);
	
	private static final String HOSTFIELD = "repohost";

	private IdStrategy idStrategy;
	
	/**
	 * @param idStrategy to fill the refid field
	 */
	@Inject
	public HandlerAbxDependencies(IdStrategy idStrategy) {
		this.idStrategy = idStrategy;
	}
	
	@Override
	public void handle(IndexingItemProgress progress) {
		IndexingDoc fields = progress.getFields();
		String host = (String) fields.getFieldValue(HOSTFIELD);
		if (host == null) {
			throw new IllegalStateException("Depending on indexer that adds host field " + HOSTFIELD);
		}
		String abxprop = (String) fields.getFieldValue("prop_abx.Dependencies");
		if (abxprop == null) {
			return;
		}
		if (abxprop.length() == 0) {
			logger.debug("abx:Dependencies property exists but is empty");
			return;
		}
		for (String d : abxprop.split("\n")) {
			CmsItemIdArg id = new CmsItemIdArg(d);
			id.setHostname(host);

			String indexid = idStrategy.getIdHead(id);
			fields.addField("refid", indexid);
			String url = id.getUrl();
			if (id.isPegged()) {
				RepoRevision revision = new RepoRevision(id.getPegRev(), null); // ? do we need date lookup?
				fields.addField("refid", idStrategy.getId(id/*, revision*/)); // in addition to head id
				url = url + "?p=" + id.getPegRev();
			}
			
			fields.addField("refurl", url);
		}
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return new HashSet<Class<? extends IndexingItemHandler>>() {private static final long serialVersionUID = 1L;{
			add(HandlerPathinfo.class);
			add(HandlerProperties.class);
		}};
	}

}
