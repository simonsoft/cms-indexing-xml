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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.HandlerProperties;

/**
 * Preprocesses the doc for common fields so that the old reposxml schema is supported while transitioning to the new repositem fields.
 */
public class XmlIndexRestrictFields {

	private static final Logger logger = LoggerFactory.getLogger(XmlIndexRestrictFields.class);
	
	/**
	 * Until {@link IndexingDoc#deepCopy()} can get only the {@link HandlerPathinfo} and {@link HandlerProperties} fields we use this to map repositem fields to reposxml schema.
	 * Key is field name, value is rename or null for using same name (we should end up with only nulls here).
	 */
	public static final Map<String, String> FIELDS_KEEP = new HashMap<String, String>() {private static final long serialVersionUID = 1L;{
		put("id", null);
		put("path", null);
		put("pathname", null);
		put("pathdir", null);
		put("pathin", null);
		put("pathext", null);
		put("pathfull", null);
		put("patharea", null);
		put("pathmain", null);
		put("rev", null);
		put("revt", null);
		put("repo", null);
		put("repoid", null);
		put("repoparent", null);
		put("repohost", null);
	}};
	
	public void handle(IndexingDoc itemDoc) {
		Set<String> keep = FIELDS_KEEP.keySet();
		Set<String> remove = new LinkedHashSet<String>();
		for (String name : itemDoc.getFieldNames()) {
			if (name.startsWith("prop_")) continue;
			if (!keep.contains(name)) {
				logger.trace("Removing field '{}' not in xml keep list", name);
				remove.add(name);
			}
		}
		for (String r : remove) {
			itemDoc.removeField(r);
		}
	}

}
