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
package se.simonsoft.cms.admin;

import java.io.File;

import se.simonsoft.cms.item.CmsRepository;

/**
 * Provides repository information to services that do administrative tasks.
 * 
 * With knowledge of the local path to a repository, access is allowed without authentication.
 * To prevent unauthorized use of these services, paths should be considered sensitive
 * information which impls should be unaware of at initialization.
 * Requiring the path to be provided with each service call in a way
 * regulates that the caller is authorized.
 */
public class CmsRepositoryInspection extends CmsRepository {

	private File adminPath;

	/**
	 * @param parentPath With leading but not traling slash
	 * @param name Repository name, no slashes
	 * @param localAdminPath The path to the repository locally, for admin tasks, sensitive information
	 */
	public CmsRepositoryInspection(String parentPath, String name, File localAdminPath) {
		super(parentPath, name);
		this.adminPath = localAdminPath;
	}

	/**
	 * @return local path to repository root
	 */
	public File getAdminPath() {
		return adminPath;
	}
	
}
