package se.simonsoft.xmltracking.index.add;

import java.util.Collection;

/**
 * Current field set for indexing, used in {@link IndexFieldExtraction}.
 */
public interface IndexFields {

	/**
	 * @return Names of all added fields.
	 */
	Collection<String> getFieldNames();
	
	/**
	 * Adds field  value of arbitrary type
	 * @param name Name that matches an indexing schema field
	 * @param value Valy with type matching the indexing schema field
	 */
	void addField(String name, Object value);
	
	/**
	 * @return Current field value or null if the field does not exist
	 */
	Object getFieldValue(String name);
	
}
