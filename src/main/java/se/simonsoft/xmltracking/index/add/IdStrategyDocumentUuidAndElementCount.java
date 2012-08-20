package se.simonsoft.xmltracking.index.add;

import se.simonsoft.xmltracking.source.XmlSourceElement;

/**
 * Generic id strategy with no need for a context, generates document id.
 * Warning: with this impl, reindexing requires empty schema or there'll be duplicates.
 */
public class IdStrategyDocumentUuidAndElementCount implements IdStrategy {

	@Override
	public void start() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Method not implemented");
	}

	@Override
	public String getElementId(XmlSourceElement element) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Method not implemented");
	}

}
