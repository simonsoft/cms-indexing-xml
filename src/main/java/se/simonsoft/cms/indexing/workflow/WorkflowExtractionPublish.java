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
package se.simonsoft.cms.indexing.workflow;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.enterprise.context.ApplicationScoped;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.indexing.abx.HandlerReleaseLabel;
import se.simonsoft.cms.item.structure.CmsLabelVersion;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobManifest;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;

@ApplicationScoped
public class WorkflowExtractionPublish extends WorkflowExtraction {

	
	public void handle(WorkflowIndexingInput input, IndexingDoc fields) {
		
		PublishJobOptions options = (PublishJobOptions) deserializeOption(input.getOptions(), PublishJobOptions.class);
		
		handlePublish(input, options, fields);
	}
	
	void handlePublish(WorkflowIndexingInput input, PublishJobOptions options, IndexingDoc fields) {
		// Focus on fields available in the manifest in order to support reindexing.
		PublishJobManifest manifest = options.getManifest();
		
		// Indexing full manifest.job object.  
		handleManifestMap("embd_" + input.getWorkflow() + "_job", manifest.getJob(), fields);
		
		if (manifest.getDocument() != null) {
			// #1438 Handle multiple abbreviated versions.
			// The abbreviated versions are in custom.versionN  
			// Likely a webapp handler that makes queries against these SolR documents with sortable ReleaseLabel
			
			// #1592 Normalize versionrelease for string sorting in SolR.
			// Ensure no SolR field analysis by using the same field as type:file indexing.
			// Field analysis will ruin the careful string sorting.
			String rl = manifest.getDocument().get("versionrelease");
			CmsLabelVersion l = new CmsLabelVersion(rl);
			fields.setField(HandlerReleaseLabel.RELEASELABEL_META_FIELD + "_sort", l.getLabelSort());
			
			handleManifestMap("embd_" + input.getWorkflow() + "_document", manifest.getDocument(), fields);
		}
		
		if (manifest.getCustom() != null) {
			handleManifestMap("embd_" + input.getWorkflow() + "_custom", manifest.getCustom(), fields);
		}
		// Currently ignoring "meta" since it is intended for the target system.
		
		
		// Require progress fields if this is a CDN workflow.
		LinkedHashMap<String, String> progressParams = options.getProgress().getParams();
		if (isCdn(input) && (progressParams == null || progressParams.isEmpty())) {
			throw new IllegalStateException("CDN extraction requires progress information from unzip");
		}
		
		// Extract the CDN-specifics (all fields in 'progress')
		// This can NOT be restored during reindexing, requires re-delivery to CDN. 
		handleManifestMap("embd_" + input.getWorkflow() + "_progress", progressParams, fields);
	}
	
	
	void handleManifestMap(String prefix, Map<String, String> map, IndexingDoc fields) {
		if (map == null) {
			return;
		}
		for (Entry<String, String> e: map.entrySet()) {
			fields.setField(prefix + "_" + e.getKey(), e.getValue());			
		}
	}

	boolean isCdn(WorkflowIndexingInput input) {
		return "publish-cdn".equals(input.getWorkflow());	
	}
	
}
