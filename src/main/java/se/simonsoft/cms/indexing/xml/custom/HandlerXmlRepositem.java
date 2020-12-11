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
package se.simonsoft.cms.indexing.xml.custom;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.xml.sax.ContentHandler;

import net.sf.saxon.s9api.Destination;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SAXDestination;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.IndexingItemProgress;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceDocumentS9api;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceReaderS9api;
import se.simonsoft.cms.xmlsource.transform.SaxonMessageListener;

/**
 * Extract XML information for repositem core.
 * 
 * @author takesson
 * 
 */
public class HandlerXmlRepositem {

	private static final Logger logger = LoggerFactory.getLogger(HandlerXmlRepositem.class);

	private transient Processor processor;
	private transient XmlSourceReaderS9api sourceReader;
	private transient XsltExecutable xsltCompiled;
	private transient XsltTransformer transformer; // if creation is fast we could be thread safe and load this for every read

	/**
	 * How to get document status from already extracted fields.
	 */
	public static final String STATUS_FIELD_NAME = "prop_cms.status";
	public static final String PATHAREA_FIELD_NAME = "patharea";
	public static final String RID_PROP_FIELD_NAME = "prop_abx.ReleaseId";
	public static final String TPROJECT_PROP_FIELD_NAME = "prop_abx.TranslationProject";
	public static final String PROPNAME_DITAMAP_FIELD_NAME = "prop_abx.Ditamap";
	private static final QName STATUS_PARAM = new QName("document-status");
	private static final QName PATHAREA_PARAM = new QName("patharea");
	private static final QName PATHEXT_PARAM = new QName("pathext");
	private static final QName DITAMAP_PARAM = new QName("ditamap");

	@Inject
	public HandlerXmlRepositem(Processor processor) {
		
		this.processor = processor;
		this.sourceReader = new XmlSourceReaderS9api(processor);
		// Hard-coding the XSL for now. Need to discuss whether to inject or configure with svn properties.
		InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
				"se/simonsoft/cms/indexing/xml/source/xml-indexing-repositem.xsl");
		init(new StreamSource(xsl));
	}

	private void init(Source xslt) {

		XsltCompiler compiler = processor.newXsltCompiler();
		try {
			xsltCompiled = compiler.compile(xslt);
		} catch (SaxonApiException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException("Error not handled", e);
		}

		transformer = xsltCompiled.load();
	}

	public void handle(IndexingItemProgress progress, XmlSourceDocumentS9api xmlDoc) {

		IndexingDoc fields = progress.getFields();
		CmsChangesetItem processedFile = progress.getItem();

		try {
			XdmNode node = xmlDoc.getDocumentNodeXdm(); //Starting from the actual DOCUMENT node.
			transformer.setInitialContextNode(node);

		} catch (Exception e) {
			throw new RuntimeException("failed to set context node for transformation", e);
		}

		ContentHandler transformOutputHandler = new ContentHandlerToIndexFields(progress.getFields());
		Destination xmltrackingFieldsHandler = new SAXDestination(transformOutputHandler);
		//Destination xmltrackingFieldsHandler = new net.sf.saxon.s9api.Serializer(System.out);

				
		// Status as parameter to XSL.
		Object status = fields.getFieldValue(STATUS_FIELD_NAME);
		if (status != null) {
			transformer.setParameter(STATUS_PARAM, new XdmAtomicValue((String) status));
		}
		
		// Patharea as parameter to XSL.
		Object patharea = fields.getFieldValue(PATHAREA_FIELD_NAME);
		if (patharea != null) {
			transformer.setParameter(PATHAREA_PARAM, new XdmAtomicValue((String) patharea));
		}
		
		// The file extension field must always be extracted.
		transformer.setParameter(PATHEXT_PARAM, new XdmAtomicValue((String) fields.getFieldValue("pathext")));

		// #1345 Make Release / Translation ditamap available for extraction.
		// The ditamap property is suppressed in reposxml but included in repositem.
		final String ditamapStr = (String) fields.getFieldValue(PROPNAME_DITAMAP_FIELD_NAME);
		if (ditamapStr != null) {
			// The ditamap is provided as a Transform parameter.
			XmlSourceDocumentS9api ditamap = sourceReader.read(new ByteArrayInputStream(ditamapStr.getBytes(StandardCharsets.UTF_8)));
			transformer.setParameter(DITAMAP_PARAM, ditamap.getDocumentNodeXdm());
		} 
		
		
		transformer.setErrorListener(new LoggingErrorListener());
		transformer.setMessageListener(new SaxonMessageListener());
		transformer.setDestination(xmltrackingFieldsHandler);

		try {
			transformer.transform();
		} catch (SaxonApiException e) {
			if (e.getCause() instanceof TransformerException) { // including net.sf.saxon.trans.XPathException
				String msg = MessageFormatter.format("XML invalid for transformation at {}: {}", processedFile, e.getMessage()).getMessage();
				logger.error(msg);
				throw new XmlNotWellFormedException(msg, e);
			}
			throw new RuntimeException("Extraction aborted with error at " + processedFile, e);
		}

	}

}
