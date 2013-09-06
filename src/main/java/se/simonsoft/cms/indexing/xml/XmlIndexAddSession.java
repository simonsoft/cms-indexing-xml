package se.simonsoft.cms.indexing.xml;

import java.util.Collection;

import se.repos.indexing.IndexingDoc;

public interface XmlIndexAddSession extends Collection<IndexingDoc> {

	/**
	 * Called to end session, typically when the XML file is completely read.
	 */
	void end();
	
}
