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
package se.simonsoft.cms.indexing.xml.fields;

import java.io.Reader;
import java.io.StringReader;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.indexing.xml.XmlIndexElementId;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.indexing.xml.XmlIndexProgress;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceAttribute;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceDocumentS9api;
import se.simonsoft.cms.xmlsource.transform.TransformOptions;
import se.simonsoft.cms.xmlsource.transform.TransformerService;
import se.simonsoft.cms.xmlsource.transform.TransformerServiceFactory;

public class XmlIndexFieldXslPipeline implements XmlIndexFieldExtraction {

	private static final Logger logger = LoggerFactory.getLogger(XmlIndexFieldXslPipeline.class);
	
	private final TransformerService t;
	private final TransformerService tNormalize; // Only for testing.

	
	public static final String STATUS_FIELD_NAME = "prop_cms.status";
	public static final String PATHAREA_FIELD_NAME = "patharea";
	public static final String RID_PROP_FIELD_NAME = "prop_abx.ReleaseId";
	public static final String TPROJECT_PROP_FIELD_NAME = "prop_abx.TranslationProject";
	public static final String PROPNAME_DITAMAP_FIELD_NAME = "prop_abx.Ditamap";
	private static final String STATUS_PARAM = "document-status";
	private static final String PATHAREA_PARAM = "patharea";
	private static final String DEPTH_PARAM = "reposxml-depth";
	private static final String PATHEXT_PARAM = "pathext";
	@SuppressWarnings("unused")
	private static final String DITAMAP_PARAM = "ditamap";
	
	@Inject
	public XmlIndexFieldXslPipeline(TransformerServiceFactory transformerServiceFactory) {
		this.t = transformerServiceFactory.buildTransformerService("xml-indexing-reposxml.xsl");
		this.tNormalize = transformerServiceFactory.buildTransformerService("reuse-normalize.xsl"); // Only Unit testing.
	}
	
	/*
	// For testing
	public XmlIndexFieldXslPipeline(TransformerService tReposxml, TransformerService tNormalize) {
		this.t = tReposxml;
		this.tNormalize = tNormalize;
	}
	*/
	
	
	@Override
	public void startDocument(XmlIndexProgress xmlProgress) {

	}

	public void endDocument() {

	}

	@Override
	public void begin(XmlSourceElement element, XmlIndexElementId idProvider) throws XmlNotWellFormedException {

	}

	@Override
	public void end(XmlSourceElement element, XmlIndexElementId idProvider, IndexingDoc doc) {

		// element is null during some unit testing. 
		if (element == null) {
			logger.warn("Unit testing only: parsing field 'source' for transformation.");
			XmlSourceDocumentS9api sourceDoc = getSourceFromField(doc);
			element = doTransformPipeline(sourceDoc, doc).getDocumentElement();
		}
		
		for (XmlSourceAttribute a : element.getAttributes()) {
			if (a.getName().startsWith("cmsreposxml:")) {
				String fieldName = a.getName().substring(12);
				logger.trace("Field from attribute: {} - {}", fieldName, a.getName());
				doc.addField(fieldName, a.getValue());
			}
		}
	}
	
	public XmlSourceDocumentS9api doTransformPipeline(XmlSourceDocumentS9api source, IndexingDoc doc) {
		return this.t.transform(source, getTransformOptionsFromFields(doc));
	}

	public static TransformOptions getTransformOptionsFromFields(IndexingDoc fields) {

		TransformOptions options = new TransformOptions();
		// Status as parameter to XSL.
		Object status = fields.getFieldValue(STATUS_FIELD_NAME);
		if (status != null) {
			options.setParameter(STATUS_PARAM, (String) status);
		}

		// Patharea as parameter to XSL.
		Object patharea = fields.getFieldValue(PATHAREA_FIELD_NAME);
		if (patharea != null) {
			options.setParameter(PATHAREA_PARAM, (String) patharea);
		}

		Integer depth = XmlIndexFieldExtraction.getDepthReposxml(fields);
		if (depth != null) {
			options.setParameter(DEPTH_PARAM, new Long(depth));
		}
		
		// The file extension field must always be extracted.
		/* Only repositem
		options.setParameter(PATHEXT_PARAM, (String) fields.getFieldValue("pathext"));
		*/

		// NOT supported in reposxml transform, until a requirement comes up.
		// #1345 Make Release / Translation ditamap available for extraction.
		// The ditamap property is suppressed in reposxml but included in repositem.
		/*
		final String ditamapStr = (String) fields.getFieldValue(PROPNAME_DITAMAP_FIELD_NAME);
		if (ditamapStr != null) {
			// The ditamap is provided as a Transform parameter.
			XmlSourceDocumentS9api ditamap = sourceReader.read(new ByteArrayInputStream(ditamapStr.getBytes(StandardCharsets.UTF_8)));
			options.setParameter(DITAMAP_PARAM, ditamap.getDocumentNodeXdm());
		}
		*/
		return options;
	}

	
	/**
	 * Unit testing ONLY.
	 * Places the original code (2012) into a separate method. 
	 * This approach re-parses each element during the recursion.
	 * @param fields
	 * @param transformer
	 */
	public XmlSourceDocumentS9api getSourceFromField(IndexingDoc fields) {
		
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
		
		TransformOptions options = new TransformOptions();
		options.setParameter("source-reuse-tags-param", "*");
		return this.tNormalize.transform(sourceReader, options);
	}

	
}
