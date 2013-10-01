package se.simonsoft.cms.indexing.abx;

/**
 * We use ReleasePath and TranslationPath to place new slaves, but no longer to detect if a document is a release or a translation.
 * Use {@link IndexingItemHandlerAreaFromProperties} instead.
 */
public class IndexingItemHandlerAreaFromConfig {

	public IndexingItemHandlerAreaFromConfig() {
		throw new UnsupportedOperationException("use detection based on properties instead");
	}

}
