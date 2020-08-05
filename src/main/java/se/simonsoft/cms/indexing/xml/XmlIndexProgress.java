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

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.item.CmsRepository;

/**
 * Keeps information about the current XML indexing operation.
 * 
 * @author takesson
 *
 */
public class XmlIndexProgress {
	
	private CmsRepository repository;
	private IndexingDoc baseDoc;

	public XmlIndexProgress(CmsRepository repository, IndexingDoc baseDoc) {
		this.repository = repository;
		this.baseDoc = baseDoc;
	}

	
	public CmsRepository getRepository() {
		return this.repository;
	}


	public IndexingDoc getBaseDoc() {
		return this.baseDoc;
	}

}
