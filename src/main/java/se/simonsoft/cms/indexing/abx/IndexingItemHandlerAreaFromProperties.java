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

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.HandlerProperties;

public class IndexingItemHandlerAreaFromProperties implements
		IndexingItemHandler {

	private static final String RELEASE_VAL = "release";
	private static final String TRANSLATION_VAL = "translation";
	private static final String RELEASE_FIELD = "prop_abx.ReleaseLabel";
	private static final String TRANSLATION_FIELD = "prop_abx.TranslationLocale";

	@Override
	public void handle(IndexingItemProgress progress) {
		IndexingDoc doc = progress.getFields();
		if (doc.containsKey(TRANSLATION_FIELD)) {
			doc.addField("patharea", TRANSLATION_VAL);
			doc.setField("pathmain", false);
		} else if (doc.containsKey(RELEASE_FIELD)) {
			doc.addField("patharea", RELEASE_VAL);
			doc.setField("pathmain", false);
		} else {
			doc.setField("pathmain", true);
		}
	}

	@SuppressWarnings("serial")
	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return new HashSet<Class<? extends IndexingItemHandler>>() {{
			add(HandlerProperties.class);
		}};
	}

}
