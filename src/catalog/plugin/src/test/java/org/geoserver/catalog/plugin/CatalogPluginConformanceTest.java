/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.jupiter.api.Test;

class CatalogPluginConformanceTest extends CatalogConformanceTest {

    protected @Override CatalogPlugin createCatalog(File tmpFolder) {
        CatalogPlugin catalog = new CatalogPlugin();
        catalog.setResourceLoader(new GeoServerResourceLoader(tmpFolder));
        catalog.addListener(new CatalogPluginStyleResourcePersister(catalog));
        return catalog;
    }

    @Test
    void getPublishedFindsBothTypes() {
        LayerGroupInfo group = addLayerGroup();
        String layerId = data.layerFeatureTypeA.getId();
        CatalogPlugin plugin = (CatalogPlugin) catalog;

        PublishedInfo layer = plugin.getPublished(layerId, PublishedInfo.class);
        PublishedInfo published = plugin.getPublished(group.getId(), PublishedInfo.class);
        LayerGroupInfo typed = plugin.getPublished(group.getId(), LayerGroupInfo.class);

        assertEquals(layerId, layer.getId());
        assertEquals(group.getId(), published.getId());
        assertEquals(group.getId(), typed.getId());
    }

    @Test
    void getPublishedOfOtherTypeIsNull() {
        LayerGroupInfo group = addLayerGroup();
        CatalogPlugin plugin = (CatalogPlugin) catalog;

        assertNull(plugin.getPublished(group.getId(), LayerInfo.class));
        assertNull(plugin.getPublished("missing", PublishedInfo.class));
    }

    @Test
    void findAllByIdSkipsMissingIds() {
        LayerGroupInfo group = addLayerGroup();
        String layerId = data.layerFeatureTypeA.getId();
        List<String> ids = List.of(layerId, group.getId(), "missing");
        CatalogPlugin plugin = (CatalogPlugin) catalog;

        try (Stream<PublishedInfo> found = plugin.findAllById(ids, PublishedInfo.class)) {
            List<String> foundIds = found.map(PublishedInfo::getId).toList();
            assertEquals(2, foundIds.size());
            assertEquals(Set.of(layerId, group.getId()), Set.copyOf(foundIds));
        }
    }
}
