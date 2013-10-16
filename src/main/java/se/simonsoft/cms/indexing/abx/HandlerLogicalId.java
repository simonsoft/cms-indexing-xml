package se.simonsoft.cms.indexing.abx;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.simonsoft.cms.item.CmsItemId;

/**
 * Sets the {@value #FIELD_NAME} field.
 */
public abstract class HandlerLogicalId implements IndexingItemHandler {

	public static final String FIELD_NAME = "urlid";
	
	@Override
	public void handle(IndexingItemProgress progress) {
		CmsItemId itemId = getItemId(progress);
		if (itemId != null) {
			// use addField istead of setField so we detect conflicts with other id resolution strategies
			progress.getFields().addField(FIELD_NAME, itemId.getLogicalIdFull());
		}
	}
	
	/**
	 * @param progress from {@link #handle(IndexingItemProgress)}
	 * @return null to avoid setting the field, an id preferrably with peg rev to set the field
	 */
	protected abstract CmsItemId getItemId(IndexingItemProgress progress);

}
