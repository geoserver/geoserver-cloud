/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.GeoServerTileLayerInfoImpl;
import org.geoserver.gwc.layer.StyleParameterFilter;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.util.DimensionWarning.WarningType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** @since 1.0 */
class ResourceStoreTileLayerCatalogTest {

    private @TempDir File baseDirectory;

    private ResourceStore resourceLoader;

    private ResourceStoreTileLayerCatalog catalog;

    @BeforeEach
    void setUp() throws Exception {
        resourceLoader = new GeoServerResourceLoader(baseDirectory);
        new File(baseDirectory, "gwc-layers").mkdir();
        catalog = new ResourceStoreTileLayerCatalog(resourceLoader);
        catalog.initialize();
    }

    @Test
    public void testGetLayerById() {
        GeoServerTileLayerInfo info = new GeoServerTileLayerInfoImpl();
        info.setId("id1");
        info.setName("name1");
        catalog.save(info);
        GeoServerTileLayerInfo actual = catalog.getLayerById("id1");
        actual = ModificationProxy.unwrap(actual);
        assertEquals(info, actual);
    }

    @Test
    public void testGetLayerByName() {
        GeoServerTileLayerInfo info = new GeoServerTileLayerInfoImpl();
        info.setId("id1");
        info.setName("name1");
        catalog.save(info);
        GeoServerTileLayerInfo actual = catalog.getLayerByName("name1");
        actual = ModificationProxy.unwrap(actual);
        assertEquals(info, actual);
    }

    @Test
    public void testDelete() {
        GeoServerTileLayerInfo info = new GeoServerTileLayerInfoImpl();
        info.setId("id1");
        info.setName("name1");
        catalog.save(info);

        GeoServerTileLayerInfo actual = catalog.getLayerByName("name1");
        actual = ModificationProxy.unwrap(actual);
        assertEquals(info, actual);

        GeoServerTileLayerInfo deleted = catalog.delete("id1");
        assertEquals(info, ModificationProxy.unwrap(deleted));

        assertNull(catalog.getLayerById("id1"));
    }

    @Test
    public void testSave() {
        final GeoServerTileLayerInfo original;
        {
            final GeoServerTileLayerInfo info = new GeoServerTileLayerInfoImpl();
            info.setId("id1");
            info.setName("name1");
            info.getMimeFormats().add("image/png");
            info.getMimeFormats().add("image/jpeg");

            assertNull(catalog.save(info));

            original = catalog.getLayerById("id1");
            assertEquals(info.getMimeFormats(), original.getMimeFormats());
        }

        original.getMimeFormats().clear();
        original.getMimeFormats().add("image/gif");
        original.setName("name2");

        final GeoServerTileLayerInfo oldValue = catalog.save(original);

        assertNotNull(oldValue);
        assertEquals(ImmutableSet.of("image/png", "image/jpeg"), oldValue.getMimeFormats());
        assertEquals("name1", oldValue.getName());

        assertNull(catalog.getLayerByName("name1"));
        assertNotNull(catalog.getLayerByName("name2"));

        GeoServerTileLayerInfo modified = catalog.getLayerById("id1");
        assertEquals(ImmutableSet.of("image/gif"), modified.getMimeFormats());
    }

    @Test
    public void testSaveWithEmptyStyleParamFilter() {
        final GeoServerTileLayerInfo original;
        {
            final GeoServerTileLayerInfo info = new GeoServerTileLayerInfoImpl();
            info.setId("id1");
            info.setName("name1");
            info.getMimeFormats().add("image/png");
            info.getMimeFormats().add("image/jpeg");

            StyleParameterFilter parameterFilter = new StyleParameterFilter();
            parameterFilter.setStyles(Collections.emptySet());
            info.addParameterFilter(parameterFilter);

            assertNull(catalog.save(info));

            original = catalog.getLayerById("id1");
            assertEquals(info.getMimeFormats(), original.getMimeFormats());
        }

        original.getMimeFormats().clear();
        original.getMimeFormats().add("image/gif");
        original.setName("name2");

        final GeoServerTileLayerInfo oldValue = catalog.save(original);

        assertNotNull(oldValue);
        assertEquals(Set.of("image/png", "image/jpeg"), oldValue.getMimeFormats());
        assertEquals("name1", oldValue.getName());

        assertNull(catalog.getLayerByName("name1"));
        assertNotNull(catalog.getLayerByName("name2"));

        GeoServerTileLayerInfo modified = catalog.getLayerById("id1");
        assertEquals(ImmutableSet.of("image/gif"), modified.getMimeFormats());
    }

