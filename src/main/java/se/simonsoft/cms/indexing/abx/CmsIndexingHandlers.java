package se.simonsoft.cms.indexing.abx;

import java.util.LinkedHashSet;
import java.util.Set;

import se.repos.indexing.item.IndexingItemHandler;

/**
 * Used to get a complete set of handlers for SvnTestIndexing in different test scenarios.
 * Can't include XML indexing because it is a dowstream module.
 */
public class CmsIndexingHandlers {

	public static Set<IndexingItemHandler> getStructure() {
		return new LinkedHashSet<IndexingItemHandler>() {private static final long serialVersionUID = 1L;{
			
		}};
	}
	
	public static Set<IndexingItemHandler> getFulltext() {
		return new LinkedHashSet<IndexingItemHandler>() {private static final long serialVersionUID = 1L;{
			
		}};
	}
	
}
