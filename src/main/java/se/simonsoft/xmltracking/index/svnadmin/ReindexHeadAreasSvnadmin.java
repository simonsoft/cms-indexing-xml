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

import org.apache.solr.client.solrj.SolrServer;

import se.simonsoft.xmltracking.index.add.IdStrategy;
import se.simonsoft.xmltracking.index.add.XmlSourceHandlerSolrj;
import se.simonsoft.xmltracking.source.XmlSourceHandler;
import se.simonsoft.xmltracking.source.XmlSourceReader;
import se.simonsoft.xmltracking.source.jdom.XmlSourceReaderJdom;

public class ReindexHeadAreasSvnadmin {

	private File repository;
	private String releasePath;
	private String translationPath;
	
	private IdStrategy idStrategy;
	private XmlSourceReader reader = new XmlSourceReaderJdom();
	private SolrServer server;
	
	public ReindexHeadAreasSvnadmin(File repository, String releasePath, String translationPath) {
		
	}
	
	XmlSourceHandler getSourceHandler() {
		return new XmlSourceHandlerSolrj(server, idStrategy);
	}
	
	public void reindex() {
		
	}
	
	public void reindex(String repositoryPath, XmlSourceHandler handler) {
		
	}
	
}
