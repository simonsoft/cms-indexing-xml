package se.simonsoft.cms.indexing;

import java.util.Collection;

/**
 * Support duplication into different core, with additional data.
 * Support update of existing fields or adding new ones.
 */
interface IndexItem {

	/**
	 * ID field name is always "id"
	 * @return ID of this item in index, 
	 */
	public String getId();
	
	/**
	 * @return Names of all added fields.
	 */
	public abstract Collection<String> getFieldNames();

	/**
	 * @return Current field value or null if the field does not exist
	 */
	public abstract Object getFieldValue(String name);

	/**
	 * @return only the fields that were added
	 */
	public Collection<String> getFieldNamesAdded();
	
	/**
	 * @return only the fields that existed but have been changed
	 */
	public Collection<String> getFieldNamesUpdated();
	
	/**
	 * Copies current instance but sets mark so that the new instance's
	 * {@link #getFieldNamesAdded()} and {@link #getFieldNamesUpdated()} only
	 * reflects subsequent changes.
	 * @return Exact copy of the current 
	 */
	public abstract IndexFields getCopy();

}