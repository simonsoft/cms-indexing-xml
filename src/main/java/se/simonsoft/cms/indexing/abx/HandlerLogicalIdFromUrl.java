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

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.HandlerProperties;
import se.repos.indexing.item.IndexingItemProgress;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.impl.CmsItemIdArg;

/**
 * Without cms-logicalid module we can't encode a logical id based on {@link CmsChangesetItem},
 * so we'll use the URL as specified by repos-indexing, using the assumption that the URL
 * is encoded according to Subversion/SvnKit standards.
 */
public class HandlerLogicalIdFromUrl extends HandlerLogicalId {
	
	public static final String URL_REPO_FIELD = "repourl";
	public static final String URL_ITEM_FIELD = "url";

	
	@Override
	protected CmsItemId getItemId(IndexingItemProgress progress) {
		CmsChangesetItem item = progress.getItem();
		String repourl = (String) progress.getFields().getFieldValue(URL_REPO_FIELD);
		if (repourl == null) {
			throw new AssertionError("Missing " + URL_REPO_FIELD + " field for " + item + ", can not set logical ID");
		}
		String url = (String) progress.getFields().getFieldValue(URL_ITEM_FIELD);
		if (url == null) {
			throw new AssertionError("Missing " + URL_ITEM_FIELD + " field for " + item + ", can not set logical ID");
		}

		CmsItemIdArg id = new CmsItemIdArg(new CmsRepository(repourl), url, item.getRevisionChanged().getNumber());
		if (!id.isFullyQualifiedOriginally()) {
			throw new AssertionError("Missing host information for " + item + ", can not set logical ID");
		}
		return id;
	}
	
	@SuppressWarnings("serial")
	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return new HashSet<Class<? extends IndexingItemHandler>>() {{
			add(HandlerPathinfo.class);
			add(HandlerProperties.class);
		}};
	}

}
