/**
 * Copyright (C) 2009-2012 Simonsoft Nordic AB
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

import se.simonsoft.cms.admin.CmsContentsReader;
import se.simonsoft.cms.admin.CmsRepositoryInspection;
import se.simonsoft.cms.item.CmsConnectionException;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemLookup;
import se.simonsoft.cms.item.CmsItemNotFoundException;
import se.simonsoft.cms.item.RepoRevision;

/**
 * Uses {@link CmsRepositoryInspection} and {@link CmsContentsReader} to read {@link CmsItemAndContents}.
 */
class CmsItemAndContentsLookupAdministrative implements CmsItemLookup {
	
	private CmsRepositoryInspection repository;
	private RepoRevision revision;
	private CmsContentsReader reader;

	@Inject
	public CmsItemAndContentsLookupAdministrative(
			CmsRepositoryInspection repository,
			RepoRevision revision,
			CmsContentsReader contentsReader) {
		this.repository = repository;
		this.revision = revision;
		this.reader = contentsReader;
	}

	@Override
	public CmsItem getItem(CmsItemId idWithPath) throws CmsConnectionException,
			CmsItemNotFoundException {
		return new CmsItemAndContents(idWithPath.getRelPath(), repository, revision, reader);
	}
	
}
