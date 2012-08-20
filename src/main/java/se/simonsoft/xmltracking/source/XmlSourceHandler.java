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
package se.simonsoft.xmltracking.source;

/**
 * Receiver for XML tree walking with full source extraction,
 * useful both for indexing and reuse/replacement writing.
 */
public interface XmlSourceHandler {

	void startDocument();
	
	void endDocument();
	
	/**
	 * Depth first visiting, in the order elements appear
	 *  not the order in which they end.
	 *  
	 * Named begin, although argument contains full source, to indicate
	 * that the method is called in order of element starts and to allow
	 * for a future element end method if needed.
	 * 
	 * Caller should be fast enough so that impls can ignore elements
	 * and children without worring about lost performance. It would add
	 * quite a bit of complexity to signal to traversal in the case where
	 * reuse has been established and no further depth is needed.
	 * 
	 * @param element The full element that was encountered
	 */
	void begin(XmlSourceElement element);
	
}
