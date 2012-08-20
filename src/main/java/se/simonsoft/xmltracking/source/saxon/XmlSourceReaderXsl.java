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
package se.simonsoft.xmltracking.source.saxon;

import java.io.InputStream;

import javax.inject.Inject;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.ContentHandler;

import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Destination;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SAXDestination;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

import se.simonsoft.xmltracking.source.XmlSourceHandler;
import se.simonsoft.xmltracking.source.XmlSourceReader;

/**
 * Tries to build the entire solr "add" doc using xsl, an attempt that was aborted.
 * It produces the require fields but needs lots of before it can handle the
 * aggregation of attributes (not easy in xslt because we want to support arbitrary
 * attribute names) and checksumming (probably needs some xslt extension).
 * 
 * @deprecated We decided to use xslt only to add new "matching" fields
 *  based on previous extraction of source, see {@link IndexFieldExtractionCustomXsl}.
 */
public class XmlSourceReaderXsl implements XmlSourceReader {

	private Source xslt;

	private transient XsltExecutable xsltCompiled;
	private transient XsltTransformer transformer; // if creation is fast we could be thread safe and load this for every read
	
	@Inject
	public void setExtractionStylesheet(Source xslt) {
		this.xslt = xslt;
		init();
	}
	
	public void init() {
		Configuration config = new Configuration();
		Processor processor = new Processor(config);
		
		XsltCompiler compiler = processor.newXsltCompiler();
		try {
			xsltCompiled = compiler.compile(xslt);
		} catch (SaxonApiException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException("Error not handled", e);
		}
		
		transformer = xsltCompiled.load();
	}
	
	/**
	 * 
	 * @todo synchronized may be a performance hit, maybe we should assert serial reuse in some other way
	 */
	@Override
	public synchronized void read(InputStream xml, XmlSourceHandler handler) {
		ContentHandler transformOutputHandler = new ContentHandlerXmltrackingFields(handler);
		Source xmlInput = new StreamSource(xml);
		Destination xmltrackingFieldsHandler = new SAXDestination(transformOutputHandler);
		//Destination xmltrackingFieldsHandler = new net.sf.saxon.s9api.Serializer(System.out);
		
		transformer.setErrorListener(new LoggingErrorListener());
		
		try {
			transformer.setSource(xmlInput);
		} catch (SaxonApiException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException("Error not handled", e);
		}
		
		transformer.setDestination(xmltrackingFieldsHandler);
		
		try {
			transformer.transform();
		} catch (SaxonApiException e) {
			throw new RuntimeException("Extraction aborted with error", e);
		}
	}

}

