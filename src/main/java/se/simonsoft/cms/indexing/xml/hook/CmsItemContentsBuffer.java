package se.simonsoft.cms.indexing.xml.hook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Still unclear how we buffer item contents during hook/changeset processing,
 * for use in for example both xml and fulltext indexing.
 * 
 * Also unclear how buffers would be shared (additional getter in {@link CmsItemAndContents}?)
 * and who would be responsible for error handling.
 */
public class CmsItemContentsBuffer {

	private CmsItemAndContents item;

	private transient byte[] buffer = null;
	
	public CmsItemContentsBuffer(CmsItemAndContents item) {
		this.item = item;
	}
	
	public byte[] getBuffer() {
		if (buffer == null) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			item.getContents(out);
			buffer = out.toByteArray();
		}
		return buffer;
	}
	
	/**
	 * @return for use in xml parsing, Tika etc.
	 */
	public InputStream getContents() {
		return new ByteArrayInputStream(getBuffer());
	}
	
}
