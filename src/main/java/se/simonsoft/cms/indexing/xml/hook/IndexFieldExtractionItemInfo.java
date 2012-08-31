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

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.xmltracking.index.add.IndexFieldExtraction;
import se.simonsoft.xmltracking.index.add.IndexFields;
import se.simonsoft.xmltracking.source.XmlSourceElement;

/**
 * Adds CMS item identification fields to XML indexing docs.
 * 
 * No dependencies to other fields.
 * 
 * Part of the CMS2 strategy for mapping search results to a {@link CmsItemId} instance
 * (see ticket:401 and ticket:406).
 */
public class IndexFieldExtractionItemInfo implements
		IndexFieldExtraction {

	private IndexingContext context;

	@Inject
	public IndexFieldExtractionItemInfo(IndexingContext indexingContext) {
		this.context = indexingContext;
	}
	
	@Override
	public void extract(IndexFields fields, XmlSourceElement processedElement) {
		// can these be the common ID field names?
		CmsItemPath path = context.getItemPath();
		RepoRevision rev = context.getRevision();
		CmsRepository repo = context.getRepository();		
		fields.addField("path", path.getPath());
		fields.addField("pathname", path.getName());
		CmsItemPath parent = path.getParent();
		fields.addField("pathdir", parent == null ? "" : parent.getPath());
		fields.addField("pathext", path.getExtension());
		fields.addField("rev", rev.getNumber());
		fields.addField("revt", rev.getDate().getTime());
		fields.addField("repo", repo.getName());
		fields.addField("repoparent", repo.getParentPath());
		if (repo.isHostKnown()) {
			fields.addField("repohost", repo.getHost());
		}
	}

}
