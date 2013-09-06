package se.simonsoft.cms.indexing.xml;

import java.util.HashMap;
import java.util.Map;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.ItemPathinfo;
import se.repos.indexing.item.ItemProperties;

/**
 * Preprocesses the doc for common fields so that the old reposxml schema is supported while transitioning to the new repositem fields.
 */
public class SupportLegacySchema {

	/**
	 * Until {@link IndexingDoc#deepCopy()} can get only the {@link ItemPathinfo} and {@link ItemProperties} fields we use this to map repositem fields to reposxml schema.
	 * Key is field name, value is rename or null for using same name (we should end up with only nulls here).
	 */
	public static final Map<String, String> FIELDS_KEEP = new HashMap<String, String>() {private static final long serialVersionUID = 1L;{
		put("path", null);
	}};
	
	public void handle(IndexingDoc itemDoc) {
		// TODO Auto-generated method stub
	}

}
