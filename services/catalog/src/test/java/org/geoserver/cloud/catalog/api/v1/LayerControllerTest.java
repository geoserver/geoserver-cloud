/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;

@AutoConfigureWebTestClient(timeout = "360000")
public class LayerControllerTest extends AbstractReactiveCatalogControllerTest<LayerInfo> {

    public LayerControllerTest() {
        super(LayerInfo.class);
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

    public @Override @Test void testFindAll() {
        assertTrue(super.findAll().isEmpty());
        LayerInfo layer1 = testData.layerFeatureTypeA;
        LayerInfo layer2 =
                testData.createLayer(
                        "cov-layer-id",
                        testData.coverageA,
                        "coverage layer",
                        true,
                        testData.style1);

        catalog.add(layer1);
        super.testFindAll(layer1);
        catalog.add(layer2);
        super.testFindAll(layer1, layer2);
    }

    public @Override @Test void testFindAllByType() {
        assertTrue(super.findAll().isEmpty());
        LayerInfo layer1 = testData.layerFeatureTypeA;
        LayerInfo layer2 =
                testData.createLayer(
                        "cov-layer-id",
                        testData.coverageA,
                        "coverage layer",
                        true,
                        testData.style1);

        catalog.add(layer1);
        super.testFindAll(LayerInfo.class, layer1);
        catalog.add(layer2);
        super.testFindAll(LayerInfo.class, layer1, layer2);
    }

    public @Override @Test void testFindById() {
        catalog.add(testData.layerFeatureTypeA);
        super.testFindById(testData.layerFeatureTypeA);
    }

    public @Override @Test void testQueryFilter() {
        catalog.add(testData.layerFeatureTypeA);
        StyleInfo style1 = testData.style1;
        StyleInfo style2 = testData.style2;
        StyleInfo style3 = testData.createStyle("style3");
        StyleInfo style4 = testData.createStyle("style4");
        catalog.add(style3);
        catalog.add(style4);

        LayerInfo layer1 = catalog.getLayer(testData.layerFeatureTypeA.getId());
        layer1.getStyles().add(style4);
        layer1.getStyles().add(style2);
        catalog.save(layer1);
        LayerInfo layer2 =
                testData.createLayer(
                        "cov-layer-id",
                        testData.coverageA,
                        "coverage layer",
                        true,
                        style2,
                        style1,
                        style3,
                        style4);
        catalog.add(layer1);
        catalog.add(layer2);

        String cql = String.format("\"defaultStyle.name\" = '%s'", style1.getName());
        super.testQueryFilter(cql, layer1);

        cql = String.format("\"styles.name\" = '%s'", style4.getName());
        super.testQueryFilter(cql, layer1, layer2);
    }

    public @Test void testLayerCRUD() {
        LayerInfo layer = testData.layerFeatureTypeA;
        crudTest(
                layer,
                catalog::getLayer,
                l -> {
                    l.setDefaultStyle(testData.style2);
                },
                (orig, updated) -> {
                    assertEquals(testData.style2, updated.getDefaultStyle());
                });
    }

    public @Test void testUpdateStyles() {
        LayerInfo layer = testData.layerFeatureTypeA;
        catalog.add(layer);

        testUpdate(
                layer,
                l -> {
                    l.getStyles().clear();
                    l.getStyles().add(testData.style1);
                    l.getStyles().add(testData.style2);
                },
                (orig, updated) -> {
                    assertEquals(2, updated.getStyles().size());
                    Set<StyleInfo> expected = Sets.newHashSet(testData.style2, testData.style1);
                    assertEquals(expected, updated.getStyles());
                });
    }

    public @Test void testFindLayersByResource() {
        LayerInfo layer = testData.layerFeatureTypeA;
        catalog.add(layer);

        String resourceId = layer.getResource().getId();
        client().getRelative("/layers/resource/{id}", resourceId)
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_STREAM_JSON)
                .expectBodyList(LayerInfo.class)
                .consumeWith(res -> assertEquals(1, res.getResponseBody().size()));
    }

    public @Test void testFindLayersByResource_NonExistentResourceId() {
        client().getRelative("/layers/resource/{id}", "bad-resource-id")
                .expectStatus()
                .isNotFound();
    }

    public @Test void testFindLayersWithStyle() {
        StyleInfo style1 = testData.style1; // on layer1 and layer2
        StyleInfo style2 = testData.style2; // layer2's default style
        StyleInfo style3 = testData.createStyle("style3"); // on layer2
        StyleInfo style4 = testData.createStyle("styleWithNoLayerAssociated"); // on no layer
        catalog.add(style3);
        catalog.add(style4);

        LayerInfo layer1 = testData.layerFeatureTypeA;
        LayerInfo layer2 =
                testData.createLayer(
                        "cov-layer-id",
                        testData.coverageA,
                        "coverage layer",
                        true,
                        style2,
                        style1,
                        style3);
        catalog.add(layer1);
        catalog.add(layer2);

        testFindLayersWithStyle(style1, layer1, layer2);
        testFindLayersWithStyle(style2, layer2);
        testFindLayersWithStyle(style3, layer2);
        testFindLayersWithStyle(style4);
    }

    private void testFindLayersWithStyle(StyleInfo style, LayerInfo... expectedLayers) {

        client().getRelative("/layers/style/{styleId}", style.getId())
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_STREAM_JSON)
                .expectBodyList(LayerInfo.class)
                .consumeWith(
                        response -> {
                            List<LayerInfo> responseBody = response.getResponseBody();
                            assertEquals(
                                    Arrays.stream(expectedLayers)
                                            .map(LayerInfo::getId)
                                            .collect(Collectors.toSet()),
                                    responseBody
                                            .stream()
                                            .map(LayerInfo::getId)
                                            .collect(Collectors.toSet()));
                        });
    }
}
