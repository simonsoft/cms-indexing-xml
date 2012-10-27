/**
 * Copyright (C) 2009-2012 Simonsoft Nordic AB
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
package se.simonsoft.cms.indexing;

import java.util.Collection;

/**
 * Support duplication into different core, with additional data.
 * Support update of existing fields or adding new ones.
 */
public interface IndexItem {

	/**
	 * ID field name is always "id"
	 * @return ID of this item in index, 
	 */
	public String getId();
	
	/**
	 * @return Names of all added fields.
	 */
	public abstract Collection<String> getFieldNames();

	/**
	 * @return Current field value or null if the field does not exist
	 */
	public abstract Object getFieldValue(String name);

	/**
	 * @return only the fields that were added
	 */
	public Collection<String> getFieldNamesAdded();
	
	/**
	 * @return only the fields that existed but have been changed
	 */
	public Collection<String> getFieldNamesUpdated();
	
	/**
	 * Copies current instance but sets mark so that the new instance's
	 * {@link #getFieldNamesAdded()} and {@link #getFieldNamesUpdated()} only
	 * reflects subsequent changes.
	 * @return Exact copy of the current 
	 */
	public abstract IndexFields getCopy();

}