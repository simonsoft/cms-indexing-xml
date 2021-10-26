/**
 * Copyright (C) 2009-2017 Simonsoft Nordic AB
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

import java.util.Map;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.s9api.Processor;
import se.simonsoft.cms.xmlsource.SaxonConfiguration;
import se.simonsoft.cms.xmlsource.handler.XmlSourceReader;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceReaderS9api;
import se.simonsoft.cms.xmlsource.transform.TransformStylesheetSource;
import se.simonsoft.cms.xmlsource.transform.TransformStylesheetSourceConfig;
import se.simonsoft.cms.xmlsource.transform.TransformerServiceFactory;
import se.simonsoft.cms.xmlsource.transform.function.GetChecksum;
import se.simonsoft.cms.xmlsource.transform.function.GetLogicalId;
import se.simonsoft.cms.xmlsource.transform.function.GetPegRev;
import se.simonsoft.cms.xmlsource.transform.function.WithPegRev;

public class IndexingConfigXmlBase extends AbstractModule {

	@Override
	protected void configure() {
		bind(Processor.class).toProvider(SaxonConfiguration.class);
		Multibinder<ExtensionFunctionDefinition> transformerFunctions = Multibinder.newSetBinder(binder(), ExtensionFunctionDefinition.class);
		transformerFunctions.addBinding().to(GetChecksum.class);
		transformerFunctions.addBinding().to(GetPegRev.class);
		transformerFunctions.addBinding().to(WithPegRev.class);
		transformerFunctions.addBinding().to(GetLogicalId.class);
		bind(XmlSourceReader.class).to(XmlSourceReaderS9api.class);

		Map<String, String> stylesheets = TransformerServiceFactory.getStylesheetsForTestingMap();
		stylesheets.put("xml-indexing-repositem.xsl", "se/simonsoft/cms/indexing/xml/source/xml-indexing-repositem.xsl");
		stylesheets.put("xml-indexing-reposxml.xsl", "se/simonsoft/cms/indexing/xml/source/xml-indexing-reposxml.xsl");
		bind(TransformStylesheetSource.class).toInstance(new TransformStylesheetSourceConfig(stylesheets));
		
		// Set up test config defaults.
		bind(Integer.class).annotatedWith(Names.named("se.simonsoft.cms.indexing.xml.maxFilesize")).toInstance(new Integer(10 * 1048576));
		bind(String.class).annotatedWith(Names.named("se.simonsoft.cms.indexing.xml.suppressRidBefore")).toInstance(new String(""));
	}

}
