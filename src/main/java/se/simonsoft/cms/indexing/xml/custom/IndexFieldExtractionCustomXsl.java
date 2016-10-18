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
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

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
import org.slf4j.helpers.MessageFormatter;
import org.w3c.dom.Attr;
import org.xml.sax.ContentHandler;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingHandlerException;
import se.simonsoft.cms.indexing.xml.XmlIndexElementId;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.indexing.xml.XmlIndexProgress;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;
import se.simonsoft.cms.xmlsource.handler.jdom.XmlSourceReaderJdom;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceElementS9api;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceReaderS9api;

/**
 * 
 * Depends on "source" field extraction from {@link XmlSourceReaderJdom} when using that impl (old).
 * Depends on getElement() returning an S9API XdmNode in {@link XmlSourceReaderS9api} when using that impl.
 */
public class IndexFieldExtractionCustomXsl implements XmlIndexFieldExtraction {
	
	private static final Logger logger = LoggerFactory.getLogger(IndexFieldExtractionCustomXsl.class);

	private transient Processor processor;
	private transient XsltExecutable xsltCompiled;
	private transient XsltTransformer transformer; // if creation is fast we could be thread safe and load this for every read
	
	// we request a DOM-implementation:
	private static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	// we have to create document-loader:
	private static javax.xml.parsers.DocumentBuilder db;
	
	/**
	 * How to get document status from already extracted fields.
	 */
	public static final String STATUS_FIELD_NAME = "prop_cms.status";
	public static final String DEPTH_FIELD_NAME = "depth";
	
	private static final String I_NS_FIELD_PREFIX = "ins_";
	
	private static final QName STATUS_PARAM = new QName("document-status");
	private static final QName DEPTH_PARAM = new QName("document-depth");
	
	private static final QName A_ATTR_PARAM = new QName("ancestor-attributes");
	private static final String A_ATTR_FIELD_PREFIX = "aa_";
	//private static final QName R_ATTR_PARAM = new QName("root-attributes");
	
	@Inject
	public IndexFieldExtractionCustomXsl(XmlMatchingFieldExtractionSource xslSource, Processor processor) {
		this.processor = processor;
		init(xslSource.getXslt());
		initDom();
	}
	
	private void init(Source xslt) {
		
		XsltCompiler compiler = processor.newXsltCompiler();
		try {
			xsltCompiled = compiler.compile(xslt);
		} catch (SaxonApiException e) {
			throw new RuntimeException("Error not handled: " + e.getMessage(), e);
		}
		
		transformer = xsltCompiled.load();
	}
	
	private void initDom() {
		
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Error not handled: " + e.getMessage(), e);
		}
	}
	
	
	/** Just need a way to get a DOM element/document. 
	 * Unsure what APIs we should use so this was a sample using pure W3C DOM. 
	 * @param rootElementName
	 * @return
	 */
	private org.w3c.dom.Document createDomDocument(String rootElementName) {

		try {
			// Moved init to constructor.
			
			// creating a new DOM-document...
			org.w3c.dom.Document document = db.newDocument();

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
	
		// Saxon 9.5.1 requires that parameter element uses same Configuration as Source/Stylesheet.
		// Having different Configurations also caused massive Heap issues with net.sf.saxon.om.NamePool.NameEntry.
		net.sf.saxon.s9api.DocumentBuilder db = processor.newDocumentBuilder();
		try {
			org.w3c.dom.Document doc = createDomDocument("attributes");
			org.w3c.dom.Element elem = doc.getDocumentElement();
			
			Collection<String> fieldNames = fields.getFieldNames();
			
			// First extract namespaces.
			Map<String,String> namespaces = new HashMap<String,String>();
			for (String fieldName: fieldNames) {
				if (fieldName.startsWith(I_NS_FIELD_PREFIX)) {
					String prefix = fieldName.substring(fieldName.indexOf('_') + 1);
					namespaces.put(prefix, (String) fields.getFieldValue(fieldName));
					//logger.trace("added NS to map: {} - {}", prefix, fields.getFieldValue(fieldName));
				}
				
			}
			
			// Add attributes to element.
			for (String fieldName : fieldNames) {
				try {
					if (fieldName.startsWith(A_ATTR_FIELD_PREFIX)) {
						String value = (String) fields.getFieldValue(fieldName);
						if (value != null) {
							String name = fieldName.substring(3).replace('.', ':');
							int nssep = name.indexOf(':');
							if (nssep == -1 || name.startsWith("xml:")) { // Handle xml namespace as no-namespace. 
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
					String msg = MessageFormatter.format("failed to pass attribute in field '{}' to XSLT: {}", fieldName, e.getMessage()).getMessage();
					logger.error(msg);
					throw new IndexingHandlerException(msg);
				}
			}

			
			XdmNode xdmDoc = db.build(new DOMSource(doc));
			return xdmDoc;
		} catch (IndexingHandlerException e) {
			throw e;
		} catch (Exception e) {
			throw new IndexingHandlerException("failed to pass attributes to XSLT", e);
		}
	}

	@Override
	public void begin(XmlSourceElement processedElement, XmlIndexElementId idProvider) throws XmlNotWellFormedException {
		
	}
	
	@Override
	public void end(XmlSourceElement processedElement, XmlIndexElementId idProvider, IndexingDoc fields) throws XmlNotWellFormedException {
		
		// processedElement is null during some unit testing. 
		
		if (processedElement instanceof XmlSourceElementS9api) {
			//logger.debug("reusing XdmNode for transformation");
			setSourceXdm((XmlSourceElementS9api) processedElement);
		} else {
			//logger.debug("parsing element source for transformation");
			setSourceFromField(fields, transformer);
		}
		
		ContentHandler transformOutputHandler = new ContentHandlerToIndexFields(fields);
		Destination xmltrackingFieldsHandler = new SAXDestination(transformOutputHandler);
		//Destination xmltrackingFieldsHandler = new net.sf.saxon.s9api.Serializer(System.out);
		
		transformer.setErrorListener(new LoggingErrorListener());
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
				String msg = MessageFormatter.format("XML invalid for transformation at {}: {}", processedElement, e.getMessage()).getMessage();
				throw new XmlNotWellFormedException(msg, e);
			}
			throw new RuntimeException("Extraction aborted with error at " + processedElement, e);
		}
		
	}
	
	private void setSourceXdm(XmlSourceElementS9api element) {
		
		
		try {
			XdmNode node = element.getElementXdm();			
			transformer.setInitialContextNode(node);
			
		} catch (Exception e) {

			throw new RuntimeException("failed to set context node for transformation", e);
		}
		
	}
	
	/**
	 * Places the original code (2012) into a separate method. 
	 * This approach re-parses each element during the recursion.
	 * @param fields
	 * @param transformer
	 */
	private void setSourceFromField(IndexingDoc fields, XsltTransformer transformer) {
		
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
		
		try {
			transformer.setSource(xmlInput);
		} catch (SaxonApiException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException("Error not handled", e);
		}
	}

	@Override
	public void startDocument(XmlIndexProgress xmlProgress) {
		
	}
	@Override
	public void endDocument() {
		
	}

}
