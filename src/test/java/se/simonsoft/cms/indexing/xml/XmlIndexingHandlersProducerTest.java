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
package se.simonsoft.cms.indexing.xml;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.Test;

import se.repos.indexing.IndexingHandlers;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.repos.indexing.item.ItemPropertiesBufferStrategy;
import se.simonsoft.cms.indexing.abx.HandlerAbxBaseLogicalId;
import se.simonsoft.cms.indexing.abx.HandlerAbxDependencies;
import se.simonsoft.cms.indexing.abx.HandlerAbxMasters;
import se.simonsoft.cms.indexing.abx.HandlerLogicalIdFromUrl;
import se.simonsoft.cms.indexing.abx.HandlerPathareaFromProperties;
import se.simonsoft.cms.indexing.abx.HandlerReleaseLabel;
import se.simonsoft.cms.indexing.abx.HandlerXmlMasters;
import se.simonsoft.cms.indexing.abx.HandlerXmlReferences;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.cms.item.inspection.CmsChangesetReader;

public class XmlIndexingHandlersProducerTest {

	@Test
	public void testProducedHandlerOrderMatchesLegacyConfiguration() {
		XmlIndexingHandlersProducer producer = new XmlIndexingHandlersProducer();
		HandlerXml handlerXml = mock(HandlerXml.class);

		Set<IndexingItemHandler> handlers = producer.createIndexingItemHandlers(
				mock(IdStrategy.class),
				mock(SolrClient.class),
				mock(SolrClient.class),
				mock(CmsRepository.class),
				mock(CmsChangesetReader.class),
				mock(ItemContentBufferStrategy.class),
				mock(ItemPropertiesBufferStrategy.class),
				handlerXml);

		assertEquals(legacyHandlerTypes(), handlerTypes(handlers, handlerXml));
	}

	private List<Class<? extends IndexingItemHandler>> legacyHandlerTypes() {
		List<Class<? extends IndexingItemHandler>> result = new ArrayList<>();
		add(result, IndexingHandlers.STANDARD.get(IndexingHandlers.Group.Unblock));
		add(result, IndexingHandlers.STANDARD.get(IndexingHandlers.Group.Structure));
		add(result, IndexingHandlers.STANDARD.get(IndexingHandlers.Group.Fast));
		result.add(HandlerLogicalIdFromUrl.class);
		result.add(HandlerAbxBaseLogicalId.class);
		result.add(HandlerAbxDependencies.class);
		result.add(HandlerAbxMasters.class);
		result.add(HandlerReleaseLabel.class);
		result.add(HandlerPathareaFromProperties.class);
		add(result, IndexingHandlers.STANDARD.get(IndexingHandlers.Group.Nice));
		result.add(HandlerXml.class);
		result.add(HandlerXmlReferences.class);
		result.add(HandlerXmlMasters.class);
		add(result, IndexingHandlers.STANDARD.get(IndexingHandlers.Group.Content));
		add(result, IndexingHandlers.STANDARD.get(IndexingHandlers.Group.Final));
		result.add(MarkerXmlCommit.class);
		return result;
	}

	private void add(List<Class<? extends IndexingItemHandler>> target,
			Iterable<Class<? extends IndexingItemHandler>> source) {
		for (Class<? extends IndexingItemHandler> handlerType : source) {
			target.add(handlerType);
		}
	}

	private List<Class<? extends IndexingItemHandler>> handlerTypes(
			Set<IndexingItemHandler> handlers, HandlerXml handlerXml) {
		List<Class<? extends IndexingItemHandler>> result = new ArrayList<>();
		for (IndexingItemHandler handler : handlers) {
			result.add(handler == handlerXml ? HandlerXml.class : handler.getClass());
		}
		return result;
	}
}
