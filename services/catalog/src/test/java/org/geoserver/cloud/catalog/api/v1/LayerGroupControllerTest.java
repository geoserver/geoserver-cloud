/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;

@AutoConfigureWebTestClient(timeout = "360000")
public class LayerGroupControllerTest extends AbstractCatalogInfoControllerTest<LayerGroupInfo> {

    public LayerGroupControllerTest() {
        super(LayerGroupController.BASE_URI, LayerGroupInfo.class);
    }

    protected @Override void assertPropertriesEqual(
            LayerGroupInfo expected, LayerGroupInfo actual) {
        assertEquals(expected.getWorkspace(), actual.getWorkspace());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.isAdvertised(), actual.isAdvertised());
        assertEquals(expected.getLayers(), actual.getLayers());
    }

    public @Test void layerGroupCRUD_NoWorkspace() {
        WorkspaceInfo workspace = null;
        LayerGroupInfo layerGroup =
                testData.createLayerGroup(
                        "layerGroupCRUD_NoWorkspace",
                        workspace,
                        "layerGroupCRUD_NoWorkspace_name",
                        testData.layer,
                        testData.style2);
        crudTest(
                layerGroup,
                lg -> {
                    lg.setEnabled(false);
                    lg.setTitle("changed title");
                    lg.setAbstract("new abstract");
                    lg.setAdvertised(false);
                },
                catalog::getLayerGroup);
    }

    public @Test void layerGroupCRUD_Workspace() {
        WorkspaceInfo workspace = testData.ws;
        LayerGroupInfo layerGroup =
                testData.createLayerGroup(
                        "layerGroupCRUD_NoWorkspace",
                        workspace,
                        "layerGroupCRUD_NoWorkspace_name",
                        testData.layer,
                        testData.style2);
        crudTest(
                layerGroup,
                lg -> {
                    lg.setEnabled(false);
                    lg.setTitle("changed title");
                    lg.setAbstract("new abstract");
                    lg.setAdvertised(false);
                },
                catalog::getLayerGroup);
    }
}
