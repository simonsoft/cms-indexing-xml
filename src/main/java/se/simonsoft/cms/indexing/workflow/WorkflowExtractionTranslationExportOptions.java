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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import se.simonsoft.cms.publish.config.databinds.job.PublishJobDelivery;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobProgress;


// Duplicated from cms-release, consider moving to cms-item or cms-publish-config.
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowExtractionTranslationExportOptions {

	private String project; 
	private String pdf;
	private PublishJobDelivery delivery = new PublishJobDelivery();
	private PublishJobProgress progress = new PublishJobProgress(); // Added during the execution.
	
	public WorkflowExtractionTranslationExportOptions() {
	}

	public String getProject() {
		return this.project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getPdf() {
		return this.pdf;
	}

	public void setPdf(String pdf) {
		this.pdf = pdf;
	}

	public PublishJobDelivery getDelivery() {
		return this.delivery;
	}

	public void setDelivery(PublishJobDelivery delivery) {
		this.delivery = delivery;
	}
	
	public PublishJobProgress getProgress() {
		return this.progress;
	}
}
