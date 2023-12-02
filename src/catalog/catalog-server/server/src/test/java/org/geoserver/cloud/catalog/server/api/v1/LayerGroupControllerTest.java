/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.server.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.api.filter.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;

@AutoConfigureWebTestClient(timeout = "360000")
class LayerGroupControllerTest
        extends AbstractReactiveCatalogControllerTest<LayerGroupInfo> {

    public LayerGroupControllerTest() {
        super(LayerGroupInfo.class);
    }

    protected @Override void assertPropertriesEqual(
            LayerGroupInfo expected, LayerGroupInfo actual) {
        assertEquals(expected.getWorkspace(), actual.getWorkspace());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.isAdvertised(), actual.isAdvertised());
        assertEquals(expected.getLayers(), actual.getLayers());
    }

    @Override public  @Test void testFindAll() {
        super.testFindAll(testData.layerGroup1);
        LayerGroupInfo lg2 =
                testData.createLayerGroup(
                        "lg2",
                        testData.workspaceA,
                        "lg2",
                        testData.layerFeatureTypeA,
                        testData.style2);
        catalog.add(lg2);
        super.testFindAll(testData.layerGroup1, lg2);
    }

    @Override public  @Test void testFindById() {
        super.testFindById(testData.layerGroup1);
    }

    @Override public  @Test void testFindAllByType() {
        super.testFindAll(LayerGroupInfo.class, testData.layerGroup1);
    }

    @Override public  @Test void testQueryFilter() {
        LayerGroupInfo lg1 = testData.layerGroup1;
        LayerGroupInfo lg2 =
                testData.createLayerGroup(
                        "lg2",
                        testData.workspaceA,
                        "lg2",
                        testData.layerFeatureTypeA,
                        testData.style2);
        catalog.add(lg2);

        super.testQueryFilter(LayerGroupInfo.class, Filter.INCLUDE, lg1, lg2);
        super.testQueryFilter(LayerGroupInfo.class, Filter.EXCLUDE);

        String cql = String.format("\"workspace.name\" = '%s'", testData.workspaceA.getName());
        super.testQueryFilter(cql, lg2);

        cql = "\"workspace.name\" IS NULL";
        super.testQueryFilter(cql, lg1);
    }

    @Test void testLayerGroupCRUD_NoWorkspace() {
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
                catalog::getLayerGroup,
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

    @Test void testLayerGroupCRUD_Workspace() {
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
                catalog::getLayerGroup,
                lg -> {
                    lg.getStyles().set(0, testData.style1);
                },
                (orig, updated) -> {
                    assertNull(orig.getStyles().get(0));
                    assertEquals(testData.style1, updated.getStyles().get(0));
                });
    }

    @Test void testUpdateLayers() {
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
                catalog::getLayerGroup,
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
}
