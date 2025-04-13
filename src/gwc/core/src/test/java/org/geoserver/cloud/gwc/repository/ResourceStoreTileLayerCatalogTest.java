/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.gwc.layer.GWCGeoServerConfigurationProvider;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.GeoServerTileLayerInfoImpl;
import org.geoserver.gwc.layer.StyleParameterFilter;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.util.DimensionWarning.WarningType;
import org.geowebcache.config.XMLConfigurationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.context.WebApplicationContext;
import org.xmlunit.assertj.XmlAssert;
import org.xmlunit.builder.Input;

/**
 * @since 1.0
 */
class ResourceStoreTileLayerCatalogTest {

    private @TempDir File baseDirectory;

    private ResourceStore resourceLoader;

    private ResourceStoreTileLayerCatalog catalog;

    @BeforeEach
    void setUp() {
        resourceLoader = new GeoServerResourceLoader(baseDirectory);
        new File(baseDirectory, "gwc-layers").mkdir();

        WebApplicationContext context = mock(WebApplicationContext.class);

        Map<String, XMLConfigurationProvider> configProviders = Map.of("gs", new GWCGeoServerConfigurationProvider());

        when(context.getBeansOfType(XMLConfigurationProvider.class)).thenReturn(configProviders);
        when(context.getBean("gs")).thenReturn(configProviders.get("gs"));

        Optional<WebApplicationContext> webappCtx = Optional.of(context);
        catalog = new ResourceStoreTileLayerCatalog(resourceLoader, webappCtx);
        catalog.initialize();
    }

    private GeoServerTileLayerInfo add(String id, String name) {
        GeoServerTileLayerInfo info = new GeoServerTileLayerInfoImpl();
        info.setId(id);
        info.setName(name);
        catalog.save(info);
        return info;
    }

    @Test
    void testGetLayerIds() {
        assertThat(catalog.getLayerIds()).isEmpty();

        add("id1", "layer1");
        assertThat(catalog.getLayerIds()).containsExactly("id1");

        add("id2", "layer2");
        assertThat(catalog.getLayerIds()).containsExactlyInAnyOrder("id1", "id2");
    }

    @Test
    void testGetLayerId() {
        assertThat(catalog.getLayerId("layer1")).isNull();

        add("id1", "layer1");
        add("id2", "layer2");

        assertThat(catalog.getLayerId("layer1")).isEqualTo("id1");
        assertThat(catalog.getLayerId("layer2")).isEqualTo("id2");
    }

    @Test
    void testGetLayerById() {
        GeoServerTileLayerInfo info = add("id1", "name1");
        GeoServerTileLayerInfo actual = catalog.getLayerById("id1");
        actual = ModificationProxy.unwrap(actual);
        assertEquals(info, actual);
    }

    @Test
    void testGetLayerName() {
        assertThat(catalog.getLayerName("id1")).isNull();

        add("id1", "layer1");
        add("id2", "layer2");

        assertThat(catalog.getLayerName("id1")).isEqualTo("layer1");
        assertThat(catalog.getLayerName("id2")).isEqualTo("layer2");
    }

    @Test
    void testGetLayerNames() {
        assertThat(catalog.getLayerNames()).isEmpty();

        add("id1", "layer1");
        assertThat(catalog.getLayerNames()).containsExactly("layer1");

        add("id2", "layer2");
        assertThat(catalog.getLayerNames()).containsExactlyInAnyOrder("layer1", "layer2");
    }

    @Test
    void testGetLayerByName() {
        GeoServerTileLayerInfo info = add("id1", "name1");
        GeoServerTileLayerInfo actual = catalog.getLayerByName("name1");
        actual = ModificationProxy.unwrap(actual);
        assertEquals(info, actual);
    }

    @Test
    void testExists() {
        assertThat(catalog.exists("id1")).isFalse();

        add("id1", "layer1");
        add("id2", "layer2");

        assertThat(catalog.exists("id1")).isTrue();
        assertThat(catalog.exists("layer1")).isFalse();

        assertThat(catalog.exists("id2")).isTrue();
        assertThat(catalog.exists("layer2")).isFalse();
    }

    @Test
    void testDelete() {
        GeoServerTileLayerInfo info = add("id1", "name1");
        GeoServerTileLayerInfo actual = catalog.getLayerByName("name1");
        actual = ModificationProxy.unwrap(actual);
        assertEquals(info, actual);

        GeoServerTileLayerInfo deleted = catalog.delete("id1");
        assertEquals(info, ModificationProxy.unwrap(deleted));

        assertNull(catalog.getLayerById("id1"));
    }

    @Test
    void testSave() {
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
        assertEquals(Set.of("image/png", "image/jpeg"), oldValue.getMimeFormats());
        assertEquals("name1", oldValue.getName());

        assertNull(catalog.getLayerByName("name1"));
        assertNotNull(catalog.getLayerByName("name2"));

        GeoServerTileLayerInfo modified = catalog.getLayerById("id1");
        assertEquals(Set.of("image/gif"), modified.getMimeFormats());
    }

    @Test
    void testSaveWithEmptyStyleParamFilter() {
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
        assertEquals(Set.of("image/gif"), modified.getMimeFormats());
    }

    @Test
    void testEvents() {
        AtomicBoolean hasBeenCreated = new AtomicBoolean(false);
        AtomicBoolean hasBeenModified = new AtomicBoolean(false);
        AtomicBoolean hasBeenDeleted = new AtomicBoolean(false);

        catalog.addListener((layerId, type) -> {
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
    void testSavedXML() throws IOException {
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

        String actual =
                FileUtils.readFileToString(new File(baseDirectory, "gwc-layers/id1.xml"), StandardCharsets.UTF_8);

        String expected =
                """
                <GeoServerTileLayer>
                  <id>id1</id>
                  <enabled>false</enabled>
                  <name>name2</name>
                  <mimeFormats>
                    <string>image/gif</string>
                  </mimeFormats>
                  <gridSubsets/>
                  <metaWidthHeight>
                    <int>0</int>
                    <int>0</int>
                  </metaWidthHeight>
                  <expireCache>0</expireCache>
                  <expireClients>0</expireClients>
                  <parameterFilters>
                    <styleParameterFilter>
                      <key>STYLES</key>
                      <defaultValue></defaultValue>
                      <allowedStyles class="sorted-set"/>
                    </styleParameterFilter>
                  </parameterFilters>
                  <gutter>0</gutter>
                  <cacheWarningSkips>
                    <warning>Default</warning>
                    <warning>Nearest</warning>
                    <warning>FailedNearest</warning>
                  </cacheWarningSkips>
                </GeoServerTileLayer>
                """;

        XmlAssert.assertThat(Input.fromString(actual))
                .and(Input.fromString(expected))
                .areIdentical();
    }
}
