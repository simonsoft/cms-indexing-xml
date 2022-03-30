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

import java.util.Date;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.solrj.SolrAdd;
import se.repos.indexing.solrj.SolrAddCommitWithin;
import se.repos.indexing.solrj.SolrPingOp;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.cms.item.workflow.WorkflowExecutionId;

@ApplicationScoped
public class WorkflowIndexing {

	@Inject
	@Named("repositem")
	SolrClient solrCore;	
	
	@Inject
	WorkflowExtractionWork extractionWork;
	
	@Inject
	WorkflowExtractionTranslationExport extractionTranslationExport;
	
	@Inject
	WorkflowExtractionPublish extractionPublish;
	
	@Inject
	IdStrategy idStrategy;
	
	
	private Logger logger = LoggerFactory.getLogger(WorkflowIndexing.class); 
	
	public void index(WorkflowIndexingInput input) {
		IndexingDocIncrementalSolrj fields = new IndexingDocIncrementalSolrj();
		
		logger.info("Index workflow: {} {}", input.getWorkflow(), input.getExecutionId());
		SolrPingOp solrPing = new SolrPingOp(solrCore);
		solrPing.run();

		if (input.getItemId() == null) {
			logger.warn("Workflow indexing processing event: {}", input.getStatus());
			extractAbortEvent(input, fields);
		} else {
			extractCommonFields(input, fields);
			if ("work".equals(input.getWorkflow())) {
				extractionWork.handle(input, fields);
			} else if ("translationexport".equals(input.getWorkflow())) {
				extractionTranslationExport.handle(input, fields);
			} else if ("publish".equals(input.getWorkflow())) {
				extractionPublish.handle(input, fields);
			} else if ("publish-cdn".equals(input.getWorkflow())) {
				extractionPublish.handle(input, fields);
			} else {
				throw new IllegalArgumentException("Unknown workflow: " + input.getWorkflow());
			}
		}
		
		logger.info("Workflow indexing sending: id={} workflow={}", fields.getFieldValue("id"), input.getWorkflow());
		SolrAdd solrAdd = new SolrAddCommitWithin(solrCore, fields);
		solrAdd.run();
		logger.info("Workflow indexing sent   : id={} workflow={}", fields.getFieldValue("id"), input.getWorkflow());
	}

	
	void extractCommonFields(WorkflowIndexingInput input, IndexingDoc d) {
		WorkflowExecutionId executionId = new WorkflowExecutionId(input.getExecutionId());
		if (!executionId.hasUuid()) {
			throw new IllegalArgumentException("Workflow indexing requires field executionid ending with UUID: " + input.getExecutionId());
		}
		
		if (input.getItemId() == null) {
			throw new IllegalArgumentException("Workflow indexing requires field itemid: " + input.getExecutionId());
		}
		CmsRepository repository = input.getItemId().getRepository();
		CmsItemPath path = input.getItemId().getRelPath();
		Long rev = input.getItemId().getPegRev();
		
		if (rev == null) {
			throw new IllegalArgumentException("Workflow 'itemid' must specify revision: " + input.getItemId());
		}
		
		// Using only the UUID, allows update based on abort event where itemid/repository is not known.
		d.setField("id", executionId.getUuid());

		d.setField("type", input.getWorkflow());
		
		// See HandlerPathinfo
		d.setField("repo", repository.getName());
		d.setField("repoparent", repository.getParentPath());
		d.setField("repohost", repository.getHost());
		d.setField("repoid", idStrategy.getIdRepository(repository));
		
		// Setting 'path' might cause these docs to turn up in cms-reporting (likely resulting in exception due to missing item fields).
		// The field can be workflow-specific because the WorkflowExecutionStatus instances are per-workflow/statemachine.
		if (path != null) {
			d.setField("embd_" + input.getWorkflow() + "_path", path.getPath());
		}		
		
		d.setField("rev", rev);
		d.setField("revauthor", input.getUserId());
		d.setField("t", new Date());
		d.setField("complete", input.isComplete());

		// Must be provided by publish reindex code based on manifest.job.configname.
		// configname is available as input root attribute initially, always in manifest.
		if (input.getConfigname() != null) {
			d.setField("embd_" + input.getWorkflow() + "_configname", input.getConfigname());
		}
		
		d.setField("embd_" + input.getWorkflow() + "_executionid", input.getExecutionId()); // Might be same as uuid after reindexing (from publish manifest).
		d.setField("embd_" + input.getWorkflow() + "_uuid", executionId.getUuid());
		d.setField("embd_" + input.getWorkflow() + "_status", input.getStatus());
		// TODO: Consider additional fields for error-code and error-cause.
		if (input.getError() != null && !input.getError().isBlank()) {
			d.setField("text_error", input.getError());
		}
	}
	
	void extractAbortEvent(WorkflowIndexingInput input, IndexingDocIncrementalSolrj d) {
		// Assuming the start event has been indexed so we can make a status update without having the itemId.
		
		WorkflowExecutionId executionId = new WorkflowExecutionId(input.getExecutionId());
		if (!executionId.hasUuid()) {
			throw new IllegalArgumentException("Workflow indexing requires field executionid ending with UUID: " + input.getExecutionId());
		}
		
		if ("ABORTED".equals(input.getStatus()) || "TIMED_OUT".equals(input.getStatus())) {
			// Set id before enabling updateMode.
			d.setField("id", executionId.getUuid());

			d.setUpdateMode(true);
			d.setField("complete", true);
			d.setField("embd_" + input.getWorkflow() + "_status", input.getStatus().toLowerCase());
			d.setField("text_error", "Workflow terminated before completion: " + input.getStatus().toLowerCase());
		
		} else {
			throw new IllegalArgumentException("Workflow Abort event unknown status: " + input.getStatus());
		}
	}
	
}