    @Test
    public void testEvents() throws IOException, InterruptedException {
        AtomicBoolean hasBeenCreated = new AtomicBoolean(false);
        AtomicBoolean hasBeenModified = new AtomicBoolean(false);
        AtomicBoolean hasBeenDeleted = new AtomicBoolean(false);

        catalog.addListener(
                (layerId, type) -> {
                    switch (type) {
                        case CREATE:
                            hasBeenCreated.set(true);
                            break;
                        case DELETE:
                            hasBeenDeleted.set(true);
                            break;
                        case MODIFY:
                            hasBeenModified.set(true);
                            break;
                        default:
                            break;
                    }
                });

        final GeoServerTileLayerInfo l = new GeoServerTileLayerInfoImpl();
        l.setId("l1");
        l.setName("layer-1");

        catalog.save(l);
        assertTrue(hasBeenCreated.get());

        final GeoServerTileLayerInfo afterAdd = catalog.getLayerById("l1");
        assertNotSame(l, afterAdd);
        assertEquals(l, afterAdd);

        afterAdd.setName("layer-1-modified");

        catalog.save(afterAdd);
        assertTrue(hasBeenModified.get());

        final GeoServerTileLayerInfo afterSave = catalog.getLayerById("l1");
        assertNotSame(afterAdd, afterSave);
        assertEquals(afterAdd, afterSave);

        GeoServerTileLayerInfo deleted = catalog.delete("l1");
        assertTrue(hasBeenDeleted.get());
        assertNotNull(deleted);
        assertEquals(afterSave, deleted);

        hasBeenDeleted.set(false);
        assertNull(catalog.delete("l1"));
        assertFalse(hasBeenDeleted.get());
    }

    @Test
    public void testSavedXML() throws Exception {
        // checking that the persistence looks as expected
        final GeoServerTileLayerInfo original;
        {
            final GeoServerTileLayerInfo info = new GeoServerTileLayerInfoImpl();
            info.setId("id1");
            info.setName("name1");
            info.getMimeFormats().add("image/png");
            info.getMimeFormats().add("image/jpeg");
            info.setCacheWarningSkips(new LinkedHashSet<>(Arrays.asList(WarningType.values())));

            StyleParameterFilter parameterFilter = new StyleParameterFilter();
            parameterFilter.setStyles(Collections.emptySet());
            info.addParameterFilter(parameterFilter);

            assertNull(catalog.save(info));

            original = catalog.getLayerById("id1");
            assertEquals(info.getMimeFormats(), original.getMimeFormats());

            original.getMimeFormats().clear();
            original.getMimeFormats().add("image/gif");
            original.setName("name2");
        }

        catalog.save(original);

        File file = new File(baseDirectory, "gwc-layers/id1.xml");
        String xml = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        // XPathEngine xpath = XMLUnit.newXpathEngine();
        // Document doc = XMLUnit.buildControlDocument(xml);
        // // no custom attribute for the class, we set a default
        // assertEquals("", xpath.evaluate("//cacheWarningSkips/class", doc));
        // assertEquals("Default", xpath.evaluate("//cacheWarningSkips/warning[1]", doc));
        // assertEquals("Nearest", xpath.evaluate("//cacheWarningSkips/warning[2]", doc));
        // assertEquals("FailedNearest", xpath.evaluate("//cacheWarningSkips/warning[3]", doc));
    }
}
