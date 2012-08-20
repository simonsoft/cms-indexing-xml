package se.simonsoft.xmltracking.source;

/**
 * Receiver for XML tree walking with full source extraction,
 * useful both for indexing and reuse/replacement writing.
 */
public interface XmlSourceHandler {

	void startDocument();
	
	void endDocument();
	
	/**
	 * Depth first visiting, in the order elements appear
	 *  not the order in which they end.
	 *  
	 * Named begin, although argument contains full source, to indicate
	 * that the method is called in order of element starts and to allow
	 * for a future element end method if needed.
	 * 
	 * Caller should be fast enough so that impls can ignore elements
	 * and children without worring about lost performance. It would add
	 * quite a bit of complexity to signal to traversal in the case where
	 * reuse has been established and no further depth is needed.
	 * 
	 * @param element The full element that was encountered
	 */
	void begin(XmlSourceElement element);
	
}
