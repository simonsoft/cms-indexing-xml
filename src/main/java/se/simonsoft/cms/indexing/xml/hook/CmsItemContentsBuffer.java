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
package se.simonsoft.cms.indexing.xml.hook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Still unclear how we buffer item contents during hook/changeset processing,
 * for use in for example both xml and fulltext indexing.
 * 
 * Also unclear how buffers would be shared (additional getter in {@link CmsItemAndContents}?)
 * and who would be responsible for error handling.
 */
public class CmsItemContentsBuffer {

	private CmsItemAndContents item;

	private transient byte[] buffer = null;
	
	public CmsItemContentsBuffer(CmsItemAndContents item) {
		this.item = item;
	}
	
	public byte[] getBuffer() {
		if (buffer == null) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			item.getContents(out);
			buffer = out.toByteArray();
		}
		return buffer;
	}
	
	/**
	 * @return for use in xml parsing, Tika etc.
	 */
	public InputStream getContents() {
		return new ByteArrayInputStream(getBuffer());
	}
	
}
