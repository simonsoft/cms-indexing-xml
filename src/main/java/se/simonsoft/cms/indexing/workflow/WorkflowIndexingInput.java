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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.impl.CmsItemIdArg;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowIndexingInput {

	private String workflow;
	private String executionId;
	private String executionStart;
	private String status;
	private String error;
	private boolean complete = false;
	
	private String configname; // cmsconfig-module:configname
	private String action;
	private CmsItemId itemId;
	private String userId;
	private JsonNode options;
	
	
	public WorkflowIndexingInput() {
		// Default constructor for Jackson.
	}
	
	
	// Set by each index Task in SFN workflow.
	public String getWorkflow() {
		return workflow;
	}


	public void setWorkflow(String workflow) {
		this.workflow = workflow;
	}

	@JsonGetter("executionid")
	public String getExecutionId() {
		return executionId;
	}

	@JsonSetter("executionid")
	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}

	@JsonGetter("executionstart")
	public String getExecutionStart() {
		return executionStart;
	}

	@JsonSetter("executionstart")
	public void setExecutionStart(String executionStart) {
		this.executionStart = executionStart;
	}


	public String getAction() {
		return this.action;
	}



	public boolean isComplete() {
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}


	public String getStatus() {
		return status;
	}


	public void setStatus(String status) {
		this.status = status;
	}


	public String getConfigname() {
		return configname;
	}


	public void setConfigname(String configname) {
		this.configname = configname;
	}


	public void setAction(String action) {
		this.action = action;
	}
	
	public String getError() {
		return error;
	}
	
	public void setError(String error) {
		this.error = error;
	}
	
	@JsonSetter("itemid")
	public void setId(String itemId) {
		this.itemId = new CmsItemIdArg(itemId);
	}
	
	@JsonGetter("itemid")
	public String getItemIdJson() {
		return this.itemId.getLogicalIdFull();
	}
	
	@JsonIgnore
	public void setItemId(CmsItemId itemId) {
		this.itemId = itemId;
	}

	@JsonIgnore
	public CmsItemId getItemId() {
		return this.itemId;
	}
	
	@JsonIgnore
	public JsonNode getOptions() {
		return this.options;
	}
	
	@JsonSetter("options")
	public void setOptions(JsonNode options) {
		this.options = options;
	}
	
	@JsonGetter("userid")
	public String getUserId() {
		return this.userId;
	}

	@JsonSetter("userid")
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
}
