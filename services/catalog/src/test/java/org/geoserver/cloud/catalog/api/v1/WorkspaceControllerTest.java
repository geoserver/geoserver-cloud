/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.geoserver.catalog.WorkspaceInfo;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;

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
}
