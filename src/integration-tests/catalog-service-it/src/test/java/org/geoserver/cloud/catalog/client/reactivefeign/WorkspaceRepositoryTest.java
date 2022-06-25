/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import lombok.Getter;
import lombok.experimental.Accessors;

@EnableAutoConfiguration
@Accessors(fluent = true)
public class WorkspaceRepositoryTest
        extends AbstractCatalogServiceClientRepositoryTest<WorkspaceInfo, WorkspaceRepository> {

    private @Autowired @Getter WorkspaceRepository repository;

    public WorkspaceRepositoryTest() {
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
        super.testFindAllIncludeFilter(
                WorkspaceInfo.class, testData.workspaceA, testData.workspaceB, testData.workspaceC);
    }

    public @Override @Test void testQueryFilter() {
        WorkspaceInfo wsA = serverCatalog.getWorkspace(testData.workspaceA.getId());
        WorkspaceInfo wsB = serverCatalog.getWorkspace(testData.workspaceB.getId());
        WorkspaceInfo wsC = serverCatalog.getWorkspace(testData.workspaceC.getId());

        wsB.setIsolated(true);
        wsC.setIsolated(true);
        serverCatalog.save(wsB);
        serverCatalog.save(wsC);

        super.testQueryFilter("isolated = true", wsB, wsC);
        super.testQueryFilter("isolated = false", wsA);
        super.testQueryFilter(format("\"id\" = '%s'", wsA.getId()), wsA);
    }

    public @Test void testFindByName() {
        WorkspaceInfo ws1 = testData.workspaceA;
        assertEquals(ws1, repository.findFirstByName(ws1.getName(), infoType).get());
    }

    public @Test void testWorkspaceCRUD() {
        WorkspaceInfo ws = testData.faker().workspaceInfo("workspaceCRUD");
        crudTest(
                ws,
                serverCatalog::getWorkspace,
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
        WorkspaceInfo expected = serverCatalog.getDefaultWorkspace();
        assertNotNull(expected);
        WorkspaceInfo actual = repository.getDefaultWorkspace().get();
        assertEquals(expected, actual);
    }

    public @Test void testGetDefaultWorkspaceIsNullOnEmptyCatalog() {
        testData.deleteAll();
        assertNull(serverCatalog.getDefaultWorkspace());
        assertFalse(repository.getDefaultWorkspace().isPresent());
    }

    public @Test void testSetDefaultWorkspace() {
        WorkspaceInfo current = serverCatalog.getDefaultWorkspace();
        assertNotNull(current);
        assertEquals(testData.workspaceA.getId(), current.getId());
        assertEquals(testData.workspaceA, repository.getDefaultWorkspace().get());

        WorkspaceInfo expected = testData.workspaceB;

        repository.setDefaultWorkspace(expected);
        assertEquals(expected, serverCatalog.getDefaultWorkspace());
        assertEquals(expected, repository.getDefaultWorkspace().get());
    }

    public @Test void testUnsetDefaultWorkspace() {
        WorkspaceInfo ws2 = testData.workspaceB;
        // preflight check
        serverCatalog.setDefaultWorkspace(null);
        assertNull(serverCatalog.getDefaultWorkspace());
        serverCatalog.setDefaultWorkspace(ws2);
        assertEquals(ws2, serverCatalog.getDefaultWorkspace());

        WorkspaceInfo current = serverCatalog.getDefaultWorkspace();
        assertNotNull(current);
        assertEquals(ws2.getId(), current.getId());

        repository.unsetDefaultWorkspace();

        assertNull(serverCatalog.getDefaultWorkspace());
        assertTrue(repository.getDefaultWorkspace().isEmpty());

        // check idempotency
        repository.unsetDefaultWorkspace();

        assertNull(serverCatalog.getDefaultWorkspace());
        assertTrue(repository.getDefaultWorkspace().isEmpty());
    }
}
