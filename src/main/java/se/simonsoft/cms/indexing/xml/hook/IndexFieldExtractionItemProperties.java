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
package se.simonsoft.cms.indexing.xml.hook;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.indexing.IndexFields;
import se.simonsoft.cms.item.properties.CmsItemProperties;
import se.simonsoft.xmltracking.index.add.IndexFieldExtraction;
import se.simonsoft.xmltracking.source.XmlSourceElement;

public class IndexFieldExtractionItemProperties implements IndexFieldExtraction {

	private static final Logger logger = LoggerFactory.getLogger(IndexFieldExtractionItemProperties.class);
	
	private IndexingContext context;

	@Inject
	public IndexFieldExtractionItemProperties(IndexingContext indexingContext) {
		this.context = indexingContext;
	}
	
	@Override
	public void extract(IndexFields fields, XmlSourceElement ignored) {
		CmsItemProperties props = context.getItemProperties();
		for (String prop : props.getKeySet()) {
			String val = props.getString(prop);
			if (val == null) {
				logger.warn("Property {} not readable as string, will not be indexed", prop);
			} else {
				fields.addField("prop_" + prop, val);
			}
		}
	}

}
