package se.simonsoft.cms.indexing.workflow;

import java.util.LinkedHashMap;

import javax.enterprise.context.ApplicationScoped;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;

@ApplicationScoped
public class WorkflowExtractionPublishCdn extends WorkflowExtractionPublish {
	
	@Override
	public void handle(WorkflowIndexingInput input, IndexingDoc fields) {
		
		PublishJobOptions options = (PublishJobOptions) deserializeOption(input.getOptions(), PublishJobOptions.class);
		
		handlePublish(input, options, fields);
		
		LinkedHashMap<String, String> progressParams = options.getProgress().getParams();
		if (progressParams == null || progressParams.isEmpty()) {
			throw new IllegalStateException("CDN extraction requires progress information from unzip");
		}
		
		// Extract the CDN-specifics
		handleManifestMap("embd_" + input.getWorkflow() + "_cdn", progressParams, fields);
	}

}
