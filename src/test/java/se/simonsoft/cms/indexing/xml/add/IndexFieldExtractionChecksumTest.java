/**
 * Copyright (C) 2009-2012 Simonsoft Nordic AB
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
package se.simonsoft.cms.indexing.xml.add;

import static org.junit.Assert.*;

import org.junit.Test;

import se.simonsoft.cms.indexing.IndexFields;
import se.simonsoft.xmltracking.index.add.IndexFieldExtraction;
import se.simonsoft.xmltracking.index.add.IndexFieldExtractionChecksum;
import se.simonsoft.xmltracking.index.add.IndexFieldsSolrj;

public class IndexFieldExtractionChecksumTest {

	@Test
	public void testExtract() {
		IndexFields fields = new IndexFieldsSolrj();
		fields.addField("text", "some text");
		fields.addField("source", "<p id=\"x\">some text</p>");
		fields.addField("source_2", "<p>some text</p>");
		fields.addField("other", "ooooo");
		IndexFieldExtraction checksum = new IndexFieldExtractionChecksum("text", "source");
		checksum.extract(fields, null);
		assertEquals("552e21cd4cd9918678e3c1a0df491bc3", fields.getFieldValue("c_md5_text"));
		assertEquals("37aa63c77398d954473262e1a0057c1e632eda77", fields.getFieldValue("c_sha1_text"));
		assertEquals("352ce7ce1c6dc7d4e1cdb05ae7a49a96", fields.getFieldValue("c_md5_source"));
		assertEquals("919da29ed4554a5d7efe8499dfa4b1b117e2608d", fields.getFieldValue("c_sha1_source"));
		assertEquals("1b721070babbb45c49aef7586a0ef646", fields.getFieldValue("c_md5_source_2"));
		assertEquals("1930e31da552393b5febb8db2c6056f56dabbece", fields.getFieldValue("c_sha1_source_2"));
		assertEquals(null, fields.getFieldValue("c_md5_other"));
	}

}
