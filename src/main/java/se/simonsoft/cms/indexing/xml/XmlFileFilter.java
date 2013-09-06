package se.simonsoft.cms.indexing.xml;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;

public interface XmlFileFilter {

	/**
	 * @return true if the system should try to handle the item as xml
	 */
	boolean isXml(CmsChangesetItem c, IndexingDoc fields);
	
}
