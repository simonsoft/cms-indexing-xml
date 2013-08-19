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
package se.simonsoft.cms.indexing.xml.hook;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import se.simonsoft.xmltracking.index.add.IdStrategy;
import se.simonsoft.xmltracking.source.XmlSourceElement;

/**
 * Predictable IDs, supporting overwrite/reindexing, using {@link IndexingContext}
 * to get repository name + revision + item and adding element number increment.
 */
public class IdStrategyRepoRevisionItemElem implements IdStrategy {

	private IndexingContext context;
	
	private transient int n = Integer.MIN_VALUE;
	private String doc = null;
	
	private Map<XmlSourceElement, String> assigned;
	
	@Inject
	public IdStrategyRepoRevisionItemElem(IndexingContext indexingContext) {
		this.context = indexingContext;
	}
	
	@Override
	public void start() {
		n = 0;
		assigned = new HashMap<XmlSourceElement, String>();
		doc = context.getRepository().getName() + "^" + context.getItemPath() + "?p=" + context.getRevision().getNumber();
	}

	@Override
	public String getElementId(XmlSourceElement element) {
		if (doc == null) {
			throw new IllegalStateException("Id strategy not initialized with a document id, start method must be called for each item");
		}
		if (assigned.containsKey(element)) {
			return assigned.get(element);
		}
		String id = doc + "#" + n++;
		assigned.put(element, id);
		return id;
	}

}
