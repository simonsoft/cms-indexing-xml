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
package se.simonsoft.xmltracking.index.svnadmin;

import java.io.File;

import se.simonsoft.xmltracking.index.add.IdStrategy;
import se.simonsoft.xmltracking.source.XmlSourceElement;

/**
 * Sets IDs based on repo name and path
 * (for offline indexing that does not know the URLs).
 * 
 * Intentionally without a revision or timestamp, so that
 * documents are overwritten at next indexing.
 * 
 * TODO needs notification of path from file traversal
 * before a new document's first element.
 * Verify at {@link #start()} that such a path has just been provided.
 */
public class IdStrategySvnRepoHead implements IdStrategy {

	private String prefix;

	public IdStrategySvnRepoHead(File repositoryPath) {
		this(repositoryPath.getName());
	}
	
	public IdStrategySvnRepoHead(String name) {
		this.prefix = name + "^";
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getElementId(XmlSourceElement element) {
		// TODO Auto-generated method stub
		return null;
	}

}
