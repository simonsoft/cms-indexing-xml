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
public class WorkflowExtractionWork extends WorkflowExtraction {

	
	public void handle(WorkflowIndexingInput input, IndexingDoc fields) {
		
		// Workflow cms-work-v1 does not forwards 'options' because some execution actions do not have options.
		//WorkflowExtractionTranslationExportOptions options = (WorkflowExtractionTranslationExportOptions) deserializeOption(input.getOptions(), WorkflowExtractionTranslationExportOptions.class);
		
		fields.setField("embd_" + input.getWorkflow() + "_action", input.getAction());
	}
	
	
}
