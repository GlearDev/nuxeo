/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.tabs.RelationTabSubPage;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Document Relations test.
 *
 * @since 5.9.1
 */
public class ITDocumentRelationTest extends AbstractTest {

    private final static String WORKSPACE_TITLE = ITDocumentRelationTest.class.getSimpleName() + "_WorkspaceTitle_" + new Date().getTime();

    private final String FILE_NAME1 = "File1";

    private final String FILE_NAME2 = "File2";

    @Before
    public void setUp() throws UserNotConnectedException, IOException {
        DocumentBasePage documentBasePage = login();

        documentBasePage = documentBasePage.getNavigationSubPage().goToDocument("Workspaces");
        DocumentBasePage workspacePage = createWorkspace(documentBasePage, WORKSPACE_TITLE, null);

        // Create test File 1
        DocumentBasePage newFile = createFile(workspacePage, FILE_NAME1, null, false, null, null, null);

        workspacePage = newFile.getNavigationSubPage().goToDocument(WORKSPACE_TITLE);

        // Create test File 2
        newFile = createFile(workspacePage, FILE_NAME2, null, false, null, null, null);
        logout();
    }

    @After
    public void tearDown() throws UserNotConnectedException {
        DocumentBasePage documentBasePage = login();

        deleteWorkspace(documentBasePage, WORKSPACE_TITLE);

        logout();
    }

    /**
     * Create a relation between 2 documents and delete it.
     *
     * @throws UserNotConnectedException
     * @since 5.9.1
     */
    @Test
    public void testSimpleRelationBetweenTwoDocuments() throws UserNotConnectedException {
        DocumentBasePage documentBasePage = login();

        documentBasePage = documentBasePage.getContentTab().goToDocument("Workspaces").getContentTab().goToDocument(
                WORKSPACE_TITLE);

        documentBasePage = documentBasePage.getContentTab().goToDocument(FILE_NAME1);

        RelationTabSubPage relationTabSubPage = documentBasePage.getRelationTab();

        relationTabSubPage = relationTabSubPage.initRelationSetUp();

        relationTabSubPage = relationTabSubPage.setRelationWithDocument(FILE_NAME2,
                "http://purl.org/dc/terms/ConformsTo");

        List<WebElement> existingRelations = relationTabSubPage.getExistingRelations();
        assertNotNull(existingRelations);

        assertEquals(1, existingRelations.size());

        WebElement newRelation = existingRelations.get(0);

        assertEquals("Conforms to", newRelation.findElement(By.xpath("td[1]")).getText());

        assertNotNull(newRelation.findElement(By.linkText(FILE_NAME2)));

        relationTabSubPage = relationTabSubPage.deleteRelation(0);

        assertEquals(0, relationTabSubPage.getExistingRelations().size());

        logout();
    }

}
