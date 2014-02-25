/**
 * Copyright (C) 2009-2013 Simonsoft Nordic AB
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
package se.simonsoft.cms.indexing.xml;

import static org.mockito.Mockito.*;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Test;

import se.repos.indexing.IndexAdmin;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.indexing.IdStrategy;

public class IndexAdminXmlTest {

	@Test
	public void testClear() throws SolrServerException, IOException {
		IndexAdmin central = mock(IndexAdmin.class);
		SolrServer reposxml = mock(SolrServer.class);
		CmsRepository repository = mock(CmsRepository.class);
		IdStrategy idStrategy = mock(IdStrategy.class);
		when(idStrategy.getIdRepository(repository)).thenReturn("the/re\"po/id");
		
		IndexAdminXml admin = new IndexAdminXml(repository, idStrategy, reposxml);
		admin.setIndexAdminCentral(central);
		verify(central).addPostAction(admin);
		
		admin.clear();
		verify(reposxml).deleteByQuery("repoid:\"the/re\\\"po/id\"");
		verify(reposxml).commit();
		verify(reposxml).optimize();
		verifyNoMoreInteractions(reposxml, central);
	}

}
