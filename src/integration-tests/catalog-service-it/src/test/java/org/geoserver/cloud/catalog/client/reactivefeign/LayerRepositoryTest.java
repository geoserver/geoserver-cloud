/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Sets;

import lombok.Getter;
import lombok.experimental.Accessors;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import java.util.Set;

@EnableAutoConfiguration
@Accessors(fluent = true)
public class LayerRepositoryTest
        extends AbstractCatalogServiceClientRepositoryTest<LayerInfo, LayerRepository> {

    private @Autowired @Getter LayerRepository repository;

    private LayerInfo layerFTA;
    private LayerInfo layerCVA;
    private LayerInfo layerWMTSA;

    public LayerRepositoryTest() {
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
        serverCatalog.remove(testData.layerGroup1);
        serverCatalog.remove(testData.layerFeatureTypeA);
        assertTrue(serverCatalog.getLayers().isEmpty());

        layerFTA = testData.layerFeatureTypeA;
        layerCVA = testData.createLayer(testData.coverageA, testData.style2);
        layerWMTSA = testData.createLayer(testData.wmtsLayerA, testData.style1);
    }

    private void addLayers() {
        serverCatalog.add(layerFTA);
        serverCatalog.add(layerCVA);
        serverCatalog.add(layerWMTSA);
    }

    public @Override @Test void testFindAll() {
        assertEquals(0, repository.findAll().count());
        addLayers();
        super.testFindAll(layerFTA, layerWMTSA, layerCVA);
    }

    public @Override @Test void testFindAllByType() {
        testFindAllIncludeFilter(LayerInfo.class);
        addLayers();
        testFindAllIncludeFilter(LayerInfo.class, layerFTA, layerCVA, layerWMTSA);
    }

    public @Override @Test void testFindById() {
        serverCatalog.add(testData.layerFeatureTypeA);
        super.testFindById(testData.layerFeatureTypeA);
    }

    public @Override @Test void testQueryFilter() {
        serverCatalog.add(testData.layerFeatureTypeA);
        StyleInfo style1 = testData.style1;
        StyleInfo style2 = testData.style2;
        StyleInfo style3 = testData.createStyle("style3");
        StyleInfo style4 = testData.createStyle("style4");
        serverCatalog.add(style3);
        serverCatalog.add(style4);

        LayerInfo layer1 = serverCatalog.getLayer(testData.layerFeatureTypeA.getId());
        layer1.getStyles().add(style4);
        layer1.getStyles().add(style2);
        serverCatalog.save(layer1);
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
        serverCatalog.add(layer1);
        serverCatalog.add(layer2);

        String cql = String.format("\"defaultStyle.name\" = '%s'", style1.getName());
        super.testQueryFilter(cql, layer1);

        cql = String.format("\"styles.name\" = '%s'", style4.getName());
        super.testQueryFilter(cql, layer1, layer2);
    }

    public @Test void testLayerCRUD() {
        LayerInfo layer = testData.layerFeatureTypeA;
        crudTest(
                layer,
                serverCatalog::getLayer,
                l -> {
                    l.setDefaultStyle(testData.style2);
                },
                (orig, updated) -> {
                    assertEquals(testData.style2, updated.getDefaultStyle());
                });
    }

    public @Test void testUpdateStyles() {
        LayerInfo layer = testData.layerFeatureTypeA;
        serverCatalog.add(layer);
        layer = serverCatalog.getLayer(layer.getId());

        testUpdate(
                layer,
                l -> {
                    // AttributionInfoImpl attribution = new AttributionInfoImpl();
                    // attribution.setHref("http://test.com");
                    // attribution.setLogoHeight(20);
                    // attribution.setLogoWidth(10);
                    // l.setAttribution(attribution);
                    // l.setPath("/llll");
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
        addLayers();

        testFind(() -> repository.findAllByResource(layerFTA.getResource()), layerFTA);
        testFind(() -> repository.findAllByResource(layerCVA.getResource()), layerCVA);
        testFind(() -> repository.findAllByResource(layerWMTSA.getResource()), layerWMTSA);
    }

    public @Test void testFindLayersByResource_NonExistentResource() {
        FeatureTypeInfo missingResource = testData.createFeatureType("not-added-to-catalog");

        assertEquals(0, repository.findAllByResource(missingResource).count());
    }

    public @Test void testFindLayersWithStyle() {
        StyleInfo style1 = testData.style1; // on layer1 and layer2
        StyleInfo style2 = testData.style2; // layer2's default style
        StyleInfo style3 = testData.createStyle("style3"); // on layer2
        StyleInfo style4 = testData.createStyle("styleWithNoLayerAssociated"); // on no layer
        serverCatalog.add(style3);
        serverCatalog.add(style4);

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
        serverCatalog.add(layer1);
        serverCatalog.add(layer2);

        testFindLayersWithStyle(style1, layer1, layer2);
        testFindLayersWithStyle(style2, layer2);
        testFindLayersWithStyle(style3, layer2);
        testFindLayersWithStyle(style4);
    }

    private void testFindLayersWithStyle(StyleInfo style, LayerInfo... expectedLayers) {

        testFind(() -> repository.findAllByDefaultStyleOrStyles(style), expectedLayers);
    }

    public @Test void testFindOneByName() {
        addLayers();
        assertEquals(layerFTA.getId(), repository.findOneByName(layerFTA.getName()).get().getId());
    }
}
