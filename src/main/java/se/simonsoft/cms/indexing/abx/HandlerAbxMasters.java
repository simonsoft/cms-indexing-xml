/**
 * Copyright (C) 2009-2017 Simonsoft Nordic AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.simonsoft.cms.indexing.abx;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.backend.svnkit.info.change.CmsChangesetReaderSvnkit;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.HandlerProperties;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.inspection.CmsChangesetReader;

/**
 * Uses the abx.*Master properties, splitting on newline, to add fields rel_abx.*Master.
 */
public class HandlerAbxMasters extends HandlerAbxFolders {

	private static final Logger logger = LoggerFactory.getLogger(HandlerAbxMasters.class);

	private CmsChangesetReaderSvnkit changesetReader;

	private static final String HOSTFIELD = "repohost";
	
	/**
	 * @param idStrategy to fill the refid/relid field
	 */
	@Inject
	public HandlerAbxMasters(IdStrategy idStrategy) {
		super(idStrategy);
	}

	@Inject
	public void setCmsChangesetReader(CmsChangesetReader changesetReader) {
		this.changesetReader = (CmsChangesetReaderSvnkit) changesetReader;
	}

	@Override
	public void handle(IndexingItemProgress progress) {
		
		logger.trace("handle(IndexItemProgress progress)");
		
		IndexingDoc fields = progress.getFields();
		String host = (String) fields.getFieldValue(HOSTFIELD);
		if (host == null) {
			throw new IllegalStateException("Depending on indexer that adds host field " + HOSTFIELD);
		}
		
		Set<CmsItemId> masterIds = new HashSet<CmsItemId>();
		String[] abxProperties = {"abx.ReleaseMaster", "abx.AuthorMaster", "abx.TranslationMaster"};
		for (String propertyName : abxProperties) {
			masterIds.addAll(handleAbxProperty(fields, host, propertyName));
		}
		
		for (CmsItemId masterId : masterIds) {
			fields.addField("rel_abx.Masters", 
					masterId.getPegRev() == null ? 
							idStrategy.getIdHead(masterId) : 
							idStrategy.getId(masterId, new RepoRevision(masterId.getPegRev(), null)));
		}
		
		handleFolders(fields, "rel_abx.Masters_pathparents", masterIds);

		handleCommitPrevious(progress);
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return new HashSet<Class<? extends IndexingItemHandler>>() {private static final long serialVersionUID = 1L;{
			add(HandlerPathinfo.class);
			add(HandlerProperties.class);
		}};
	}
	
	/**
	 * Helper method for extracting master ids and adding them to a reference
	 * field. Assumes that the provided property is found in a field with the
	 * prefix "prop_".
	 *
	 * @param fields
	 * @param host
	 * @param propertyName name of the property field to copy master ref from
	 * @return
	 */
	protected Set<CmsItemId> handleAbxProperty(IndexingDoc fields, String host, String propertyName) {

		String fieldName = "prop_" + propertyName;
		String abxprop = (String) fields.getFieldValue(fieldName);
		Set<CmsItemId> result = new HashSet<CmsItemId>();

		String relationRevId;
		if (abxprop != null) {
			
			if (abxprop.length() != 0) {
				
				String propValueNormalized = "";
				for (String d : abxprop.split("\n")) {
					CmsItemIdArg itemId = new CmsItemIdArg(d);
					itemId.setHostname(host);
					
					// #886 Normalize the itemids in the indexed property.
					// Simply by parsing the id with latest cms-item (3.x).
					propValueNormalized = propValueNormalized.concat(itemId.getLogicalId()).concat("\n");
					
					relationRevId = itemId.getPegRev() != null ?
							idStrategy.getId(itemId, new RepoRevision(itemId.getPegRev(), null)) :
							idStrategy.getIdHead(itemId);
					
					fields.addField("rel_" + propertyName, relationRevId);
					
					// #1922: Ensure we have a relation to a commit revision.
					if (itemId.getPegRev() != null) {
						// Get the commit revision upTo id.getPegRev().
						RepoRevision commitRev = changesetReader.getChangedRevision(itemId.getRelPath(), itemId.getPegRev());
						if (commitRev != null) {
							String commitRevId = idStrategy.getId(itemId, commitRev);
							fields.addField("rel_commit_" + propertyName, commitRevId);
						}
					}
					
					result.add(itemId);
				}
				// #886 Overwrite the property field with normalized itemId(s).
				fields.setField(fieldName, propValueNormalized.trim());
			} else {
				logger.debug("{} property exists but is empty", propertyName);
			}
		}

		return result;
	}

	/**
	 * Handles commit relation fields for item history tracking via graph traversal.
	 * - rel_commit_previous: For all modifications except ADD (<= current-1)
	 * - rel_commit_previous_move: For ADD operations that are moves
	 * - rel_commit_previous_copy: For ADD operations that are copies
	 *
	 * @param progress the indexing progress containing the current item
	 */
	private void handleCommitPrevious(IndexingItemProgress progress) {
		CmsChangesetItem item = progress.getItem();
		IndexingDoc fields = progress.getFields();
		CmsRepository repository = progress.getRepository();

		// Field rel_commit_previous: All modifications except ADD should use previous commit (<= current-1)
		if (!item.isAdd()) {
			CmsItemPath itemPath = item.getPath();
			RepoRevision revision = progress.getRevision();
			RepoRevision previousCommitRev = changesetReader.getChangedRevision(itemPath, revision.getNumber() - 1);
			if (previousCommitRev != null) {
				CmsItemId itemId = new CmsItemIdArg(repository, itemPath).withPegRev(previousCommitRev.getNumber());
				String previousCommitRevId = idStrategy.getId(itemId, previousCommitRev);
				fields.addField("rel_commit_previous", previousCommitRevId);
			}
		} else {
			// Handle ADD operations: check for move and copy
			if (item.isCopy()) {
				CmsItemPath copyFromPath = item.getCopyFromPath();
				RepoRevision copyFromRevision = item.getCopyFromRevision();

				if (copyFromPath != null && copyFromRevision != null) {
					// Get the commit revision for the copy source
					RepoRevision previousCommitRev = changesetReader.getChangedRevision(copyFromPath, copyFromRevision.getNumber());
					if (previousCommitRev != null) {
						CmsItemId itemId = new CmsItemIdArg(repository, copyFromPath).withPegRev(previousCommitRev.getNumber());
						String previousCommitRevId = idStrategy.getId(itemId, previousCommitRev);

						if (item.isMove()) {
							fields.addField("rel_commit_previous_move", previousCommitRevId);
						} else {
							fields.addField("rel_commit_previous_copy", previousCommitRevId);
						}
					}
				}
			}
		}
	}
}
