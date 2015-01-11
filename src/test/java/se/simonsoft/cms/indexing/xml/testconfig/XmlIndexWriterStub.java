package se.simonsoft.cms.indexing.xml.testconfig;

import se.simonsoft.cms.indexing.xml.XmlIndexAddSession;
import se.simonsoft.cms.indexing.xml.XmlIndexWriter;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;

public class XmlIndexWriterStub implements XmlIndexWriter {

	public XmlIndexWriterStub() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public XmlIndexAddSession get() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deletePath(CmsRepository repository, CmsChangesetItem c) {
		// TODO Auto-generated method stub

	}

	@Override
	public void commit(boolean expungeDeletes) {
		// TODO Auto-generated method stub

	}

}
