package se.simonsoft.xmltracking.source.saxon;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingErrorListener implements ErrorListener {

	public static final Logger logger = LoggerFactory.getLogger(LoggingErrorListener.class);
	
	@Override
	public void error(TransformerException ex) throws TransformerException {
		logger.error("Extraction {}", ex.getMessage(), ex);
	}

	@Override
	public void fatalError(TransformerException ex) throws TransformerException {
		logger.error("Extraction fatal {}", ex.getMessage(), ex);		
	}

	@Override
	public void warning(TransformerException ex) throws TransformerException {
		logger.warn("Extraction {}", ex.getMessage(), ex);
	}

}
