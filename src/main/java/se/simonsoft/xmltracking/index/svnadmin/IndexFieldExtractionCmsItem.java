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

import se.simonsoft.xmltracking.index.add.IndexFieldExtraction;
import se.simonsoft.xmltracking.index.add.IndexFields;
import se.simonsoft.xmltracking.source.XmlSourceElement;

/**
 * Adds cms item id and revision info to indexed fields.
 * 
 * TODO design for notification from indexing script.
 */
public class IndexFieldExtractionCmsItem implements IndexFieldExtraction {

	@Override
	public void extract(IndexFields fields,
			XmlSourceElement processedElement) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("not implemented");
	}

}
