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

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.s9api.Processor;
import se.simonsoft.cms.xmlsource.SaxonConfiguration;
import se.simonsoft.cms.xmlsource.handler.XmlSourceReader;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceReaderS9api;
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

		MapBinder<String, Source> sourceBinder = MapBinder.newMapBinder(binder(), String.class, Source.class);
		sourceBinder.addBinding("identity.xsl").toInstance(new StreamSource(this.getClass().getClassLoader().getResourceAsStream("se/simonsoft/cms/xmlsource/transform/identity.xsl")));
		sourceBinder.addBinding("reuse-normalize.xsl").toInstance(new StreamSource(this.getClass().getClassLoader().getResourceAsStream("se/simonsoft/cms/xmlsource/transform/reuse-normalize.xsl")));
		sourceBinder.addBinding("itemid-normalize.xsl").toInstance(new StreamSource(this.getClass().getClassLoader().getResourceAsStream("se/simonsoft/cms/xmlsource/transform/itemid-normalize.xsl")));
	
		sourceBinder.addBinding("xml-indexing-repositem.xsl").toInstance(new StreamSource(this.getClass().getClassLoader().getResourceAsStream("se/simonsoft/cms/indexing/xml/source/xml-indexing-repositem.xsl")));
		sourceBinder.addBinding("xml-indexing-reposxml.xsl").toInstance(new StreamSource(this.getClass().getClassLoader().getResourceAsStream("se/simonsoft/cms/indexing/xml/source/xml-indexing-reposxml.xsl")));

		
		// Set up test config defaults.
		bind(Integer.class).annotatedWith(Names.named("se.simonsoft.cms.indexing.xml.maxFilesize")).toInstance(new Integer(10 * 1048576));
		bind(String.class).annotatedWith(Names.named("se.simonsoft.cms.indexing.xml.suppressRidBefore")).toInstance(new String(""));
	}

}
