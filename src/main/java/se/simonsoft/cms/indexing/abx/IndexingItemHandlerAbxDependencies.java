package se.simonsoft.cms.indexing.abx;

import java.util.HashSet;
import java.util.Set;

import se.repos.indexing.item.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.ItemProperties;

/**
 * Uses the abx:Dependencies property, splitting on newline, to add fields ref + refid + refurl.
 */
public class IndexingItemHandlerAbxDependencies implements IndexingItemHandler {

	@Override
	public void handle(IndexingItemProgress progress) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return new HashSet<Class<? extends IndexingItemHandler>>() {{
			add(ItemProperties.class);
		}};
	}

}
