package se.simonsoft.cms.indexing.xml.fields;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.indexing.xml.XmlIndexElementId;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.xmlsource.XmlSourceAttributeMapRid;
import se.simonsoft.cms.xmlsource.content.XmlSourceLookup;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceDocumentS9api;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceElementS9api;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceReaderS9api;
import se.simonsoft.cms.xmlsource.transform.TransformerService;
import se.simonsoft.cms.xmlsource.transform.TransformerServiceFactory;

public class XmlIndexReleaseReuseChecksum implements XmlIndexFieldExtraction {
	
	private static String RELEASE_CHECKSUM = "c_sha1_release_source_reuse";
	
	private XmlSourceReaderS9api sourceReader = new XmlSourceReaderS9api();
	private XmlSourceLookup xmlLookup;
	
	InputStream xsl = this.getClass().getClassLoader().getResourceAsStream(
			"se/simonsoft/cms/xmlsource/transform/reuse-normalize.xsl");
	private TransformerService t = TransformerServiceFactory.buildTransformerService(new StreamSource(xsl));
	
	private Map<String, String> ridChecksums = null;
	private CmsItemId releaseId = null;

	private static final Logger logger = LoggerFactory.getLogger(XmlIndexReleaseReuseChecksum.class);

	public XmlIndexReleaseReuseChecksum() {

	}
	
	@Inject
	public void setXmlSourceLookup(XmlSourceLookup xmlLookup) {
		this.xmlLookup = xmlLookup;
	}

	@Override
	public void begin(XmlSourceElement processedElement, XmlIndexElementId idProvider) throws XmlNotWellFormedException {

	}

	@Override
	public void end(XmlSourceElement processedElement, XmlIndexElementId idProvider, IndexingDoc fields) throws XmlNotWellFormedException {
		
		String rid = (String) fields.getFieldValue("a_cms.rid");
		if (this.ridChecksums != null && rid != null) {
			String releaseChecksum = this.ridChecksums.get(rid);
			if (releaseChecksum == null || releaseChecksum.isEmpty()) {
				logger.warn("RID {} missing in Release: {}", rid, this.releaseId);
				// TODO: Add some flag?
			}
			
			fields.addField(RELEASE_CHECKSUM, releaseChecksum);
			logger.info("Added Release checksum {} to RID {}", releaseChecksum, rid);
		} 
	}
	
	@Override
	public void startDocument(IndexingDoc baseDoc) {
		
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
			throw new IllegalArgumentException("Document can not be classified 'translation' when TranslationMaster is not specified.");
		}
		
		CmsItemId tmId = new CmsItemIdArg(tmProp);
		// The version of Release requested must be same as indexed.
		CmsItemId revId = tmId.withPegRev(rev);
		XmlSourceElementS9api doc = (XmlSourceElementS9api) xmlLookup.getElementDocument(revId);
		
		XmlSourceDocumentS9api docReuse = t.transform(doc, new HashMap<String, Object>());
		
		XmlSourceAttributeMapRid map = new XmlSourceAttributeMapRid("c_sha1_source_reuse");
		sourceReader.handle(docReuse, map);
		this.ridChecksums = map.getAttributeMap();
		this.releaseId = tmId;
		logger.info("Processed RID-map ({}) for Release: {}", this.ridChecksums.size(), tmId);
	}
	
	@Override
	public void endDocument() {
		this.ridChecksums = null;
		this.releaseId = null;

	}

}
