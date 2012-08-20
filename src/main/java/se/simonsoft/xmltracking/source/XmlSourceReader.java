package se.simonsoft.xmltracking.source;

import java.io.InputStream;

public interface XmlSourceReader {

	void read(InputStream xml, XmlSourceHandler handler);
	
}
