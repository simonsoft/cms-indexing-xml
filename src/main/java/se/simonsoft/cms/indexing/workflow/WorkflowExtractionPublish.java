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

import java.util.Map;
import java.util.Map.Entry;

import javax.enterprise.context.ApplicationScoped;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobManifest;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;

@ApplicationScoped
public class WorkflowExtractionPublish extends WorkflowExtraction {

	
	public void handle(WorkflowIndexingInput input, IndexingDoc fields) {
		
		PublishJobOptions options = (PublishJobOptions) deserializeOption(input.getOptions(), PublishJobOptions.class);
		
		// Focus on fields available in the manifest in order to support reindexing.
		PublishJobManifest manifest = options.getManifest();
		
		fields.setField("embd_" + input.getWorkflow() + "_format", manifest.getJob().get("format"));
		if (manifest.getJob().containsKey("profiling")) {
			fields.setField("embd_" + input.getWorkflow() + "_profiling", manifest.getJob().get("profiling"));
		}
		fields.setField("embd_" + input.getWorkflow() + "_start", manifest.getJob().get("start"));
		fields.setField("embd_" + input.getWorkflow() + "_topics", manifest.getJob().get("topics"));
		
		
		if (manifest.getDocument() != null) {
			// TODO: #1438 Handle multiple abbreviated versions.
			
			// TODO: #1592 Normalize versionrelease for string sorting in SolR.
			
			handleManifestMap("embd_" + input.getWorkflow() + "_document", manifest.getDocument(), fields);
		}
		
		if (manifest.getCustom() != null) {
			handleManifestMap("embd_" + input.getWorkflow() + "_custom", manifest.getCustom(), fields);
		}
		
		// Currently ignoring "meta" since it is intended for the target system.
	}
	
	
	private void handleManifestMap(String prefix, Map<String, String> map, IndexingDoc fields) {
		for (Entry<String, String> e: map.entrySet()) {
			fields.setField(prefix + "_" + e.getKey(), e.getValue());			
		}
	}
	
	
}
