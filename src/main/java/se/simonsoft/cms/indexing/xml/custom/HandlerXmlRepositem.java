package se.simonsoft.cms.indexing.xml.custom;

import java.io.InputStream;

import javax.inject.Inject;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.Configuration;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.xml.sax.ContentHandler;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.IndexingItemProgress;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceDocumentS9api;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceReaderS9api;

/**
 * Extract XML information for repositem core.
 * 
 * @author takesson
 * 
 */
public class HandlerXmlRepositem {

	private static final Logger logger = LoggerFactory.getLogger(HandlerXmlRepositem.class);

	private transient Configuration config = XmlSourceReaderS9api.getConfiguration();
	private transient Processor processor;
	private transient XsltExecutable xsltCompiled;
	private transient XsltTransformer transformer; // if creation is fast we could be thread safe and load this for every read

	/**
	 * How to get document status from already extracted fields.
	 */
	public static final String STATUS_FIELD_NAME = "prop_cms.status";
	private static final QName STATUS_PARAM = new QName("document-status");

	@Inject
	public HandlerXmlRepositem() {
		// Hard-coding the XSL for now. Need to discuss whether to inject or configure with svn properties.
		InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
				"se/simonsoft/cms/indexing/xml/source/xml-indexing-repositem.xsl");
		init(new StreamSource(xsl));
	}

	private void init(Source xslt) {

		processor = new Processor(config);

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
			XdmNode node = xmlDoc.getXdmDoc();
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

		transformer.setErrorListener(new LoggingErrorListener());
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
