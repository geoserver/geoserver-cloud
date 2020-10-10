/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.geoserver.catalog.WorkspaceInfo;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;

@AutoConfigureWebTestClient(timeout = "360000")
public class WorkspaceControllerTest extends AbstractReactiveCatalogControllerTest<WorkspaceInfo> {

    public WorkspaceControllerTest() {
        super(WorkspaceInfo.class);
    }

    protected @Override void assertPropertriesEqual(WorkspaceInfo expected, WorkspaceInfo actual) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.isIsolated(), actual.isIsolated());
    }

    public @Override @Test void testFindById() {
        testFindById(testData.workspaceA);
        testFindById(testData.workspaceB);
        testFindById(testData.workspaceC);
    }

    public @Override @Test void testFindAll() {
        super.testFindAll(testData.workspaceA, testData.workspaceB, testData.workspaceC);
    }

    public @Override @Test void testFindAllByType() {
        super.testFindAll(
                WorkspaceInfo.class, testData.workspaceA, testData.workspaceB, testData.workspaceC);
    }

    public @Test void testFindByName() {
        WorkspaceInfo ws1 = testData.workspaceA;
        assertEquals(ws1, client().getFirstByName(ws1.getName()));
    }

    public @Override @Test void testQueryFilter() {
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

    public @Test void testWorkspaceCRUD() {
        WorkspaceInfo ws = testData.createWorkspace("workspaceCRUD");
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

    public @Test void testGetDefaultWorkspace() {
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

    public @Test void testGetDefaultWorkspaceIsNullOnEmptyCatalog() {
        testData.deleteAll();
        assertNull(catalog.getDefaultWorkspace());

        client().getRelative("/workspaces/default")
                .expectStatus()
                .isNoContent()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON);
    }

    public @Test void testSetDefaultWorkspace() {
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
