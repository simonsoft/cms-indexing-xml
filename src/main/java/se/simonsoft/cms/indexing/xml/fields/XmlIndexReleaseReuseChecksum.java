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

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingHandlerException;
import se.repos.indexing.item.ItemContentBuffer;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.simonsoft.cms.indexing.xml.XmlIndexElementId;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.indexing.xml.XmlIndexProgress;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.xmlsource.XmlSourceAttributeMapRid;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;
import se.simonsoft.cms.xmlsource.handler.XmlSourceReader;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceDocumentS9api;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceElementS9api;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceReaderS9api;
import se.simonsoft.cms.xmlsource.transform.TransformOptions;
import se.simonsoft.cms.xmlsource.transform.TransformerService;
import se.simonsoft.cms.xmlsource.transform.TransformerServiceFactory;

public class XmlIndexReleaseReuseChecksum implements XmlIndexFieldExtraction {

	private static String RELEASE_CHECKSUM = "c_sha1_release_source_reuse";
	private static String RELEASE_DESCENDANTS_CHECKSUM = "reuse_c_sha1_release_descendants";
	private static String RELEASE_RID_PREFIX = "reuse_rid_";

	private static String RELEASE_RID_REUSEVALUE = "reuseridreusevalue";

	private static String PROP_RELEASEID = "prop_abx.ReleaseId";

	private XmlSourceReaderS9api sourceReader;
	private ItemContentBufferStrategy contentStrategy;
	private TransformerServiceFactory transformerServiceFactory;

	private TransformerService t;

	private Map<String, String> ridChecksums = null;
	private CmsItemId releaseId = null;

	private Map<CmsItemId, Map<String, String>> cache = new HashMap<>(); // Consider using Caffeine, for now only keeping one entry.
	
	private static final Logger logger = LoggerFactory.getLogger(XmlIndexReleaseReuseChecksum.class);

