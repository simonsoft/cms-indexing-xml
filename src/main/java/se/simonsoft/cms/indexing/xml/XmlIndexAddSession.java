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
package se.simonsoft.cms.indexing.xml;

import java.util.Collection;

import se.repos.indexing.IndexingDoc;

public interface XmlIndexAddSession extends Collection<IndexingDoc> {

	/**
	 * Called to end session, typically when the XML file is completely read.
	 */
	void end();

	/**
	 * @return accumulated form all added {@link IndexingDoc#getContentSize()}
	 */
	int sizeContentTotal();
	
}
