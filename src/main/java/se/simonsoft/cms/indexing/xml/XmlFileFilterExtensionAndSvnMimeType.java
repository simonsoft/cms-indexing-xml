package se.simonsoft.cms.indexing.xml;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;

/**
 * Identifies XML on file extension and svn:mime-type property.
 * An alternative would be to resort to trial-and-error
 * (which we need anyway because some XML is not valid)
 * or detected content type from fulltext/metadata extraction.
 */
public class XmlFileFilterExtensionAndSvnMimeType implements XmlFileFilter {

	private Set<String> extensionsToTry = new HashSet<String>(Arrays.asList("xml", "xhtml", "html"));

	@Override
	public boolean isXml(CmsChangesetItem c, IndexingDoc fields) {
		// TODO legacy behavior now, add check for svn prop
		return extensionsToTry.contains(c.getPath().getExtension());
	}
	
}
