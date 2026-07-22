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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;

import jakarta.enterprise.context.Dependent;
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
		// repos-indexing is now CDI annotated, while these tests still use Guice.
		bindScope(Dependent.class, Scopes.NO_SCOPE);

		Set<ExtensionFunctionDefinition> transformerFunctions = new LinkedHashSet<>();
		transformerFunctions.add(new GetChecksum());
		transformerFunctions.add(new GetPegRev());
		transformerFunctions.add(new WithPegRev());
		transformerFunctions.add(new GetLogicalId());
		Processor processor = new SaxonConfiguration(transformerFunctions).get();
		XmlSourceReaderS9api reader = new XmlSourceReaderS9api(processor);
		bind(Processor.class).toInstance(processor);
		bind(XmlSourceReaderS9api.class).toInstance(reader);
		bind(XmlSourceReader.class).toInstance(reader);

		Map<String, String> stylesheets = TransformerServiceFactory.getStylesheetsForTestingMap();
		stylesheets.put("xml-indexing-repositem.xsl", "se/simonsoft/cms/indexing/xml/source/xml-indexing-repositem.xsl");
		stylesheets.put("xml-indexing-reposxml.xsl", "se/simonsoft/cms/indexing/xml/source/xml-indexing-reposxml.xsl");
		TransformStylesheetSource stylesheetSource = new TransformStylesheetSourceConfig(stylesheets);
		bind(TransformStylesheetSource.class).toInstance(stylesheetSource);
		bind(TransformerServiceFactory.class).toInstance(new TransformerServiceFactory(processor, reader, stylesheetSource));
		
		// Set up test config defaults.
		bind(Integer.class).annotatedWith(Names.named("se.simonsoft.cms.indexing.xml.maxFilesize")).toInstance(new Integer(10 * 1048576));
		bind(String.class).annotatedWith(Names.named("se.simonsoft.cms.indexing.xml.suppressRidBefore")).toInstance(new String(""));
		bind(String.class).annotatedWith(Names.named("se.simonsoft.cms.indexing.xml.tsourceAllowed")).toInstance(new String("tsp"));
	}

}
