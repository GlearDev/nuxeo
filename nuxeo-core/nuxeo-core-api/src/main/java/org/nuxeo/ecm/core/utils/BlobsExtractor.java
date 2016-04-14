/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;

/**
 * Extractor for all the blobs of a document.
 *
 * @author Florent Guillaume
 * @author Benjamin Jalon
 */
public class BlobsExtractor {

    protected static final Log log = LogFactory.getLog(BlobsExtractor.class);

    protected final Map<String, Map<String, List<String>>> blobFieldPaths = new HashMap<String, Map<String, List<String>>>();

    protected List<String> docTypeCached = new ArrayList<String>();

    protected SchemaManager schemaManager;

    private Set<String> pathProperties;

    private Set<String> excludedPathProperties;

    private boolean indexAllBinary = false;

    private boolean isDefaultConfiguration = true;

    protected SchemaManager getSchemaManager() {
        if (schemaManager == null) {
            schemaManager = Framework.getService(SchemaManager.class);
        }
        return schemaManager;
    }

    /**
     * Get properties of the given document that contain a blob value. This method uses the cache engine to find these
     * properties.
     */
    public List<Property> getBlobsProperties(DocumentModel doc) {

        List<Property> result = new ArrayList<Property>();
        for (String schema : getBlobFieldPathForDocumentType(doc.getType()).keySet()) {
            List<String> pathsList = getBlobFieldPathForDocumentType(doc.getType()).get(schema);
            for (String path : pathsList) {
                if (!isInterestingBlobProperty(path)) {
                    continue;
                }
                List<String> pathSplitted = Arrays.asList(path.split("/[*]/"));
                if (pathSplitted.size() == 0) {
                    throw new IllegalStateException("Path detected not wellformed: " + pathsList);
                }
                Property prop = doc.getProperty(pathSplitted.get(0));

                if (pathSplitted.size() >= 1) {
                    List<String> subPath = pathSplitted.subList(1, pathSplitted.size());
                    getBlobValue(prop, subPath, path, result);
                }
            }
        }

        return result;
    }

    /**
     * Get path list of properties that may contain a blob for the given document type.
     *
     * @param documentType document type name
     * @return return the property names that contain blob
     */
    public Map<String, List<String>> getBlobFieldPathForDocumentType(String documentType) {
        DocumentType docType = getSchemaManager().getDocumentType(documentType);

        if (!docTypeCached.contains(documentType)) {
            Map<String, List<String>> paths = new HashMap<String, List<String>>();
            blobFieldPaths.put(docType.getName(), paths);

            createCacheForDocumentType(docType);
        }

        return blobFieldPaths.get(documentType);
    }

    public void invalidateDocumentTypeCache(String docType) {
        if (docTypeCached.contains(docType)) {
            docTypeCached.remove(docType);
        }
    }

    public void invalidateCache() {
        docTypeCached = new ArrayList<String>();
    }

    protected void createCacheForDocumentType(DocumentType docType) {

        for (Schema schema : docType.getSchemas()) {
            findInteresting(docType, schema, "", schema);
        }

        if (!docTypeCached.contains(docType.getName())) {
            docTypeCached.add(docType.getName());
        }
    }

    /**
     * Analyzes the document's schemas to find which fields and complex types contain blobs. For each blob fields type
     * found, {@link BlobsExtractor#blobMatched(DocumentType, Schema, String, Field)} is called and for each property
     * that contains a subProperty containing a Blob,
     * {@link BlobsExtractor#containsBlob(DocumentType, Schema, String, Field)} is called
     *
     * @param schema The parent schema that contains the field
     * @param ct Current type parsed
     * @return {@code true} if the passed complex type contains at least one blob field
     */
    protected boolean findInteresting(DocumentType docType, Schema schema, String path, ComplexType ct) {
        boolean interesting = false;
        for (Field field : ct.getFields()) {
            Type type = field.getType();
            String blobMatchedPath = field.getName().getPrefixedName();
            if (path.isEmpty()) {
                // add schema name as prefix if the schema doesn't have a prefix
                if (!schema.getNamespace().hasPrefix()) {
                    blobMatchedPath = schema.getName() + ":" + blobMatchedPath;
                }
            } else {
                blobMatchedPath = path + "/" + blobMatchedPath;
            }
            if (type.isSimpleType()) {
                continue; // not binary text
            } else if (type.isListType()) {
                Type ftype = ((ListType) type).getField().getType();
                if (ftype.isComplexType()) {
                    blobMatchedPath += "/*";
                    if (findInteresting(docType, schema, blobMatchedPath, (ComplexType) ftype)) {
                        containsBlob(docType, schema, blobMatchedPath, field);
                        interesting |= true;
                    }
                } else {
                    continue; // not binary text
                }
            } else { // complex type
                ComplexType ctype = (ComplexType) type;
                if (TypeConstants.isContentType(type)) {
                    blobMatched(docType, schema, blobMatchedPath, field);
                    interesting = true;
                } else {
                    interesting |= findInteresting(docType, schema, blobMatchedPath, ctype);
                }
            }
        }
        if (interesting) {
            containsBlob(docType, schema, path, null);
        }
        return interesting;
    }

