/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.geoserver.gwc.layer.GWCGeoServerConfigurationProvider;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.GeoServerTileLayerInfoImpl;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.gwc.layer.TileLayerCatalogListener;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.ResourceStore;
import org.geowebcache.config.XMLConfigurationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.util.Map;
import java.util.Optional;

class CachingTileLayerCatalogTest {

    private CachingTileLayerCatalog caching;
    private ResourceStoreTileLayerCatalog catalog;

    private @TempDir File baseDirectory;
    private ResourceStore resourceLoader;

    private CacheManager cacheManager;

    @BeforeEach
    void beforeEach() {
        resourceLoader = new GeoServerResourceLoader(baseDirectory);
        new File(baseDirectory, "gwc-layers").mkdir();

        WebApplicationContext context = mock(WebApplicationContext.class);

        Map<String, XMLConfigurationProvider> configProviders =
                Map.of("gs", new GWCGeoServerConfigurationProvider());

        when(context.getBeansOfType(XMLConfigurationProvider.class)).thenReturn(configProviders);
        when(context.getBean("gs")).thenReturn(configProviders.get("gs"));

        Optional<WebApplicationContext> webappCtx = Optional.of(context);
        catalog = new ResourceStoreTileLayerCatalog(resourceLoader, webappCtx);
        catalog.initialize();

        cacheManager = new CaffeineCacheManager();
        caching = new CachingTileLayerCatalog(cacheManager, catalog);
        caching.initialize();
    }

    @Test
    public void initialize() {
        caching = new CachingTileLayerCatalog(cacheManager, catalog);
        assertThat(caching.idCache).isNull();
        assertThat(caching.namesById).isNotNull().isEmpty();

        add(catalog, "tl1");
        add(catalog, "tl2");

        caching.initialize();
        assertThat(caching.idCache).isNotNull();
        assertThat(caching.namesById).isNotNull().hasSize(2);
    }

    @Test
    public void reset() {

        assertThat(caching.idCache).isNotNull();
        add(caching, "tl1");
        add(caching, "tl2");
        assertThat(caching.namesById).isNotNull().hasSize(2);

        caching.reset();
        assertThat(caching.idCache).isNull();
        assertThat(caching.namesById).isNotNull().isEmpty();
    }

    @Test
    public void onTileLayerEvent() {
        final String origName = "origName";
        final String newName = "newName";
        catalog.addListener(
                new TileLayerCatalogListener() {
                    @Override
                    public void onEvent(String layerId, Type type) {
                        TileLayerEvent event =
                                switch (type) {
                                    case CREATE -> TileLayerEvent.created(this, layerId, origName);
                                    case MODIFY -> TileLayerEvent.modified(
                                            this, layerId, newName, origName);
                                    case DELETE -> TileLayerEvent.deleted(this, layerId, newName);
                                    default -> throw new IllegalStateException();
                                };
                        caching.onTileLayerEvent(event);
                    }
                });

        // do crud ops bypassing the caching decorator, expect the events have the desired effect
        assertThat(caching.idCache.get("tl1")).isNull();
        var tl1 = add(catalog, "tl1", origName);
        assertThat(caching.namesById.get("tl1"))
                .as("create event should have added the id->name mapping")
                .isNotNull()
                .isEqualTo(origName);
        assertThat(caching.idCache.get("tl1")).as("create event doesn't cache a value").isNull();

        // force caching the entry
        caching.getLayerById("tl1");
        assertThat(caching.idCache.get("tl1")).isNotNull();

        tl1.setName(newName);
        catalog.save(tl1);
        assertThat(caching.namesById.get("tl1"))
                .isNotNull()
                .as("update event should have updated the id->name mapping")
                .isEqualTo(newName);

        assertThat(caching.idCache.get("tl1")).as("update event should have evicted").isNull();

        assertThat(caching.getLayerById("tl1"))
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", newName);

        catalog.delete(tl1.getId());
        assertThat(caching.idCache.get("tl1")).as("delete event should have evicted").isNull();
        assertThat(caching.namesById.get("tl1")).isNull();
    }

