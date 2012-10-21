package se.simonsoft.cms.indexing;

/**
 * Semi fast post-processing of item index additions.
 * 
 * Should be rather fast because indexing queue will be shared with high priority
 * TODO is this a good idea? Should we have two background levels instead. How about item contents buffering?
 * 
 * For slow processing implement {@link IndexItemFollowerBackground}.
 */
public interface IndexItemFollower {

	
	
}
