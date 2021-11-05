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
package se.simonsoft.cms.indexing.workflow;

import static org.junit.Assert.*;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;

public class WorkflowIndexingTest {
	
	ObjectMapper objectMapper = new ObjectMapper();
	ObjectReader readerMessage = objectMapper.reader().forType(WorkflowIndexingInput.class);

	String translationExportJson = "{\"status\":\"webhook failed\",\"complete\":true,\"error\":\"Notification failed, contact your TSP\",\"workflow\":\"translationexport\",\"itemid\":\"x-svn://demo-dev.simonsoftcms.se/svn/demo1?p=664\",\"executionid\":\"arn:aws:states:eu-west-1:518993259802:execution:cms-demo-dev-translationexport-v1:841138e5-02e6-4f85-8a1d-5c26c3d005b6\",\"options\":{\"project\":\"AFN0WB\",\"pdf\":\"NONE\",\"delivery\":{\"type\":\"webhook\",\"params\":{\"name\":\"Beeceptor integration\",\"urlXX\":\"https://translationexport.free.beeceptor.com\"},\"headers\":{}},\"progress\":{\"params\":{\"packageurl\":\"https://cms-translation-cheftest.s3.eu-west-1.amazonaws.com/cms4/demo-dev/untranslated/2021-02-25T151347Z-demo1-AFN0WB.coti.zip?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20211104T225647Z&X-Amz-SignedHeaders=host&X-Amz-Expires=604800&X-Amz-Credential=AKIAJF3MLHINBV7LX4KA%2F20211104%2Feu-west-1%2Fs3%2Faws4_request&X-Amz-Signature=6ee50cd19c5d8fc8ad15cd0d6ceaad866749be17f0216487d89effef5579c78b\"}},\"error\":{\"Error\":\"BadRequest\",\"Cause\":\"PublishJobDelivery param 'url' must not be null and must use protocol https.\"}},\"userid\":\"takesson\"}";
	
	@Test
	public void testTranslationExport() throws JsonMappingException, JsonProcessingException {
		IndexingDoc fields = new IndexingDocIncrementalSolrj();
		WorkflowIndexingInput input = readerMessage.readValue(translationExportJson);
		
		WorkflowExtractionTranslationExport extractionTranslationExport = new WorkflowExtractionTranslationExport();
		extractionTranslationExport.handle(input, fields);
		
		assertTrue(fields.containsKey("embd_translationexport_packageurl"));
		assertEquals("AFN0WB", fields.getFieldValue("embd_translationexport_project"));
	}

}
