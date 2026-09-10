/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.config.plugin.GeoServerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultPropertyValuesResolverTest {

    Catalog catalog;
    DefaultPropertyValuesResolver resolver;
    CatalogTestData testData;

    @BeforeEach
    void setUp() {
        catalog = new CatalogPlugin();
        GeoServerImpl geoServer = new GeoServerImpl();
        geoServer.setCatalog(catalog);
        resolver = new DefaultPropertyValuesResolver(catalog);
        testData = CatalogTestData.initialized(() -> catalog, () -> geoServer).initCatalog();
    }

    /**
     * A layer group deserialized from a REST payload without a {@code <styles>} element has a {@code null} styles list,
     * since XStream bypasses field initializers. The resolver must treat it like an empty list and assign the default
     * style to every layer.
     */
    @Test
    void resolveLayerGroupWithNullStylesAssignsOneDefaultStylePerLayer() {
        LayerGroupInfoImpl layerGroup = layerGroupWithTwoLayers();
        layerGroup.setStyles(null);

        resolver.resolve((CatalogInfo) layerGroup);

        assertEquals(Arrays.asList(null, null), layerGroup.getStyles());
    }

    @Test
    void resolveLayerGroupWithEmptyStylesAssignsOneDefaultStylePerLayer() {
        LayerGroupInfoImpl layerGroup = layerGroupWithTwoLayers();
        layerGroup.getStyles().clear();

        resolver.resolve((CatalogInfo) layerGroup);

        assertEquals(Arrays.asList(null, null), layerGroup.getStyles());
    }

    @Test
    void resolveLayerGroupWithAssignedStylesLeavesThemUntouched() {
        LayerGroupInfoImpl layerGroup = layerGroupWithTwoLayers();
        List<StyleInfo> assigned = Arrays.asList(testData.style1, testData.style2);
        layerGroup.setStyles(assigned);

        resolver.resolve((CatalogInfo) layerGroup);

        assertEquals(assigned, layerGroup.getStyles());
    }

    private LayerGroupInfoImpl layerGroupWithTwoLayers() {
        LayerGroupInfoImpl layerGroup = new LayerGroupInfoImpl();
        layerGroup.setName("layerGroup");
        layerGroup.getLayers().add(testData.layerFeatureTypeA);
        layerGroup.getLayers().add(testData.layerFeatureTypeA);
        return layerGroup;
    }
}
