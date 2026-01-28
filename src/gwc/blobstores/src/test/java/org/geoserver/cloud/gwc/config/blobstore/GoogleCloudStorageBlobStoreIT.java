/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.config.blobstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.geowebcache.GeoWebCacheEnvironment;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.GeoWebCacheExtensions;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.blobstore.gcs.GoogleCloudStorageBlobStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verify {@link GoogleCloudStorageBlobStore} works by connecting to an GCS emulator
 *
 * @see GoogleCloudStorageContainerSupport
 */
@Testcontainers(disabledWithoutDocker = true)
class GoogleCloudStorageBlobStoreIT {

    public static GoogleCloudStorageContainerSupport containerSupport = new GoogleCloudStorageContainerSupport();

    private GoogleCloudStorageBlobStore store;
    private TileLayerDispatcher dispatcher;
    private final List<String> names = List.of("testLayer", "testLayer1", "testLayer2");

    @BeforeAll
    static void setUpContainer() throws Exception {
        containerSupport.before();
        StaticWebApplicationContext mockContext = new StaticWebApplicationContext();
        mockContext.registerSingleton("gwcEnv", GeoWebCacheEnvironment.class);
        new GeoWebCacheExtensions().setApplicationContext(mockContext);
    }

    @AfterAll
    static void tearDownContainer() {
        containerSupport.after();
    }

    @BeforeEach
    void setUpStore(TestInfo testInfo) throws Exception {
        TileLayerDispatcher layers = createMockLayerDispatcher();

        String prefix = testInfo.getDisplayName();
        store = containerSupport.createBlobStore(prefix, layers);
    }

    @AfterEach
    void tearDownStore() {
        store.destroy();
    }

    private TileLayerDispatcher createMockLayerDispatcher() {
        dispatcher = mock(TileLayerDispatcher.class);
        List<TileLayer> layers = Stream.of("testLayer", "testLayer1", "testLayer2")
                .map(name -> {
                    TileLayer mock = mock(TileLayer.class);
                    when(mock.getName()).thenReturn(name);
                    when(mock.getId()).thenReturn(name);
                    when(mock.getGridSubsets()).thenReturn(Collections.singleton("testGridSet"));
                    when(mock.getMimeTypes()).thenReturn(Arrays.asList(org.geowebcache.mime.ImageMime.png));
                    try {
                        when(dispatcher.getTileLayer(name)).thenReturn(mock);
                    } catch (GeoWebCacheException e) {
                        throw new IllegalStateException(e);
                    }
                    return mock;
                })
                .toList();
        when(dispatcher.getLayerList()).thenReturn(layers);
        when(dispatcher.getLayerNames()).thenReturn(Set.copyOf(names));
        when(dispatcher.getLayerCount()).thenReturn(names.size());
        return dispatcher;
    }

    @Test
    void testPutGet() throws Exception {
        final String layerName = names.getFirst();
        final long[] xyz = new long[] {0, 0, 0};
        final String gridSetId = "EPSG:3857";
        final String format = "image/png";
        final Map<String, String> parameters = null;
        byte[] contents = new byte[] {1, 2, 3, 4, 5, 6, 7};
        final Resource blob = new ByteArrayResource(contents);

        TileObject tile = TileObject.createCompleteTileObject(layerName, xyz, gridSetId, format, parameters, blob);
        store.put(tile);

        TileObject query = TileObject.createQueryTileObject(layerName, xyz, gridSetId, format, parameters);
        assertThat(store.get(query)).isTrue();
        assertThat(query.getBlob()).isNotNull();
        byte[] readContents = IOUtils.toByteArray(query.getBlob().getInputStream());
        assertThat(readContents).isEqualTo(contents);
    }
}
