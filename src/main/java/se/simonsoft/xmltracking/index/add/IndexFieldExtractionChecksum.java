package se.simonsoft.xmltracking.index.add;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import se.simonsoft.cms.item.Checksum;
import se.simonsoft.cms.item.impl.ChecksumRead;
import se.simonsoft.xmltracking.source.XmlSourceElement;

/**
 * Creates checksums of selected fields and adds with checksum type prefixed field names.
 */
public class IndexFieldExtractionChecksum implements IndexFieldExtraction {

	private List<String> prefixes;

	public IndexFieldExtractionChecksum() {
		this("text", "source");
	}
	
	public IndexFieldExtractionChecksum(String... fieldprefixes) {
		this.prefixes = Arrays.asList(fieldprefixes);
	}

	protected ChecksumRead getChecksummer() {
		return new ChecksumRead();
	}
	
	@Override
	public void extract(IndexFields fields, XmlSourceElement processedElement) {
		Collection<String> orgFieldNames = new LinkedList<String>(fields.getFieldNames());
		for (String n : orgFieldNames) {
			for (String p : prefixes) {
				if (n.startsWith(p)) {
					Object v = fields.getFieldValue(n);
					if (v instanceof String) {
						Checksum c = getChecksum((String) v);
						fields.addField("c_md5_" + n, c.getMd5());
						fields.addField("c_sha1_" + n, c.getSha1());
					} else {
						throw new IllegalArgumentException("Only string fields can be checksummed, field " + n + " was " + v.getClass() + " " + v);
					}
				}
			}
		}
	}

	private Checksum getChecksum(String value) {
		ChecksumRead c = new ChecksumRead();
		try {
			c.add(new ByteArrayInputStream(value.getBytes()));
		} catch (IOException e) { // would be very odd when we use byte array
			throw new RuntimeException("Value read error");
		}
		return c;
	}

}