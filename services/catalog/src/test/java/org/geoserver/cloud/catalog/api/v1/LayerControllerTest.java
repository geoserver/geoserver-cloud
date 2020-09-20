/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.geoserver.catalog.LayerInfo;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;

@Ignore
@AutoConfigureWebTestClient(timeout = "360000")
public class LayerControllerTest extends AbstractCatalogInfoControllerTest<LayerInfo> {

    public LayerControllerTest() {
        super(LayerController.BASE_URI, LayerInfo.class);
    }

    protected @Override void assertPropertriesEqual(LayerInfo expected, LayerInfo actual) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getResource().getId(), actual.getResource().getId());
        assertEquals(expected.getDefaultStyle(), actual.getDefaultStyle());
        assertEquals(expected.getStyles(), actual.getStyles());
    }

    public @Before void removeExisitng() {
        // can't create the layer with testData.ft as its resource otherwise, it's a 1:1
        // relationship
        catalog.remove(testData.layerGroup1);
        catalog.remove(testData.layerFeatureTypeA);
    }

    public @Test void layerCRUD() {
        LayerInfo layer =
                testData.createLayer(
                        "layerCRUD_id",
                        testData.featureTypeA,
                        "layerCRUD title",
                        true,
                        testData.style1,
                        testData.style2);
        crudTest(
                layer,
                l -> {
                    l.setTitle("changed title");
                    l.setDefaultStyle(testData.style2);
                    l.getStyles().clear();
                    l.getStyles().add(testData.style1);
                },
                catalog::getLayer);
    }
}
