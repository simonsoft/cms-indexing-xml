package se.simonsoft.cms.indexing.history;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;

import se.simonsoft.cms.indexing.IndexingCore;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.events.change.CmsChangeset;

public class CmsIndexingHistoryImplIntegrationTest {

	@Test
	public void test() {
		CmsRepository repo = mock(CmsRepository.class);
		
		CmsIndexingHistory history = new CmsIndexingHistoryImpl(repo);

		CmsChangeset rev = mock(CmsChangeset.class);
		IndexingCore core = mock(IndexingCore.class);
		history.begin(rev, core);
	}

}