	@Inject
	public XmlIndexReleaseReuseChecksum(XmlSourceReader sourceReader, TransformerServiceFactory transformerServiceFactory) {
		
		this.sourceReader = (XmlSourceReaderS9api) sourceReader;
		this.transformerServiceFactory = transformerServiceFactory;
		t = this.transformerServiceFactory.buildTransformerService("reuse-normalize.xsl");

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
		Integer depth = (Integer) fields.getFieldValue("depth");
		String ridProp = (String) fields.getFieldValue(PROP_RELEASEID);
		
		// Translations prepared in earlier versions of CMS do not have the property abx:ReleaseId (now propagates from Release).
		// This property is required by Pretranslate 2.0.x.
		// The property should contain the RID of the root element. Simple fix when depth=1.
		if (this.releaseId != null && ridProp == null && depth.equals(1)) {
			logger.warn("Translation prepared without ReleaseId property, adding: " + rid);
			fields.setField(PROP_RELEASEID, rid);
		}
		
		// TODO: Likely need to take tsuppress, tvalidate into account.
		if (hasAncestorAttributeCmsActive(fields, "tsuppress")) {
			logger.info("Suppressing element due to ancestor tsuppress attribute: {}", rid);
			return;
		}
		if (hasAncestorAttributeCmsInActive(fields, "tvalidate")) {
			logger.info("Suppressing element due to ancestor tvalidate attribute: {}", rid);
			return;
		}
		

		if (this.ridChecksums != null && rid != null) {
			String releaseChecksum = this.ridChecksums.get(rid);
			if (releaseChecksum == null || releaseChecksum.isEmpty()) {
				String msg = MessageFormatter.format("RID {} missing in Release: {}", rid, this.releaseId).getMessage();
				logger.warn(msg);
				throw new IndexingHandlerException(msg);
				// Verifies that elements have not been removed from Release. Will not notice if elements without RID have been added to Translation.
				// This could trigger error if the Translation has inline element with RID, but the Release does not.
			}
			
			// TODO: Identify if there are missing elements in Translation. Perhaps introduce a "Validate Translation" early in HandlerXml instead.

			// Checksum field used by Pretranslate 1.x (non-join).
			fields.addField(RELEASE_CHECKSUM, releaseChecksum);
			logger.trace("Added Release checksum {} to RID {}", releaseChecksum, rid);
			
			// Add checksums for elements with reusevalue > 0 (on root element only). Used by Pretranslate 2.0.x.
			if (depth.equals(1)) {
				addDescendantChecksums(fields);
			}
		}
	}
	
	
	private void addDescendantChecksums(IndexingDoc fields) {
		
		String ridStr = (String) fields.getFieldValue(RELEASE_RID_REUSEVALUE);
		//logger.trace("RIDs with reusevalue > 0: {}", ridStr); // Very large logging.
		if (ridStr == null || ridStr.trim().isEmpty()) {
			logger.warn("No RIDs with reusevalue > 0");
			return;
		}
		
		List<String> rids = Arrays.asList(ridStr.split(" "));
		logger.debug("RID count with reusevalue > 0: {}", rids.size());
		
		// Add checksums for elements with reusevalue > 0.
		HashMap<String, TreeMap<String, String>> shards = new HashMap<>();
		for (String key: rids) {
			String checksum = this.ridChecksums.get(key);
			if (checksum == null) {
				logger.warn("RID has reusevalue > 0 but no checksum available in Release: {}", key);
				continue;
			}
			logger.trace("RID with reusevalue > 0: {} {}", key, checksum);
			
			// Field (multivalue) with all valid checksums.
			fields.addField(RELEASE_DESCENDANTS_CHECKSUM, checksum);
			
			// Sharding based on 2 characters. Generates total 256 fields across the whole search core. 
			String shardKey = checksum.substring(0, 2);
			TreeMap<String, String> shard = shards.get(shardKey);
			if (shard == null) {
				shard = new TreeMap<>();
				shards.put(shardKey, shard);
			}
			// Mapping checksum to RID by concating them.
			String hashRid = checksum + " " + key;
			
			// Index the first instance when there are elements with identical checksum.
			if (!shard.containsKey(checksum)) {
				shard.put(checksum, hashRid);
				//logger.info("Added checksum map field {}", fieldname);
			}
		}
		// Add the shards as SortedSets to indexing.
		for (String shardKey: shards.keySet()) {
			String fieldName = RELEASE_RID_PREFIX.concat(shardKey);
			TreeMap<String, String> shardContent = shards.get(shardKey);
			// The TreeMap will return the values in sorted order (key is prefix of the value).
			for (String v: shardContent.values()) { 
				fields.addField(fieldName, v);
			}
		}
	}

	@Override
	public void startDocument(XmlIndexProgress xmlProgress) {

		// Clearing the RID data structures. Can carry over from previous document if it failed with exception.
		this.ridChecksums = null;
		this.releaseId = null;
				
		IndexingDoc baseDoc = xmlProgress.getBaseDoc();

		String id = (String) baseDoc.getFieldValue("id");
		Long rev = (Long) baseDoc.getFieldValue("rev");
		Collection<Object> pathArea = baseDoc.getFieldValues("patharea");

		if (pathArea == null || !pathArea.contains("translation")) {
			logger.debug("File is not a Translation: " + id);
			return;
		}
		
		logger.info("File is a Translation: " + id);
		String tmProp = (String) baseDoc.getFieldValue("prop_abx.TranslationMaster");
		if (tmProp == null) {
			throw new IndexingHandlerException("Document can not be classified 'translation' when TranslationMaster is not specified.");
		}
		
		// Make sure the Translation was made with CMS 2.0 or later.
		// Verifying property containing the top RID would make most sense but it might not be set on very early Translations.
		String tProjectProp = (String) baseDoc.getFieldValue("prop_abx.TranslationProject");
		if (tProjectProp == null) {
			logger.info("File is a Translation created with CMS 1.x: " + id);
			return;
		}

		CmsItemId tmId = new CmsItemIdArg(tmProp);
		// The version of Release requested must be same as indexed (Release is same commit or most recent).
		// Ensures identical result after re-indexing even if Release has been updated (must iterate Translation if updated Release checksums are desirable).
		CmsItemId revId = tmId.withPegRev(rev);
		// TODO: Get the commit revision of the Release in order to determine a cache key.
		// For now, using the indexed revision (Translation commit) due to lack of APIs.
		// Works for Translations committed in same transaction.
		// Using Release commit revision would work across indexed revision (e.g. Pretranslate completing each item).
		
		this.ridChecksums = getChecksumMap(xmlProgress, revId);
		this.releaseId = tmId;
		
	}
	
