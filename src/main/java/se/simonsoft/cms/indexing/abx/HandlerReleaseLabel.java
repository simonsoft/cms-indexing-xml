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
package se.simonsoft.cms.indexing.abx;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.simonsoft.cms.item.structure.CmsLabelVersion;
import se.repos.indexing.item.HandlerProperties;

public class HandlerReleaseLabel implements
		IndexingItemHandler {

	private static final Logger logger = LoggerFactory.getLogger(HandlerReleaseLabel.class);
	
	private static final String RELEASELABEL_PROP_FIELD = "prop_abx.ReleaseLabel";

	public static final String RELEASELABEL_META_FIELD = "meta_s_s_releaselabel";
	
	
	
	@Override
	public void handle(IndexingItemProgress progress) {
		IndexingDoc doc = progress.getFields();
		String rl = (String) doc.getFieldValue(RELEASELABEL_PROP_FIELD);
		
		if (rl != null && !rl.isBlank()) {
			extractReleaseLabel(progress.getFields(), rl, RELEASELABEL_META_FIELD);
		}
		
	}

	public static void extractReleaseLabel(IndexingDoc f, String rl, String prefix) {
		
		try {
			CmsLabelVersion l = new CmsLabelVersion(rl);
			f.setField(prefix, l.getLabel());
			f.setField(prefix + "_sort", l.getLabelSort());
			
			
			{ // Version
				List<String> segOrig = l.getVersionSegments();
				List<String> segSort = l.getVersionSegmentsSort();
				for (int pos = 0; pos < segOrig.size(); pos++) {
					f.setField(prefix + "_version" + Integer.toString(pos), segOrig.get(pos));
					f.setField(prefix + "_version" + Integer.toString(pos) + "_sort", segSort.get(pos));
				}
			}
			
			{ // Prerelease
				List<String> segOrig = l.getPrereleaseSegments();
				List<String> segSort = l.getPrereleaseSegmentsSort();
				for (int pos = 0; pos < l.getSegments().size(); pos++) {
					f.setField(prefix + "_prerelease" + Integer.toString(pos), segOrig.get(pos));
					f.setField(prefix + "_prerelease" + Integer.toString(pos) + "_sort", segSort.get(pos));
				}
			}
			
		} catch (Exception e) {
			String msg = MessageFormatter.format("Failed to extract ReleaseLabel: {}", e.getMessage()).getMessage();
			logger.warn(msg);
			logger.debug("Failed to extract ReleaseLabel: {}", e);
			f.addField("text_error", msg); // multi-value
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
