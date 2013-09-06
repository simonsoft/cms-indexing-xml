/**
 * Copyright (C) 2009-2013 Simonsoft Nordic AB
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
package se.simonsoft.xmltracking.index;

import se.simonsoft.xmltracking.index.add.IdStrategy;
import se.simonsoft.xmltracking.index.add.XmlIndexFieldExtraction;

/**
 * Provides access to metadata in IdStrategy#getElementId and IndexFieldExtensions.
 * 
 * Do we need this?
 * - Path is better handled in {@link IdStrategy} impl
 * - Revision could be added using index fields pre-indexing service.
 *   See {@link XmlIndexFieldExtraction}
 * - Custom notifications to this impl by the caller of XmlSourceHandler
 * @deprecated No, we don't need this, too inflexible
 */
public interface CmsItemMetadata {

	/**
	 * @return Identification, possibly URL? Cooperation with {@link IdStrategy}
	 */
	String getPathNormalized();
	
	/**
	 * @return Current document's revision, null if not versioned
	 */
	Long getRevisionNumber();
	
}
