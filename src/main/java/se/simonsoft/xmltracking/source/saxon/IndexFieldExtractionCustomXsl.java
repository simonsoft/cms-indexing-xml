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
package se.simonsoft.xmltracking.source.saxon;

import java.io.Reader;
import java.io.StringReader;

import javax.inject.Inject;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.ContentHandler;

import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Destination;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SAXDestination;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

import se.simonsoft.cms.indexing.IndexFields;
import se.simonsoft.xmltracking.index.add.IndexFieldExtraction;
import se.simonsoft.xmltracking.source.XmlSourceElement;
import se.simonsoft.xmltracking.source.jdom.XmlSourceReaderJdom;

/**
 * 
 * Depends on "source" field extraction from {@link XmlSourceReaderJdom}.
 */
public class IndexFieldExtractionCustomXsl implements IndexFieldExtraction {
	
	private transient XsltExecutable xsltCompiled;
	private transient XsltTransformer transformer; // if creation is fast we could be thread safe and load this for every read
	
	/**
	 * How to get document status from already extracted fields.
	 */
	public static final String STATUS_FIELD_NAME = "prop_cms:status";
	
	private static final QName STATUS_PARAM = new QName("status");
	
	@Inject
	public IndexFieldExtractionCustomXsl(XmlMatchingFieldExtractionSource xslSource) {
		init(xslSource.getXslt());
	}
	
	private void init(Source xslt) {
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

	@Override
	public void extract(IndexFields fields, XmlSourceElement processedElement) {
		Object source = fields.getFieldValue("source");
		if (source == null) {
			throw new IllegalArgumentException("Prior to text extraction, 'source' field must have been extracted.");
		}
		Reader sourceReader;
		if (source instanceof Reader) {
			sourceReader = (Reader) source;
		} else if (source instanceof String) {
			sourceReader = new StringReader((String) source);
		} else {
			throw new IllegalArgumentException("Unexpected source field " + source.getClass() + " in " + fields);
		}
		Source xmlInput = new StreamSource(sourceReader);
		
		ContentHandler transformOutputHandler = new ContentHandlerToIndexFields(fields);
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
		
		Object status = fields.getFieldValue(STATUS_FIELD_NAME);
		if (status != null) {
			transformer.setParameter(STATUS_PARAM, new XdmAtomicValue((String) status));
		}
		
		try {
			transformer.transform();
		} catch (SaxonApiException e) {
			throw new RuntimeException("Extraction aborted with error", e);
		}
		
	}

}