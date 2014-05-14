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
package se.simonsoft.cms.indexing.xml.custom;

import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Destination;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SAXDestination;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.xml.sax.ContentHandler;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;
import se.simonsoft.cms.xmlsource.handler.jdom.XmlSourceReaderJdom;

/**
 * 
 * Depends on "source" field extraction from {@link XmlSourceReaderJdom}.
 */
public class IndexFieldExtractionCustomXsl implements XmlIndexFieldExtraction {
	
	private static final Logger logger = LoggerFactory.getLogger(IndexFieldExtractionCustomXsl.class);
	
	private transient XsltExecutable xsltCompiled;
	private transient XsltTransformer transformer; // if creation is fast we could be thread safe and load this for every read
	
	/**
	 * How to get document status from already extracted fields.
	 */
	public static final String STATUS_FIELD_NAME = "prop_cms.status";
	public static final String DEPTH_FIELD_NAME = "depth";
	public static final String TSUPPRESS_A_FIELD_NAME = "ia_cms.tsuppress";
	
	private static final QName STATUS_PARAM = new QName("document-status");
	private static final QName DEPTH_PARAM = new QName("document-depth");
	
	private static final QName A_ATTR_PARAM = new QName("ancestor-attributes");
	//private static final QName D_ATTR_PARAM = new QName("document-attributes");
	
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
	
	
	/** Just need a way to get a DOM element/document. 
	 * Unsure what APIs we should use so this was a sample using pure W3C DOM. 
	 * @param rootElementName
	 * @return
	 */
	private org.w3c.dom.Document createDomDocument(String rootElementName) {

		try {
			// first of all we request out 
			// DOM-implementation:
			DocumentBuilderFactory factory =
					DocumentBuilderFactory.newInstance();
			// then we have to create document-loader:
			DocumentBuilder loader = factory.newDocumentBuilder();

			// createing a new DOM-document...
			org.w3c.dom.Document document = loader.newDocument();

			// initially it has no root-element, ... so we create it:
			org.w3c.dom.Element root = document.createElement(rootElementName);

			// we can add an element to a document only once,
			// the following calls will raise exceptions:
			document.appendChild(root);

			//return root;
			return document;

		} catch (Exception e) {
			throw new RuntimeException("failed to create DOM element/document", e);
		}
	}
	

	private XdmValue getAttributeInheritedDoc(IndexingDoc fields) {
	
		net.sf.saxon.s9api.DocumentBuilder db = new Processor(false).newDocumentBuilder();
		try {
			org.w3c.dom.Document doc = createDomDocument("attributes");
			org.w3c.dom.Element elem = doc.getDocumentElement();
			
			Collection<String> fieldNames = fields.getFieldNames();
			
			// First extract namespaces.
			Map<String,String> namespaces = new HashMap<String,String>();
			for (String fieldName: fieldNames) {
				if (fieldName.startsWith("ins_")) {
					String prefix = fieldName.substring(4);
					namespaces.put(prefix, (String) fields.getFieldValue(fieldName));
					//logger.trace("added NS to map: {} - {}", prefix, fields.getFieldValue(fieldName));
				}
				
			}
			
			// Add attributes to element.
			for (String fieldName : fieldNames) {
				try {
					if (fieldName.startsWith("ia_")) {
						String value = (String) fields.getFieldValue(fieldName);
						if (value != null) {
							String name = fieldName.substring(3).replace('.', ':');
							int nssep = name.indexOf(':');
							if (nssep == -1 || fieldName.startsWith("ia_xml")) {
								Attr attr = doc.createAttribute(name);
								attr.setValue(value);
								elem.setAttributeNode(attr);
							} else {
								String nsuri = namespaces.get(name.substring(0, nssep));
								//logger.trace("adding attribute with NS uri: {}", nsuri);
								Attr attr = doc.createAttributeNS(nsuri, name);
								attr.setValue(value);
								elem.setAttributeNode(attr);
							}
						} else {
							logger.warn("field for attribute {} is null", fieldName);
						}
					}
				} catch (Exception e) {
					logger.error("failed to pass attribute in field {} to XSLT", fieldName);
					throw e;
				}
			}

			
			XdmNode xdmDoc = db.build(new DOMSource(doc));
			return xdmDoc;
		} catch (Exception e) {
			throw new RuntimeException("failed to pass attributes to XSLT", e);
		}
	}

	@Override
	public void extract(XmlSourceElement processedElement, IndexingDoc fields) throws XmlNotWellFormedException {
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
		
		// Status as parameter to XSL.
		Object status = fields.getFieldValue(STATUS_FIELD_NAME);
		if (status != null) {
			transformer.setParameter(STATUS_PARAM, new XdmAtomicValue((String) status));
		}
		
		// Depth as parameter to XSL
		Object depth = fields.getFieldValue(DEPTH_FIELD_NAME);
		if (depth != null) {
			transformer.setParameter(DEPTH_PARAM, new XdmAtomicValue((Integer) depth));
		}
		
		
		// Sending all inherited attributes to XSL.
		transformer.setParameter(A_ATTR_PARAM, this.getAttributeInheritedDoc(fields));
		
		
		try {
			transformer.transform();
		} catch (SaxonApiException e) {
			if (e.getCause() instanceof TransformerException) { // including net.sf.saxon.trans.XPathException
				throw new XmlNotWellFormedException("XML invalid for transformation at " + processedElement, e);
			}
			throw new RuntimeException("Extraction aborted with error at " + processedElement, e);
		}
		
	}

	@Override
	public void endDocument() {
		
	}

}
