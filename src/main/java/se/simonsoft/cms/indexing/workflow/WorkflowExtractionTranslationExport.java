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

import javax.enterprise.context.ApplicationScoped;

import se.repos.indexing.IndexingDoc;

@ApplicationScoped
public class WorkflowExtractionTranslationExport extends WorkflowExtraction {

	
	public void handle(WorkflowIndexingInput input, IndexingDoc fields) {
		
		WorkflowExtractionTranslationExportOptions options = (WorkflowExtractionTranslationExportOptions) deserializeOption(input.getOptions(), WorkflowExtractionTranslationExportOptions.class);
		
		fields.setField("embd_" + input.getWorkflow() + "_project", options.getProject());
		fields.setField("embd_" + input.getWorkflow() + "_pdf", options.getPdf());
		
		if (options.getDelivery() != null && options.getDelivery().getType() != null && !options.getDelivery().getType().equals("none")) {
			String name = options.getDelivery().getType(); // Fallback
			fields.setField("embd_" + input.getWorkflow() + "_delivery", options.getDelivery().getType());
			if (options.getDelivery().getParams().containsKey("name")) {
				name = options.getDelivery().getParams().get("name");
			}
			fields.setField("embd_" + input.getWorkflow() + "_delivery_name", name);
		}
		
		if (options.getProgress() != null && options.getProgress().getParams() != null) {
			if (options.getProgress().getParams().containsKey("packageurl")) {
				// TODO: Remove, not supplied by webapp 5.2.6+
				fields.setField("embd_" + input.getWorkflow() + "_packageurl", options.getProgress().getParams().get("packageurl"));				
			}
			if (options.getProgress().getParams().containsKey("packagename")) {
				fields.setField("embd_" + input.getWorkflow() + "_packagename", options.getProgress().getParams().get("packagename"));				
			}
			if (options.getProgress().getParams().containsKey("packagepath")) {
				fields.setField("embd_" + input.getWorkflow() + "_packagepath", options.getProgress().getParams().get("packagepath"));				
			}
		}
	}
	
	
}
