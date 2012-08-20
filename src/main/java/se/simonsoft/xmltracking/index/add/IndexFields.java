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
package se.simonsoft.xmltracking.index.add;

import java.util.Collection;

/**
 * Current field set for indexing, used in {@link IndexFieldExtraction}.
 */
public interface IndexFields {

	/**
	 * @return Names of all added fields.
	 */
	Collection<String> getFieldNames();
	
	/**
	 * Adds field  value of arbitrary type
	 * @param name Name that matches an indexing schema field
	 * @param value Valy with type matching the indexing schema field
	 */
	void addField(String name, Object value);
	
	/**
	 * @return Current field value or null if the field does not exist
	 */
	Object getFieldValue(String name);
	
}
