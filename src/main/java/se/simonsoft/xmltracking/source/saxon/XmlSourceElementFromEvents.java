package se.simonsoft.xmltracking.source.saxon;

import se.simonsoft.xmltracking.source.XmlSourceElement;

/**
 * Xml (indexing) element data collected using the SAX model,
 * ready after endElement.
 */
public class XmlSourceElementFromEvents extends XmlSourceElement {

	public XmlSourceElementFromEvents() {
		super("(yetunkown)", null, "");
	}

}
