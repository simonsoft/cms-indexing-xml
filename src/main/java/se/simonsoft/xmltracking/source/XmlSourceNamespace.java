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
package se.simonsoft.xmltracking.source;

public class XmlSourceNamespace {

	private String name;
	private String uri;
	
	public XmlSourceNamespace(String name, String uri) {
		if (name == null) {
			throw new IllegalArgumentException("name can not be null");
		}
		if (uri == null) {
			throw new IllegalArgumentException("uri can not be null");
		}		
		this.name = name;
		this.uri = uri;
	}
	
	public String getName() {
		return name;
	}
	
	public String getUri() {
		return uri;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj != null && obj instanceof XmlSourceNamespace
				&& name.equals(((XmlSourceNamespace) obj).getName())
				&& uri.equals(((XmlSourceNamespace) obj).getUri());
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return "xmlns:" + name + "=\"" + uri + "\"";
	}
	
}
