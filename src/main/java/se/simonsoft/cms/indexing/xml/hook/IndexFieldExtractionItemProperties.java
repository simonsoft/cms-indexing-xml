package se.simonsoft.cms.indexing.xml.hook;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.properties.CmsItemProperties;
import se.simonsoft.xmltracking.index.add.IndexFieldExtraction;
import se.simonsoft.xmltracking.index.add.IndexFields;
import se.simonsoft.xmltracking.source.XmlSourceElement;

public class IndexFieldExtractionItemProperties implements IndexFieldExtraction {

	private static final Logger logger = LoggerFactory.getLogger(IndexFieldExtractionItemProperties.class);
	
	private IndexingContext context;

	@Inject
	public IndexFieldExtractionItemProperties(IndexingContext indexingContext) {
		this.context = indexingContext;
	}
	
	@Override
	public void extract(IndexFields fields, XmlSourceElement ignored) {
		CmsItemProperties props = context.getItemProperties();
		for (String prop : props.getKeySet()) {
			String val = props.getString(prop);
			if (val == null) {
				logger.warn("Property {} not readable as string, will not be indexed", prop);
			} else {
				fields.addField("prop_" + prop, val);
			}
		}
	}

}
