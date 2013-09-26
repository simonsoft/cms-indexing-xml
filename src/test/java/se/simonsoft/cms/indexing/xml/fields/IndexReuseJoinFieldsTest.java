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
package se.simonsoft.cms.indexing.xml.fields;

import static org.junit.Assert.*;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;

public class IndexReuseJoinFieldsTest {

	@Test
	public void testExtract() {
		IndexingDoc fields = new IndexingDocIncrementalSolrj();
		fields.addField("prop_abx.TranslationMaster", "x-svn:///svn/testaut1^/tms/release/R001/xml/Docs/My%20First%20Novel.xml?p=6");
		fields.addField("prop_abx.TranslationLocale", "nb-NO");
		fields.addField("reusevalue", "2");
		new IndexReuseJoinFields().extract(null, fields);
		assertEquals("should get release corresponding to pathfull field", "/svn/testaut1/tms/release/R001/xml/Docs/My First Novel.xml", fields.getFieldValue("reuserelease"));
		assertEquals("should use 1 for reusevalue in concat field", "1nb-NO", fields.getFieldValue("reusevaluelocale"));
	}

	@Test
	public void testTranslationOfTranslation() {
		IndexingDoc fields = new IndexingDocIncrementalSolrj();
		fields.addField("prop_abx.TranslationMaster", "x-svn:///svn/testaut1^/tms/release/R001/xml/Docs/My%20Second%20Novel.xml?p=6");
		fields.addField("prop_abx.TranslationMaster", "x-svn:///svn/testaut1^/tms/lang/en-US/release/R001/xml/Docs/My%20Second%20Novel.xml?p=10");
		new IndexReuseJoinFields().extract(null, fields);
		assertEquals("should get the first value of TranslationMaster multi-value", "/svn/testaut1/tms/release/R001/xml/Docs/My Second Novel.xml", fields.getFieldValue("reuserelease"));
	}	
	
}
