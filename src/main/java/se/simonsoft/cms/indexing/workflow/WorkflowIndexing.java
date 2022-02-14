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
import java.util.regex.Pattern;

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

@ApplicationScoped
public class WorkflowIndexing {

	@Inject
	@Named("repositem")
	SolrClient solrCore;	
	
	@Inject
	WorkflowExtractionTranslationExport extractionTranslationExport;
	
	@Inject
	WorkflowExtractionPublish extractionPublish;
	
	@Inject
	IdStrategy idStrategy;
	
	private Pattern uuid = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
	private Logger logger = LoggerFactory.getLogger(WorkflowIndexing.class); 
	
	public void index(WorkflowIndexingInput input) {
		IndexingDoc fields = new IndexingDocIncrementalSolrj();
		
		logger.info("Index workflow: {} {}", input.getWorkflow(), input.getExecutionId());
		SolrPingOp solrPing = new SolrPingOp(solrCore);
		solrPing.run();

		extractCommonFields(input, fields);
		if ("translationexport".equals(input.getWorkflow())) {
			extractionTranslationExport.handle(input, fields);
		} else if ("publish".equals(input.getWorkflow())) {
			extractionPublish.handle(input, fields);
		} else {
			throw new IllegalArgumentException("Unknown workflow: " + input.getWorkflow());
		}
		
		logger.info("Workflow indexing adding: id={} workflow={}", fields.getFieldValue("id"), input.getWorkflow());
		SolrAdd solrAdd = new SolrAddCommitWithin(solrCore, fields);
		solrAdd.run();
		logger.info("Workflow indexing added : id={} workflow={}", fields.getFieldValue("id"), input.getWorkflow());
	}

	
	void extractCommonFields(WorkflowIndexingInput input, IndexingDoc d) {
		String[] executionArn = input.getExecutionId().split(":");
		String executionUuid = executionArn[executionArn.length - 1];
		if (!uuid.matcher(executionUuid).matches()) {
			throw new IllegalArgumentException("Workflow indexing requires field executionid ending with UUID: " + input.getExecutionId());
		}
		
		if (input.getItemId() == null) {
			// TODO: Remove workaround
			//throw new IllegalArgumentException("Workflow indexing requires field itemid: " + input.getExecutionId());
			input.setItemId(new CmsItemIdArg("x-svn://ubuntu-cheftest1.pdsvision.net/svn/demo1"));
		}
		CmsRepository repository = input.getItemId().getRepository();
		CmsItemPath path = input.getItemId().getRelPath();
		Long rev = input.getItemId().getPegRev();
		
		if (rev == null) {
			throw new IllegalArgumentException("Workflow 'itemid' must specify revision: " + input.getItemId());
		}
		
		d.setField("id", idStrategy.getIdRepository(repository) + "#" + executionUuid);

		d.setField("type", input.getWorkflow());
		
		// See HandlerPathinfo
		d.setField("repo", repository.getName());
		d.setField("repoparent", repository.getParentPath());
		d.setField("repohost", repository.getHost());
		d.setField("repoid", idStrategy.getIdRepository(repository));
		
		// Setting 'path' might cause these docs to turn up in cms-reporting (likely resulting in exception due to missing item fields).
		// The field can be workflow-specific because the WorkflowExecutionStatus instances are per-workflow/statemachine.
		if (path != null) {
			d.addField("embd_" + input.getWorkflow() + "_path", path.getPath());
		}		
		
		d.setField("rev", rev);
		d.setField("revauthor", input.getUserId());
		d.setField("t", new Date());
		d.setField("complete", input.isComplete());

		// Must be provided by publish reindex code based on manifest.job.configname.
		// configname is available as input root attribute initially, always in manifest.
		if (input.getConfigname() != null) {
			d.addField("embd_" + input.getWorkflow() + "_configname", input.getConfigname());
		}
			
		d.addField("embd_" + input.getWorkflow() + "_executionid", input.getExecutionId());
		d.addField("embd_" + input.getWorkflow() + "_uuid", executionUuid);
		d.addField("embd_" + input.getWorkflow() + "_status", input.getStatus());
		if (input.getError() != null && !input.getError().isBlank()) {
			d.addField("text_error", input.getError());
		}
		
	}
	
}
