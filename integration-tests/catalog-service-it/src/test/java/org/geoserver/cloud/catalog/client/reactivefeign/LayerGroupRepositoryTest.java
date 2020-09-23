/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@EnableAutoConfiguration
@Accessors(fluent = true)
public class LayerGroupRepositoryTest
        extends AbstractCatalogServiceClientRepositoryTest<LayerGroupInfo, LayerGroupRepository> {

    private @Autowired @Getter LayerGroupRepository repository;

    LayerGroupInfo lg1WorkspaceA;
    LayerGroupInfo lg2WorkspaceA;

    public LayerGroupRepositoryTest() {
        super(LayerGroupInfo.class);
    }

    public @Before void before() {
        lg1WorkspaceA =
                testData.createLayerGroup(
                        "lg1wsA-id",
                        testData.workspaceA,
                        "lg1wsA",
                        testData.layerFeatureTypeA,
                        testData.style2);
        lg2WorkspaceA =
                testData.createLayerGroup(
                        "lg2wsA-id",
                        testData.workspaceA,
                        "lg2wsA",
                        testData.layerFeatureTypeA,
                        (StyleInfo) null);
    }

    protected @Override void assertPropertriesEqual(
            LayerGroupInfo expected, LayerGroupInfo actual) {
        assertEquals(expected.getWorkspace(), actual.getWorkspace());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.isAdvertised(), actual.isAdvertised());
        assertEquals(expected.getLayers(), actual.getLayers());
    }

    public @Override @Test void testFindAll() {
        super.testFindAll(testData.layerGroup1);
        serverCatalog.add(lg1WorkspaceA);
        super.testFindAll(testData.layerGroup1, lg1WorkspaceA);
    }

    public @Override @Test void testFindById() {
        super.testFindById(testData.layerGroup1);
    }

    public @Override @Test void testFindAllByType() {
        testFindAll(testData.layerGroup1);
    }

    public @Override @Test void testQueryFilter() {
        LayerGroupInfo lg1 = testData.layerGroup1;
        serverCatalog.add(lg1WorkspaceA);

        super.testQueryFilter(LayerGroupInfo.class, Filter.INCLUDE, lg1, lg1WorkspaceA);
        super.testQueryFilter(LayerGroupInfo.class, Filter.EXCLUDE);

        String cql = String.format("\"workspace.name\" = '%s'", testData.workspaceA.getName());
        super.testQueryFilter(cql, lg1WorkspaceA);

        cql = "\"workspace.name\" IS NULL";
        super.testQueryFilter(cql, lg1);
    }

    public @Test void testLayerGroupCRUD_NoWorkspace() {
        WorkspaceInfo workspace = null;
        LayerGroupInfo layerGroup =
                testData.createLayerGroup(
                        "layerGroupCRUD_NoWorkspace",
                        workspace,
                        "layerGroupCRUD_NoWorkspace_name",
                        testData.layerFeatureTypeA,
                        testData.style2);
        crudTest(
                layerGroup,
                serverCatalog::getLayerGroup,
                lg -> {
                    lg.setEnabled(false);
                    lg.setTitle("changed title");
                    lg.setAbstract("new abstract");
                    lg.setAdvertised(false);
                },
                (orig, updated) -> {
                    assertFalse(updated.isEnabled());
                    assertFalse(updated.isAdvertised());
                    assertEquals("changed title", updated.getTitle());
                    assertEquals("new abstract", updated.getAbstract());
                    assertEquals(1, orig.getLayers().size());
                    assertEquals(1, updated.getLayers().size());
                });
    }

    public @Test void testLayerGroupCRUD_Workspace() {
        final WorkspaceInfo workspace = testData.workspaceA;
        LayerGroupInfo layerGroup =
                testData.createLayerGroup(
                        "layerGroupCRUD_Workspace",
                        workspace,
                        "layerGroupCRUD_Workspace_name",
                        testData.layerFeatureTypeA,
                        (StyleInfo) null);
        crudTest(
                layerGroup,
                serverCatalog::getLayerGroup,
                lg -> {
                    lg.getStyles().set(0, testData.style1);
                },
                (orig, updated) -> {
                    assertNull(orig.getStyles().get(0));
                    assertEquals(testData.style1, updated.getStyles().get(0));
                });
    }

    public @Test void testUpdateLayers() {
        WorkspaceInfo workspace = null;
        LayerGroupInfo layerGroup =
                testData.createLayerGroup(
                        "layerGroupCRUD_NoWorkspace",
                        workspace,
                        "layerGroupCRUD_NoWorkspace_name",
                        testData.layerFeatureTypeA,
                        testData.style2);

        LayerInfo layer2 = testData.createLayer(testData.coverageA, testData.style1);
        serverCatalog.add(layer2);

        crudTest(
                layerGroup,
                serverCatalog::getLayerGroup,
                lg -> {
                    lg.setEnabled(false);
                    lg.setTitle("changed title");
                    lg.setAbstract("new abstract");
                    lg.setAdvertised(false);
                    lg.getLayers().add(layer2);
                    lg.getStyles().add(testData.style1);
                },
                (orig, updated) -> {
                    assertFalse(updated.isEnabled());
                    assertFalse(updated.isAdvertised());
                    assertEquals("changed title", updated.getTitle());
                    assertEquals("new abstract", updated.getAbstract());
                    assertEquals(1, orig.getLayers().size());
                    assertEquals(2, updated.getLayers().size());
                    assertEquals(1, orig.getStyles().size());
                    assertEquals(2, updated.getStyles().size());
                });
    }

    public @Test void testFindAllByWorkspaceIsNull() {
        testFind(() -> repository.findAllByWorkspaceIsNull(), testData.layerGroup1);
        serverCatalog.add(lg1WorkspaceA);
        testFind(() -> repository.findAllByWorkspaceIsNull(), testData.layerGroup1);
        LayerGroupInfo lg2 =
                testData.createLayerGroup(
                        "lg2", null, "lg2", testData.layerFeatureTypeA, (StyleInfo) null);
        serverCatalog.add(lg2);
        testFind(() -> repository.findAllByWorkspaceIsNull(), testData.layerGroup1, lg2);
    }

    public @Test void testFindAllByWorkspace() {
        WorkspaceInfo workspace = testData.workspaceA;
        testFind(() -> repository.findAllByWorkspace(testData.workspaceA));
        serverCatalog.add(lg1WorkspaceA);
        testFind(() -> repository.findAllByWorkspace(workspace), lg1WorkspaceA);
        LayerGroupInfo lg2WorkspaceA =
                testData.createLayerGroup(
                        "lg2", workspace, "lg2", testData.layerFeatureTypeA, (StyleInfo) null);
        serverCatalog.add(lg2WorkspaceA);
        testFind(() -> repository.findAllByWorkspace(workspace), lg1WorkspaceA, lg2WorkspaceA);
    }

    public @Test void testFindByNameAndWorkspaceIsNull() {
        LayerGroupInfo global = testData.layerGroup1;
        assertEquals(
                global.getId(),
                repository.findByNameAndWorkspaceIsNull(global.getName()).get().getId());
        serverCatalog.add(lg1WorkspaceA);
        assertTrue(repository.findByNameAndWorkspaceIsNull(lg1WorkspaceA.getName()).isEmpty());
    }

    public @Test void testFindByNameAndWorkspace() {
        LayerGroupInfo globalGroup = serverCatalog.getLayerGroup(testData.layerGroup1.getId());
        WorkspaceInfo workspace = testData.workspaceA;
        serverCatalog.add(lg1WorkspaceA);
        serverCatalog.add(lg2WorkspaceA);

        assertTrue(repository.findByNameAndWorkspace(globalGroup.getName(), workspace).isEmpty());

        assertEquals(
                lg1WorkspaceA.getId(),
                repository
                        .findByNameAndWorkspace(lg1WorkspaceA.getName(), workspace)
                        .get()
                        .getId());
        assertEquals(
                lg2WorkspaceA.getId(),
                repository
                        .findByNameAndWorkspace(lg2WorkspaceA.getName(), workspace)
                        .get()
                        .getId());

        // workspace and root groups with the same name
        globalGroup.setName(lg1WorkspaceA.getName());
        serverCatalog.save(globalGroup);
        assertEquals(
                lg1WorkspaceA.getId(),
                repository.findByNameAndWorkspace(globalGroup.getName(), workspace).get().getId());
    }
}