	private Map<String, String> getChecksumMap(XmlIndexProgress xmlProgress, CmsItemId revId) {
		// TODO: #1703 Can we get the checksum of an itemid?
		Map<String, String> result = doCacheGet(revId);
		if (result != null) {
			logger.info("RID-map cache hit ({}) for Release: {}", result.size(), revId);
			return result;
		}
		
		Date start = new Date();
		XmlSourceDocumentS9api docReuse;
		try {
			docReuse = getDocumentChecksum(xmlProgress, revId);
		} catch (UnsupportedOperationException e) {
			throw new RuntimeException("The indexing backend can not support this handler.", e);
		} catch (Exception e) {
			String msg = MessageFormatter.format("Failed to process related Release document: {}", revId, e).getMessage();
			logger.warn(msg);
			throw new IndexingHandlerException(msg);
		}

		XmlSourceAttributeMapRid map = new XmlSourceAttributeMapRid("c_sha1_source_reuse");
		sourceReader.handle(docReuse, map);
		result = map.getAttributeMap();
		Date end = new Date();
		logger.info("RID-map processed ({}) in {} ms for Release: {}", result.size(), end.getTime() - start.getTime(), revId);
		// Send to a cache.
		doCachePut(revId, result);
		
		return result;
	}

	private XmlSourceDocumentS9api getDocumentChecksum(XmlIndexProgress xmlProgress, CmsItemId itemId) {

		// Possible to use the XmlSourceReader in combination with the Indexing Content Buffer concept.
		// The ItemContentBuffer implementation is injected in a per-repo context, same with Handlers.
		ItemContentBuffer releaseBuffer = contentStrategy.getBuffer(new RepoRevision(itemId.getPegRev(), null), itemId.getRelPath(), xmlProgress.getBaseDoc());
		XmlSourceDocumentS9api releaseDoc = sourceReader.read(releaseBuffer.getContents());
		XmlSourceElementS9api releaseElement = releaseDoc.getDocumentElement();
		// Execute Transform that calculates checksums on Release.
		
		// Set parameters to not preserve text/comment/pi.
		TransformOptions options = new TransformOptions();
		options.setParameter("preserve-text", Boolean.FALSE);
		options.setParameter("preserve-comment", Boolean.FALSE);
		options.setParameter("preserve-pi", Boolean.FALSE);
		XmlSourceDocumentS9api docReuse = t.transform(releaseElement, options);
		
		return docReuse;
	}
	
	private void doCachePut(CmsItemId release, Map<String, String> map) {
		this.cache.clear(); // Keeping only a single item for now.
		this.cache.put(release, map);
	}
	
	private Map<String, String> doCacheGet(CmsItemId release) {
		// TODO: #1703 Cache based in sha1 instead, frequently missing the cache due to rev based on the translation.
		return this.cache.get(release);
	}
	

	@Override
	public void endDocument() {
		// Clearing the RID data structures. Too dangerous to do caching by just keeping them.
		this.ridChecksums = null;
		this.releaseId = null;

	}
	
	private boolean hasAncestorAttributeCmsActive(IndexingDoc fields, String attrname) {
		
		String value = (String) fields.getFieldValue("aa_cms." + attrname);
		
		if (value != null && !"no".equalsIgnoreCase(value)) {
			return true;
		}
		return false;
	}
	
	private boolean hasAncestorAttributeCmsInActive(IndexingDoc fields, String attrname) {
		
		String value = (String) fields.getFieldValue("aa_cms." + attrname);
		
		if (value != null && !"yes".equalsIgnoreCase(value)) {
			return true;
		}
		return false;
	}

}
