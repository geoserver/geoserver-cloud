/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.geoserver.catalog.WorkspaceInfo;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;

@AutoConfigureWebTestClient(timeout = "360000")
public class WorkspaceControllerTest extends AbstractCatalogInfoControllerTest<WorkspaceInfo> {

    public WorkspaceControllerTest() {
        super(WorkspaceController.BASE_URI, WorkspaceInfo.class);
    }

    protected @Override void assertPropertriesEqual(WorkspaceInfo expected, WorkspaceInfo actual) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.isIsolated(), actual.isIsolated());
    }

    public @Test void findWorkspaceById() {
        testFindById(testData.workspaceA);
        testFindById(testData.workspaceB);
        testFindById(testData.workspaceC);
    }

    public @Test void workspaceCRUD() {
        WorkspaceInfo ws = testData.createWorkspace("workspaceCRUD");
        crudTest(ws, w -> w.setName("modified_name"), catalog::getWorkspace);
    }

    public @Test void testFindAll() {
        List<WorkspaceInfo> all = super.findAll();
        assertEquals(3, all.size());
    }

    public @Test void testFindByName() {
        WorkspaceInfo ws1 = testData.workspaceA;
        assertEquals(ws1, client().getByName(ws1.getName()));
    }

    public @Test void getDefaultWorkspace() {
        WorkspaceInfo expected = catalog.getDefaultWorkspace();
        assertNotNull(expected);
        WorkspaceInfo actual =
                client().getRelative("/default")
                        .expectStatus()
                        .isOk()
                        .expectHeader()
                        .contentType(MediaType.APPLICATION_JSON)
                        .expectBody(WorkspaceInfo.class)
                        .returnResult()
                        .getResponseBody();
        assertEquals(expected, actual);
    }

    public @Test void getDefaultWorkspaceIsNullOnEmptyCatalog() {
        testData.deleteAll();
        assertNull(catalog.getDefaultWorkspace());

        client().getWithAbsolutePath("/default")
                .expectStatus()
                .isNotFound()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON);
    }

    public @Test void setDefaultWorkspace() {
        WorkspaceInfo current = catalog.getDefaultWorkspace();
        assertNotNull(current);
        assertEquals(testData.workspaceA.getId(), current.getId());

        WorkspaceInfo expected = catalog.getWorkspace(testData.workspaceB.getId());

        WorkspaceInfo actual =
                client().put("/default/{id}", expected.getId())
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
