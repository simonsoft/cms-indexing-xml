/**
 * Copyright (C) 2009-2016 Simonsoft Nordic AB
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
package se.simonsoft.cms.indexing.xml.testconfig;

import javax.inject.Singleton;

import org.tmatesoft.svn.core.wc.admin.SVNLookClient;

import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.s9api.Processor;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.repos.indexing.twophases.ItemContentsMemory;
import se.simonsoft.cms.backend.svnkit.svnlook.CmsContentsReaderSvnkitLook;
import se.simonsoft.cms.backend.svnkit.svnlook.SvnlookClientProviderStateless;
import se.simonsoft.cms.indexing.xml.IndexingHandlersXml;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.indexing.xml.XmlIndexWriter;
import se.simonsoft.cms.indexing.xml.custom.IndexFieldExtractionCustomXsl;
import se.simonsoft.cms.indexing.xml.custom.XmlMatchingFieldExtractionSource;
import se.simonsoft.cms.indexing.xml.custom.XmlMatchingFieldExtractionSourceDefault;
import se.simonsoft.cms.indexing.xml.solr.XmlIndexWriterSolrjBackground;
import se.simonsoft.cms.item.inspection.CmsContentsReader;
import se.simonsoft.cms.xmlsource.SaxonConfiguration;
import se.simonsoft.cms.xmlsource.handler.XmlSourceReader;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceReaderS9api;
import se.simonsoft.cms.xmlsource.transform.function.GetChecksum;
import se.simonsoft.cms.xmlsource.transform.function.GetPegRev;
import se.simonsoft.cms.xmlsource.transform.function.WithPegRev;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

public class IndexingConfigXmlBase extends AbstractModule {

	@Override
	protected void configure() {
		bind(Processor.class).toProvider(SaxonConfiguration.class);
		Multibinder<ExtensionFunctionDefinition> transformerFunctions = Multibinder.newSetBinder(binder(), ExtensionFunctionDefinition.class);
		transformerFunctions.addBinding().to(GetChecksum.class);
		transformerFunctions.addBinding().to(GetPegRev.class);
		transformerFunctions.addBinding().to(WithPegRev.class);
		bind(XmlSourceReader.class).to(XmlSourceReaderS9api.class);

		
		// Set up test config defaults.
		bind(Integer.class).annotatedWith(Names.named("se.simonsoft.cms.indexing.xml.maxFilesize")).toInstance(new Integer(10 * 1048576));
		bind(String.class).annotatedWith(Names.named("se.simonsoft.cms.indexing.xml.suppressRidBefore")).toInstance(new String(""));
	}

}
