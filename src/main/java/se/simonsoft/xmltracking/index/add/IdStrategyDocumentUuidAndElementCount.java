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
package se.simonsoft.xmltracking.index.add;

import se.simonsoft.xmltracking.source.XmlSourceElement;

/**
 * Generic id strategy with no need for a context, generates document id.
 * Warning: with this impl, reindexing requires empty schema or there'll be duplicates.
 */
public class IdStrategyDocumentUuidAndElementCount implements IdStrategy {

	@Override
	public void start() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Method not implemented");
	}

	@Override
	public String getElementId(XmlSourceElement element) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Method not implemented");
	}

}
