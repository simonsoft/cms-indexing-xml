package se.simonsoft.cms.indexing.abx;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.ItemProperties;
import se.simonsoft.cms.item.impl.CmsItemIdArg;

/**
 * Uses the abx:Dependencies property, splitting on newline, to add fields ref + refid + refurl.
 */
public class IndexingItemHandlerAbxDependencies implements IndexingItemHandler {

	private static final Logger logger = LoggerFactory.getLogger(IndexingItemHandlerAbxDependencies.class);
	
	private static final String HOSTFIELD = "repohost";
	
	@Override
	public void handle(IndexingItemProgress progress) {
		IndexingDoc fields = progress.getFields();
		String host = (String) fields.getFieldValue(HOSTFIELD);
		if (host == null) {
			throw new IllegalStateException("Depending on indexer that adds host field " + HOSTFIELD);
		}
		String abxprop = (String) fields.getFieldValue("prop_abx.Dependencies");
		if (abxprop == null) {
			return;
		}
		if (abxprop.length() == 0) {
			logger.debug("abx:Dependencies property exists but is empty");
			return;
		}
		for (String d : abxprop.split("\n")) {
			fields.addField("refid", d);
			CmsItemIdArg id = new CmsItemIdArg(d);
			id.setHostname(host);
			String url = id.getUrl();
			if (id.isPegged()) {
				url = url + "?p=" + id.getPegRev();
			}
			fields.addField("refurl", url);
		}
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return new HashSet<Class<? extends IndexingItemHandler>>() {private static final long serialVersionUID = 1L;{
			add(ItemProperties.class);
		}};
	}

}
