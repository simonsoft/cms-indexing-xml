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

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.simonsoft.cms.item.CmsItemId;

/**
 * Sets the {@value #FIELD_NAME} field.
 */
public abstract class HandlerLogicalId implements IndexingItemHandler {

	public static final String FIELD_NAME = "urlid";
	public static final String FIELD_NAME_HEAD = "urlidhead";
	
	@Override
	public void handle(IndexingItemProgress progress) {
		CmsItemId itemId = getItemId(progress);
		if (itemId != null) {
			// use addField istead of setField so we detect conflicts with other id resolution strategies
			progress.getFields().addField(FIELD_NAME, itemId.getLogicalIdFull());
			progress.getFields().addField(FIELD_NAME_HEAD, itemId.withPegRev(null).getLogicalIdFull());
		}
	}
	
	/**
	 * @param progress from {@link #handle(IndexingItemProgress)}
	 * @return null to avoid setting the field, an id preferably with peg rev to set the field
	 */
	protected abstract CmsItemId getItemId(IndexingItemProgress progress);

}
