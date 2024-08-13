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
package se.simonsoft.cms.indexing.properties;

import net.sf.saxon.s9api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.ItemContentBuffer;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.simonsoft.cms.indexing.abx.HandlerLogicalId;
import se.simonsoft.cms.indexing.xml.custom.ContentHandlerToIndexFields;
import se.simonsoft.cms.indexing.xml.custom.LoggingErrorListener;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.info.CmsItemNotFoundException;
import se.simonsoft.cms.item.properties.CmsItemProperties;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceDocumentS9api;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceElementS9api;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceReaderS9api;
import se.simonsoft.cms.xmlsource.transform.SaxonMessageListener;

import javax.inject.Inject;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.util.*;
import java.util.regex.Pattern;

public class HandlerSubjectScheme extends HandlerLogicalId {

    private final XmlSourceReaderS9api sourceReader;
    private ItemContentBufferStrategy contentStrategy;
    private transient XsltTransformer transformer;

    private static final Logger logger = LoggerFactory.getLogger(HandlerSubjectScheme.class);

    @Inject
    public HandlerSubjectScheme(XmlSourceReaderS9api sourceReader, Processor processor) {
        var stylesheet = new StreamSource(this.getClass().getClassLoader().getResourceAsStream("se/simonsoft/cms/indexing/properties/subject-scheme-props.xsl"));
        this.sourceReader = sourceReader;
        XsltCompiler compiler = processor.newXsltCompiler();
        XsltExecutable xsltCompiled;
        try {
            logger.info("Compiling subject scheme properties indexing XSL.");
            xsltCompiled = compiler.compile(stylesheet);
            logger.info("Compiled subject scheme properties indexing XSL.");
        } catch (SaxonApiException e) {
            throw new RuntimeException("Failed to compile subject scheme properties indexing XSL: " + e.getMessage(), e);
        }
        transformer = xsltCompiled.load();
    }

    @Inject
    public void setItemContentBufferStrategy(ItemContentBufferStrategy contentStrategy) {
        this.contentStrategy = contentStrategy;
    }

    @Override
    public void handle(IndexingItemProgress progress) {
        String excludePattern = "(cmsconfig.*:.*|svn:.*|abx:.*)";
        CmsItemProperties itemProperties = progress.getProperties();
        CmsItemId itemId = getItemId(progress);
        if (itemId == null) return;
        CmsItemPath itemRelPath = itemId.getRelPath();
        if (itemRelPath == null) return;
        CmsItemPath itemParentPath = itemRelPath.getParent();
        if (itemParentPath == null) return;
        XdmMap properties = new XdmMap();
        for (String key : itemProperties.getKeySet()) {
            String value = itemProperties.getString(key);
            if (!Pattern.compile(excludePattern).matcher(key).matches()) {
                properties = properties.put(new XdmAtomicValue(key), new XdmAtomicValue(value));
            }
        }
        if (properties.isEmpty()) return;
        indexItemProperties(progress, properties);
    }

    @Override
    protected CmsItemId getItemId(IndexingItemProgress progress) {
        CmsChangesetItem item = progress.getItem();
        // Ignore non-file items as later on contentStrategy.getBuffer(...) since its entry is missing a "size" attribute.
        if (!item.isFile()) return null;
        CmsRepository repo = progress.getRepository();
        CmsItemPath path = item.getPath();
        return repo.getItemId().withRelPath(path);
    }

    @Override
    public Set<Class<? extends IndexingItemHandler>> getDependencies() {
        return Set.of();
    }

    private void indexItemProperties(IndexingItemProgress progress, XdmMap properties) {
        XmlSourceDocumentS9api document = null;
        RepoRevision revision = progress.getRevision();
        String projectFolder = progress.getItem().getPath().getPathSegments().get(0);
        Destination destination = new SAXDestination(new ContentHandlerToIndexFields(progress.getFields()));
        LoggingErrorListener errorListener = new LoggingErrorListener();
        SaxonMessageListener messageListener = new SaxonMessageListener();
        try {
            CmsItemPath relPath = new CmsItemPath(String.format("/.cms/%s/properties.ditamap", projectFolder));
            ItemContentBuffer buffer = contentStrategy.getBuffer(revision, relPath, progress.getFields());
            document = sourceReader.read(buffer.getContents());
        } catch (CmsItemNotFoundException whatever) {
            try {
                CmsItemPath relPath = new CmsItemPath("/.cms/properties.ditamap");
                ItemContentBuffer buffer = contentStrategy.getBuffer(revision, relPath, progress.getFields());
                document = sourceReader.read(buffer.getContents());
            } catch (CmsItemNotFoundException nevermind) {}
        }
        if (document != null) {
            XmlSourceElementS9api documentElement = document.getDocumentElement();
            transformer.setParameter(new QName("properties"), properties);
            transformer.setInitialContextNode(documentElement.getElementXdm());
            transformer.setErrorListener(errorListener);
            transformer.setMessageListener(messageListener);
            transformer.setDestination(destination);
            try {
                transformer.transform();
            } catch (SaxonApiException e) {
                if (e.getCause() instanceof TransformerException) { // including net.sf.saxon.trans.XPathException
                    String msg = MessageFormatter.format("XML invalid for transformation at {}: {}", progress.getItem(), e.getMessage()).getMessage();
                    logger.error(msg);
                    // No longer throwing XmlNotWellFormedException since this situation can indicate XSL bug
                    throw new RuntimeException(msg, e);
                }
                throw new RuntimeException("Extraction aborted with error at " + progress.getItem(), e);
            }
        }
    }
}
