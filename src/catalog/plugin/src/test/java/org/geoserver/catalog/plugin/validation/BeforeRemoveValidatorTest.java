/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.config.plugin.GeoServerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BeforeRemoveValidatorTest {

    Catalog catalog;
    GeoServerImpl geoServer;
    BeforeRemoveValidator validator;
    CatalogTestData testData;

    @BeforeEach
    void setUp() {
        catalog = new CatalogPlugin();
        geoServer = new GeoServerImpl();
        geoServer.setCatalog(catalog);
        validator = new BeforeRemoveValidator(catalog);
        testData = CatalogTestData.initialized(() -> catalog, () -> geoServer).initCatalog();
    }

    @Test
    void testVisitLayerInfo_layerBelongsToLayerGroup() {
        LayerInfo layer = testData.layerFeatureTypeA;
        LayerGroupInfo layerGroup = testData.layerGroup1;

        assertEquals(1, layerGroup.getLayers().size());
        assertEquals(layer.getId(), layerGroup.getLayers().get(0).getId());

        // Trying to remove the layer while it belongs to a layer group should throw an exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> validator.visit(layer));

        assertEquals(
                "Unable to delete layer %s referenced by layer group '%s'"
                        .formatted(layer.prefixedName(), layerGroup.prefixedName()),
                exception.getMessage());
    }

    @Test
    void testVisitLayerInfo_layerNotInAnyLayerGroup() {
        // Create a layer not belonging to any layer group
        FeatureTypeInfo newFeatureType = testData.createFeatureType(
                "ft2", testData.dataStoreA, testData.namespaceA, "ftName2", "ftAbstract2", "ftDescription2", true);
        catalog.add(newFeatureType);
        LayerInfo newLayer = testData.createLayer("layer2", newFeatureType, "New Layer", true, testData.style1);
        catalog.add(newLayer);

        assertDoesNotThrow(() -> validator.visit(newLayer));
    }

    @Test
    void testVisitLayerInfo_layerGroupWithOnlyNullElement() {
        // Create a layer not belonging to any layer group
        FeatureTypeInfo newFeatureType = testData.createFeatureType(
                "ft2", testData.dataStoreA, testData.namespaceA, "ftName2", "ftAbstract2", "ftDescription2", true);
        catalog.add(newFeatureType);
        LayerInfo newLayer = testData.createLayer("layer2", newFeatureType, "New Layer", true, testData.style1);
        catalog.add(newLayer);

        // Create a layer group with only one null element (PMTiles layer group case)
        // Note: We cannot use catalog.add() because it sanitizes and removes null elements
        LayerGroupInfo lgNull = testData.createLayerGroup("lg3", testData.workspaceA, "layerGroupOnlyNull", null, null);
        ((CatalogPlugin) catalog).getFacade().add(lgNull);

        assertDoesNotThrow(() -> validator.visit(newLayer));
    }

    @Test
    void testVisitLayerInfo_nestedLayerGroups() {
        LayerGroupInfo innerLayerGroup =
                testData.createLayerGroup("lg4", null, "innerGroup", testData.layerFeatureTypeA, testData.style1);
        catalog.add(innerLayerGroup);

        LayerGroupInfo outerLayerGroup = testData.createLayerGroup("lg5", null, "outerGroup", innerLayerGroup, null);
        catalog.add(outerLayerGroup);

        // Trying to remove the layer should fail because it's in the inner group
        LayerInfo layer = testData.layerFeatureTypeA;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> validator.visit(layer));

        // Should mention the first group that contains it (innerLayerGroup)
        assertEquals(
                "Unable to delete layer %s referenced by layer group '%s'"
                        .formatted(layer.prefixedName(), innerLayerGroup.prefixedName()),
                exception.getMessage());
    }
}
