/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.api;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionFinder;

import java.util.List;
import java.util.Map;

/**
 * Main publication Service.
 *
 * @author tiry
 */
public interface PublisherService {

    String DOMAIN_NAME_KEY = "DomainName";

    List<String> getAvailablePublicationTree();

    /**
     * Returns a {@code Map} with tree name as key and tree title as value.
     */
    Map<String, String> getAvailablePublicationTrees();

    PublicationTree getPublicationTree(String treeName, CoreSession coreSession, Map<String, String> params)
            throws PublicationTreeNotAvailable;

    PublicationTree getPublicationTree(String treeName, CoreSession coreSession, Map<String, String> params,
            DocumentModel currentDocument) throws PublicationTreeNotAvailable;

    PublishedDocument publish(DocumentModel doc, PublicationNode targetNode);

    PublishedDocument publish(DocumentModel doc, PublicationNode targetNode, Map<String, String> params);

    void unpublish(DocumentModel doc, PublicationNode targetNode);

    boolean isPublishedDocument(DocumentModel documentModel);

    PublicationTree getPublicationTreeFor(DocumentModel doc, CoreSession coreSession);

    PublicationNode wrapToPublicationNode(DocumentModel documentModel, CoreSession coreSession) throws
            PublicationTreeNotAvailable;

    Map<String, String> getParametersFor(String treeConfigName);

    void releaseAllTrees(String sessionId);

    RootSectionFinder getRootSectionFinder(CoreSession session);

}