    /**
     * Call during the parsing of the schema structure in {@link BlobsExtractor#findInteresting} if field is a Blob
     * Type. This method stores the path to that Field.
     *
     * @param schema The parent schema that contains the field
     * @param field Field that is a BlobType
     */
    protected void blobMatched(DocumentType docType, Schema schema, String path, Field field) {
        Map<String, List<String>> blobPathsForDocType = blobFieldPaths.get(docType.getName());
        List<String> pathsList = blobPathsForDocType.get(schema.getName());
        if (pathsList == null) {
            pathsList = new ArrayList<String>();
            blobPathsForDocType.put(schema.getName(), pathsList);
            blobFieldPaths.put(docType.getName(), blobPathsForDocType);
        }
        pathsList.add(path);
    }

    /**
     * Called during the parsing of the schema structure in {@link BlobsExtractor#findInteresting} if field contains a
     * subfield of type Blob. This method does nothing.
     *
     * @param schema The parent schema that contains the field
     * @param field Field that contains a subField of type BlobType
     */
    protected void containsBlob(DocumentType docType, Schema schema, String path, Field field) {
    }

    protected void getBlobValue(Property prop, List<String> subPath, String completePath, List<Property> result) {
        if (subPath.size() == 0) {
            if (!(prop.getValue() instanceof Blob)) {
                log.debug("Path Field not contains a blob value: " + completePath);
                return;
            }
            result.add(prop);
            return;
        }

        for (Property childProp : prop.getChildren()) {
            if ("/*".equals(subPath.get(0))) {
                log.debug("TODO : BLOB IN A LIST NOT IMPLEMENTED for this path " + completePath);
            }
            Property childSubProp = childProp.get(subPath.get(0));
            getBlobValue(childSubProp, subPath.subList(1, subPath.size()), completePath, result);
        }
    }

    /**
     * Finds all the blobs of the document.
     * <p>
     * This method is not thread-safe.
     *
     * @param doc the document
     * @return the list of blobs in the document
     */
    public List<Blob> getBlobs(DocumentModel doc) {
        List<Blob> result = new ArrayList<Blob>();
        for (Property blobField : getBlobsProperties(doc)) {
            Blob blob = (Blob) blobField.getValue();
            result.add(blob);
        }
        return result;
    }

    /**
     * Sets extractor properties.
     * <p>
     * The properties have to be defined without prefix if there is no prefix in the schema definition. For blob
     * properties, the path must include the {@code /data} part.
     */
    public void setExtractorProperties(Set<String> pathProps, Set<String> excludedPathProps, boolean indexBlobs) {
        pathProperties = normalizePaths(pathProps);
        excludedPathProperties = normalizePaths(excludedPathProps);
        indexAllBinary = indexBlobs;
        isDefaultConfiguration = (pathProps == null && excludedPathProps == null && indexBlobs);
    }

    /**
     * Removes the "/data" suffix used by FulltextConfiguration.
     * <p>
     * Adds missing schema name as prefix if no prefix ("content" -> "file:content").
     */
    protected Set<String> normalizePaths(Set<String> paths) {
        if (paths == null) {
            return null;
        }
        Set<String> normPaths = new HashSet<>();
        SchemaManager schemaManager = getSchemaManager();
        for (String path : paths) {
            // remove "/data" suffix
            if (path.endsWith("/data")) {
                path = path.substring(0, path.length() - "/data".length());
            }
            // add schema if no schema prefix
            if (schemaManager.getField(path) == null && !path.contains(":")) {
                // check without prefix
                // TODO precompute this in SchemaManagerImpl
                int slash = path.indexOf('/');
                String first = slash == -1 ? path : path.substring(0, slash);
                for (Schema schema : schemaManager.getSchemas()) {
                    if (!schema.getNamespace().hasPrefix()) {
                        // schema without prefix, try it
                        if (schema.getField(first) != null) {
                            path = schema.getName() + ":" + path;
                            break;
                        }
                    }
                }
            }
            normPaths.add(path);
        }
        return normPaths;
    }

    private boolean isInterestingBlobProperty(String path) {
        if (isDefaultConfiguration) {
            return true;
        } else if (excludedPathProperties != null && excludedPathProperties.contains(path)) {
            return false;
        } else if (pathProperties != null && pathProperties.contains(path)) {
            return true;
        } else if (indexAllBinary) {
            return true;
        }
        return false;
    }
}
