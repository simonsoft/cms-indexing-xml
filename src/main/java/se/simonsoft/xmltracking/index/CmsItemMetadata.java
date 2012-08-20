package se.simonsoft.xmltracking.index;

import se.simonsoft.xmltracking.index.add.IdStrategy;
import se.simonsoft.xmltracking.index.add.IndexFieldExtraction;

/**
 * Provides access to metadata in IdStrategy#getElementId and IndexFieldExtensions.
 * 
 * Do we need this?
 * - Path is better handled in {@link IdStrategy} impl
 * - Revision could be added using index fields pre-indexing service.
 *   See {@link IndexFieldExtraction}
 * - Custom notifications to this impl by the caller of XmlSourceHandler
 * @deprecated No, we don't need this, too inflexible
 */
public interface CmsItemMetadata {

	/**
	 * @return Identification, possibly URL? Cooperation with {@link IdStrategy}
	 */
	String getPathNormalized();
	
	/**
	 * @return Current document's revision, null if not versioned
	 */
	Long getRevisionNumber();
	
}
