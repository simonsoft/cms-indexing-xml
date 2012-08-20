package se.simonsoft.xmltracking.index.svnadmin;

import java.io.File;

import se.simonsoft.xmltracking.index.add.IdStrategy;
import se.simonsoft.xmltracking.source.XmlSourceElement;

/**
 * Sets IDs based on repo name and path
 * (for offline indexing that does not know the URLs).
 * 
 * Intentionally without a revision or timestamp, so that
 * documents are overwritten at next indexing.
 * 
 * TODO needs notification of path from file traversal
 * before a new document's first element.
 * Verify at {@link #start()} that such a path has just been provided.
 */
public class IdStrategySvnRepoHead implements IdStrategy {

	private String prefix;

	public IdStrategySvnRepoHead(File repositoryPath) {
		this(repositoryPath.getName());
	}
	
	public IdStrategySvnRepoHead(String name) {
		this.prefix = name + "^";
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getElementId(XmlSourceElement element) {
		// TODO Auto-generated method stub
		return null;
	}

}