    @Test
    public void getLayerIds() {
        add(catalog, "tl1");
        add(catalog, "tl2");
        assertThat(caching.getLayerIds()).isEmpty();
        caching.initialize();
        assertThat(caching.getLayerIds()).isEqualTo(catalog.getLayerIds());
        add(caching, "tl3");
        assertThat(caching.getLayerIds()).isEqualTo(catalog.getLayerIds());
    }

    @Test
    public void getLayerNames() {
        add(catalog, "tl1");
        add(catalog, "tl2");
        assertThat(caching.getLayerNames()).isEmpty();
        caching.initialize();
        assertThat(caching.getLayerNames()).isEqualTo(catalog.getLayerNames());
        add(caching, "tl3");
        assertThat(caching.getLayerNames()).isEqualTo(catalog.getLayerNames());
    }

    @Test
    public void save() {
        add(caching, "tl1");
        var tl = add(caching, "tl2");

        tl.setEnabled(false);
        tl.setMetaTilingX(9);

        caching.save(tl);

        assertThat(catalog.getLayerById(tl.getId())).isNotSameAs(tl).isEqualTo(tl);
        assertThat(caching.idCache.get(tl.getId(), GeoServerTileLayerInfo.class)).isEqualTo(tl);

        tl.setName("newname");
        caching.save(tl);

        assertThat(caching.namesById.get(tl.getId())).isEqualTo("newname");
        assertThat(caching.idCache.get(tl.getId(), GeoServerTileLayerInfo.class)).isEqualTo(tl);
        assertThat(catalog.getLayerById(tl.getId())).isNotSameAs(tl).isEqualTo(tl);
    }

    @Test
    public void delete() {
        add(caching, "tl1");
        add(caching, "tl2");

        assertThat(caching.idCache.get("tl1", GeoServerTileLayerInfo.class)).isNotNull();
        assertThat(caching.idCache.get("tl2", GeoServerTileLayerInfo.class)).isNotNull();

        caching.delete("tl2");
        assertThat(caching.idCache.get("tl2", GeoServerTileLayerInfo.class)).isNull();
        assertThat(catalog.getLayerById("tl2")).isNull();
        assertThat(caching.getLayerById("tl2")).isNull();

        assertThat(caching.idCache.get("tl1", GeoServerTileLayerInfo.class)).isNotNull();
        assertThat(caching.getLayerById("tl1")).isNotNull();
    }

    @Test
    public void getLayerId() {
        add(caching, "tl1", "name1");
        add(caching, "tl2", "name2");

        assertThat(caching.getLayerId("name1")).isEqualTo("tl1");
        assertThat(caching.getLayerId("name2")).isEqualTo("tl2");
    }

    @Test
    public void getLayerName() {
        // bypass cache
        add(catalog, "tl1", "name1");
        add(catalog, "tl2", "name2");

        assertThat(caching.getLayerName("tl1")).isNotNull().isEqualTo("name1");
        assertThat(caching.getLayerName("tl2")).isNotNull().isEqualTo("name2");
    }

    @Test
    public void getLayerById() {
        add(catalog, "tl1", "name1");
        add(catalog, "tl2", "name2");

        assertThat(caching.getLayerById("tl1"))
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "name1");
        assertThat(caching.getLayerById("tl3")).isNull();
    }

    @Test
    public void getLayerByName() {
        // bypass cache
        add(catalog, "tl1", "name1");
        add(catalog, "tl2", "name2");

        assertThat(caching.getLayerByName("name1"))
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", "tl1");

        // simulate a cache evicted, id to name mapping exists but layer is not cached
        caching.namesById.put("tl2", "name2");
        assertThat(caching.getLayerByName("name2"))
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", "tl2");

        assertThat(caching.getLayerByName("name3")).isNull();
    }

    private GeoServerTileLayerInfo add(TileLayerCatalog target, String name) {
        return add(target, name, name);
    }

    private GeoServerTileLayerInfo add(TileLayerCatalog target, String id, String name) {
        GeoServerTileLayerInfo info = create(id, name);
        target.save(info);
        return info;
    }

    private GeoServerTileLayerInfo create(String id, String name) {
        GeoServerTileLayerInfo info = new GeoServerTileLayerInfoImpl();
        info.setId(id);
        info.setName(name);
        return info;
    }
}
