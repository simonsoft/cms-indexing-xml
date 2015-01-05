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
package se.simonsoft.cms.indexing.xml.fields;

import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import se.repos.indexing.HandlerException;
import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.ItemContentBuffer;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.simonsoft.cms.indexing.xml.XmlIndexElementId;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.indexing.xml.XmlIndexProgress;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.inspection.CmsRepositoryInspection;
import se.simonsoft.cms.xmlsource.XmlSourceAttributeMapRid;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceDocumentS9api;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceElementS9api;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceReaderS9api;
import se.simonsoft.cms.xmlsource.transform.TransformerService;
import se.simonsoft.cms.xmlsource.transform.TransformerServiceFactory;

@SuppressWarnings("deprecation")
public class XmlIndexReleaseReuseChecksum implements XmlIndexFieldExtraction {

	private static String RELEASE_CHECKSUM = "c_sha1_release_source_reuse";

	private XmlSourceReaderS9api sourceReader = new XmlSourceReaderS9api();
	private ItemContentBufferStrategy contentStrategy;

	InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
			"se/simonsoft/cms/xmlsource/transform/reuse-normalize.xsl");
	private TransformerService t = TransformerServiceFactory.buildTransformerService(new StreamSource(xsl));

	private Map<String, String> ridChecksums = null;
	private CmsItemId releaseId = null;

	private static final Logger logger = LoggerFactory.getLogger(XmlIndexReleaseReuseChecksum.class);

	public XmlIndexReleaseReuseChecksum() {

	}

	@Inject
	public void setItemContentBufferStrategy(ItemContentBufferStrategy contentStrategy) {
		this.contentStrategy = contentStrategy;
	}

	@Override
	public void begin(XmlSourceElement processedElement, XmlIndexElementId idProvider) throws XmlNotWellFormedException {

	}

	@Override
	public void end(XmlSourceElement processedElement, XmlIndexElementId idProvider, IndexingDoc fields) throws XmlNotWellFormedException {

		// Must remove size at this time.
		fields.removeField("size");
		
		String rid = (String) fields.getFieldValue("a_cms.rid");
		if (this.ridChecksums != null && rid != null) {
			String releaseChecksum = this.ridChecksums.get(rid);
			if (releaseChecksum == null || releaseChecksum.isEmpty()) {
				String msg = MessageFormatter.format("RID {} missing in Release: {}", rid, this.releaseId).getMessage();
				logger.warn(msg);
				throw new HandlerException(msg);
				// Verifies that elements have not been removed from Release. Will not notice if elements without RID have been added to Translation.
				// This could trigger error if the Translation has inline element with RID, but the Release does not.
			}
			// TODO: Identify if there are missing elements in Translation. Perhaps introduce a "Validate Translation" early in HandlerXml instead.

			fields.addField(RELEASE_CHECKSUM, releaseChecksum);
			logger.info("Added Release checksum {} to RID {}", releaseChecksum, rid);
		}
	}

	@Override
	public void startDocument(XmlIndexProgress xmlProgress) {

		IndexingDoc baseDoc = xmlProgress.getBaseDoc();

		String id = (String) baseDoc.getFieldValue("id");
		Long rev = (Long) baseDoc.getFieldValue("rev");
		Collection<Object> pathArea = baseDoc.getFieldValues("patharea");

		if (pathArea == null || !pathArea.contains("translation")) {
			logger.info("File is not a Translation: " + id);
			return;
		}

		logger.info("File is a Translation: " + id);
		String tmProp = (String) baseDoc.getFieldValue("prop_abx.TranslationMaster");
		if (tmProp == null) {
			throw new HandlerException("Document can not be classified 'translation' when TranslationMaster is not specified.");
		}

		CmsItemId tmId = new CmsItemIdArg(tmProp);
		// The version of Release requested must be same as indexed.
		CmsItemId revId = tmId.withPegRev(rev);
		
		Date start = new Date();
		XmlSourceDocumentS9api docReuse;
		try {
			docReuse = getDocumentChecksum(xmlProgress, revId);
		} catch (Exception e) {
			String msg = MessageFormatter.format("Failed to process related Release document: {}", revId, e).getMessage();
			logger.warn(msg);
			throw new HandlerException(msg);
					
		}

		XmlSourceAttributeMapRid map = new XmlSourceAttributeMapRid("c_sha1_source_reuse");
		sourceReader.handle(docReuse, map);
		this.ridChecksums = map.getAttributeMap();
		this.releaseId = tmId;
		Date end = new Date();
		logger.info("Processed RID-map ({}) in {} ms for Release: {}", this.ridChecksums.size(), end.getTime() - start.getTime(), tmId);
		// TODO: Send to a cache.
	}

	private XmlSourceDocumentS9api getDocumentChecksum(XmlIndexProgress xmlProgress, CmsItemId itemId) {

		// Requires lookup, which is available in webapp and testing but not in indexing.
		//XmlSourceElementS9api releaseDoc = (XmlSourceElementS9api) xmlLookup.getElementDocument(revId);
		// XmlSourceDocumentS9api docReuse = t.transform(releaseDoc, new HashMap<String, Object>());
		// Not sure how we could access content here without using the deprecated multi-repo methods.
		//contentsReader.getContents(xmlProgress.getRepository(), itemId.getPegRev(), itemId.getRelPath(), out);
		ItemContentBuffer releaseBuffer = contentStrategy.getBuffer((CmsRepositoryInspection) xmlProgress.getRepository(), new RepoRevision(itemId.getPegRev(), null), itemId.getRelPath(), xmlProgress.getBaseDoc());
		XmlSourceDocumentS9api releaseDoc = sourceReader.read(releaseBuffer.getContents());
		XmlSourceElementS9api releaseElement = sourceReader.buildSourceElement(XmlSourceReaderS9api.getDocumentElement(releaseDoc.getXdmDoc()));
		// Execute Transform that calculates checksums on Release.
		XmlSourceDocumentS9api docReuse = t.transform(releaseElement, new HashMap<String, Object>());
		
		return docReuse;
	}

	@Override
	public void endDocument() {
		this.ridChecksums = null;
		this.releaseId = null;

	}

}
