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

public class HandlerPathareaFromProperties implements
		IndexingItemHandler {

	private static final String RELEASE_VAL = "release";
	private static final String TRANSLATION_VAL = "translation";
	private static final String TRANSLATION10_VAL = "translation10";
	
	private static final String RELEASELABEL_FIELD = "prop_abx.ReleaseLabel";
	private static final String TRANSLATIONLOCALE_FIELD = "prop_abx.TranslationLocale";
	
	private static final String AUTHORMASTER_FIELD = "prop_abx.AuthorMaster";
	private static final String RELEASEMASTER_FIELD = "prop_abx.ReleaseMaster";
	private static final String TRANSLATIONMASTER_FIELD = "prop_abx.TranslationMaster";

	@Override
	public void handle(IndexingItemProgress progress) {
		IndexingDoc doc = progress.getFields();
		if (doc.containsKey(TRANSLATIONMASTER_FIELD) || doc.containsKey(TRANSLATIONLOCALE_FIELD)) {
			doc.setField("pathmain", false);
			if (doc.containsKey(AUTHORMASTER_FIELD) || doc.containsKey(TRANSLATIONLOCALE_FIELD)) 
				doc.addField("patharea", TRANSLATION_VAL);
			else
				doc.addField("patharea", TRANSLATION10_VAL); // In CMS 1.0, there were neither AM or TL fields.
			
		} else if (doc.containsKey(RELEASEMASTER_FIELD) || doc.containsKey(RELEASELABEL_FIELD)) {
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
