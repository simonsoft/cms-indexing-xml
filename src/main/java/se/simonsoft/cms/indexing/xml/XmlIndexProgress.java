package se.simonsoft.cms.indexing.xml;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.item.CmsRepository;

/**
 * Keeps information about the current XML indexing operation.
 * 
 * @author takesson
 *
 */
public class XmlIndexProgress {
	
	private CmsRepository repository;
	private IndexingDoc baseDoc;

	public XmlIndexProgress(CmsRepository repository, IndexingDoc baseDoc) {
		this.repository = repository;
		this.baseDoc = baseDoc;
	}

	
	public CmsRepository getRepository() {
		return this.repository;
	}


	public IndexingDoc getBaseDoc() {
		return this.baseDoc;
	}

}
