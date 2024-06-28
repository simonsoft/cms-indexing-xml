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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.ItemContentBuffer;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.simonsoft.cms.indexing.abx.HandlerLogicalId;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.info.CmsItemNotFoundException;
import se.simonsoft.cms.item.properties.CmsItemProperties;
import se.simonsoft.cms.xmlsource.handler.XmlSourceReader;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceDocumentS9api;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceElementS9api;
import se.simonsoft.cms.xmlsource.handler.s9api.XmlSourceReaderS9api;
import se.simonsoft.cms.xmlsource.transform.TransformOptions;
import se.simonsoft.cms.xmlsource.transform.TransformerService;
import se.simonsoft.cms.xmlsource.transform.TransformerServiceFactory;

import javax.inject.Inject;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Pattern;

public class HandlerSubjectScheme extends HandlerLogicalId {

    private final XmlSourceReaderS9api sourceReader;
    private ItemContentBufferStrategy contentStrategy;
    private final TransformerServiceFactory transformerServiceFactory;
    private final TransformerService transformerServiceSubjectScheme;

    private static final Logger logger = LoggerFactory.getLogger(HandlerSubjectScheme.class);

    @Inject
    public HandlerSubjectScheme(XmlSourceReader sourceReader, TransformerServiceFactory transformerServiceFactory) {
        this.sourceReader = (XmlSourceReaderS9api) sourceReader;
        this.transformerServiceFactory = transformerServiceFactory;
        var stylesheet = new StreamSource(this.getClass().getClassLoader().getResourceAsStream("se/simonsoft/cms/indexing/properties/subject-scheme-props.xsl"));
        this.transformerServiceSubjectScheme = transformerServiceFactory.buildTransformerService(stylesheet);
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
        logger.debug("---------------------------------------------");
        logger.debug("Path: {}", itemRelPath.toString());
        logger.debug("Parent: {}", itemParentPath.toString());
        HashMap<String, String> properties = new HashMap<>(itemProperties.getKeySet().size());
        itemProperties.getKeySet().forEach(key -> {
            String value = itemProperties.getString(key);
            if (!Pattern.compile(excludePattern).matcher(key).matches()) {
                logger.debug("{}: {}", key, value);
                properties.put(key, value);
            }
        });
        if (properties.isEmpty()) return;
        String metadata = getSubjectSchemeMapMetadata(progress, properties);
        logger.debug("Result: {}", metadata);
        logger.debug("---------------------------------------------");
    }

    @Override
    protected CmsItemId getItemId(IndexingItemProgress progress) {
        CmsChangesetItem item = progress.getItem();
        if (!item.isFile()) return null;
        CmsRepository repo = progress.getRepository();
        CmsItemPath path = item.getPath();
        return repo.getItemId().withRelPath(path);
    }

    @Override
    public Set<Class<? extends IndexingItemHandler>> getDependencies() {
        return Set.of();
    }

    private String getSubjectSchemeMapMetadata(IndexingItemProgress progress, HashMap<String, String> properties) {
        XmlSourceDocumentS9api document = null;
        RepoRevision revision = progress.getRevision();
        String projectFolder = progress.getItem().getPath().getPathSegments().get(0);
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
            StringWriter resultWriter = new StringWriter();
            TransformOptions options = new TransformOptions();
            options.setParameter("properties", properties);
            String source = documentElement.getSourceAsString();
            transformerServiceSubjectScheme.transform(new StringReader(source), resultWriter, options);
            return resultWriter.toString();
        }
        return null;
    }
}
