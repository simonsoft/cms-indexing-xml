package se.simonsoft.cms.indexing.xml.hook;

import javax.inject.Inject;

import se.simonsoft.xmltracking.index.add.IdStrategy;
import se.simonsoft.xmltracking.source.XmlSourceElement;

/**
 * Predictable IDs, supporting overwrite/reindexing, using {@link IndexingContext}
 * to get repository name + revision + item and adding element number increment.
 */
public class IdStrategyRepoRevisionItemElem implements IdStrategy {

	private IndexingContext context;
	
	private transient int n = Integer.MIN_VALUE;
	private String doc = null;
	
	@Inject
	public IdStrategyRepoRevisionItemElem(IndexingContext indexingContext) {
		this.context = indexingContext;
	}
	
	@Override
	public void start() {
		n = 0;
		doc = context.getRepository().getName() + "^" + context.getItemPath() + "?p=" + context.getRevision().getNumber();
	}

	@Override
	public String getElementId(XmlSourceElement element) {
		if (doc == null) {
			throw new IllegalStateException("Id strategy not initialized with a document id, start method must be called for each item");
		}
		return doc + "#" + n++;
	}

}
