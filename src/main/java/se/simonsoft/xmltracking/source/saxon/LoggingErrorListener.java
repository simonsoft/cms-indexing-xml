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
