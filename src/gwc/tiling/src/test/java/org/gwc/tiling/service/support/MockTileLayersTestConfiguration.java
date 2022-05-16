/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.service.support;

import org.geowebcache.config.DefaultGridsets;
import org.geowebcache.config.TileLayerConfiguration;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.storage.BlobStore;
import org.geowebcache.storage.DefaultStorageBroker;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.StorageException;
import org.geowebcache.storage.TransientCache;
import org.geowebcache.storage.blobstore.file.FileBlobStore;
import org.gwc.tiling.model.TileLayerMockSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
public class MockTileLayersTestConfiguration {

    public @Bean TileLayerConfiguration mockLayersConfig(TileLayerMockSupport mockLayers) {
        return new MockTileLayersConfiguration(mockLayers);
    }

    public @Bean TileLayerDispatcher tileLayerDispatcher(List<TileLayerConfiguration> configs) {
        GridSetBroker gb = new GridSetBroker(List.of(new DefaultGridsets(false, false)));
        return new TileLayerDispatcher(gb, configs);
    }

    public @Bean StorageBroker defaultStorageBroker() throws Exception {
        return new DefaultStorageBroker(testBlobStore(), testTransientCache());
    }

    public @Bean BlobStore testBlobStore() throws StorageException {
        return new FileBlobStore("/tmp/gwc");
    }

    public @Bean TransientCache testTransientCache() {
        return new TransientCache(0, 0, 0);
    }
}
