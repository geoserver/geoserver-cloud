/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.server.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static java.lang.String.format;

import org.geoserver.catalog.WorkspaceInfo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;

@AutoConfigureWebTestClient(timeout = "360000")
class WorkspaceControllerTest extends AbstractReactiveCatalogControllerTest<WorkspaceInfo> {

    public WorkspaceControllerTest() {
        super(WorkspaceInfo.class);
    }

    protected @Override void assertPropertriesEqual(WorkspaceInfo expected, WorkspaceInfo actual) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.isIsolated(), actual.isIsolated());
    }

    @Override public  @Test void testFindById() {
        testFindById(testData.workspaceA);
        testFindById(testData.workspaceB);
        testFindById(testData.workspaceC);
    }

    @Override public  @Test void testFindAll() {
        super.testFindAll(testData.workspaceA, testData.workspaceB, testData.workspaceC);
    }

    @Override public  @Test void testFindAllByType() {
        super.testFindAll(
                WorkspaceInfo.class, testData.workspaceA, testData.workspaceB, testData.workspaceC);
    }

    @Test void testFindByName() {
        WorkspaceInfo ws1 = testData.workspaceA;
        assertEquals(ws1, client().getFirstByName(ws1.getName()));
    }

    @Override public  @Test void testQueryFilter() {
        WorkspaceInfo wsA = catalog.getWorkspace(testData.workspaceA.getId());
        WorkspaceInfo wsB = catalog.getWorkspace(testData.workspaceB.getId());
        WorkspaceInfo wsC = catalog.getWorkspace(testData.workspaceC.getId());

        wsB.setIsolated(true);
        wsC.setIsolated(true);
        catalog.save(wsB);
        catalog.save(wsC);

        super.testQueryFilter("isolated = true", wsB, wsC);
        super.testQueryFilter("isolated = false", wsA);
        super.testQueryFilter(format("\"id\" = '%s'", wsA.getId()), wsA);
    }

    @Test void testWorkspaceCRUD() {
        WorkspaceInfo ws = testData.faker().workspaceInfo("workspaceCRUD");
        crudTest(
                ws,
                catalog::getWorkspace,
                w -> {
                    w.setName("modified_name");
                    w.setIsolated(true);
                },
                (orig, updated) -> {
                    assertEquals("modified_name", updated.getName());
                    assertTrue(updated.isIsolated());
                });
    }

    @Test void testGetDefaultWorkspace() {
        WorkspaceInfo expected = catalog.getDefaultWorkspace();
        assertNotNull(expected);
        WorkspaceInfo actual =
                client().getRelative("/workspaces/default")
                        .expectStatus()
                        .isOk()
                        .expectHeader()
                        .contentType(MediaType.APPLICATION_JSON)
                        .expectBody(WorkspaceInfo.class)
                        .returnResult()
                        .getResponseBody();
        assertEquals(expected, actual);
    }

    @Test void testGetDefaultWorkspaceIsNullOnEmptyCatalog() {
        testData.deleteAll();
        assertNull(catalog.getDefaultWorkspace());

        client().getRelative("/workspaces/default")
                .expectStatus()
                .isNoContent()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON);
    }

    @Test void testSetDefaultWorkspace() {
        WorkspaceInfo current = catalog.getDefaultWorkspace();
        assertNotNull(current);
        assertEquals(testData.workspaceA.getId(), current.getId());

        WorkspaceInfo expected = catalog.getWorkspace(testData.workspaceB.getId());

        WorkspaceInfo actual =
                client().put("/workspaces/default/{id}", expected.getId())
                        .expectStatus()
                        .isOk()
                        .expectHeader()
                        .contentType(MediaType.APPLICATION_JSON)
                        .expectBody(WorkspaceInfo.class)
                        .returnResult()
                        .getResponseBody();
        assertEquals(expected, actual);
    }
}
