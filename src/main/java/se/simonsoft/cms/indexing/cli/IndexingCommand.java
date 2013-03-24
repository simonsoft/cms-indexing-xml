package se.simonsoft.cms.indexing.cli;

import se.simonsoft.cms.item.RepoRevision;

public class IndexingCommand {

	enum Operation { sync, resync, clear };
	
	private String repositoryPath; // detect type or get as parameter?
	
	private RepoRevision revision;
	
	private String repoName; // in index
	
	private String repositoryUrl; // for indexing URLs
	
}
