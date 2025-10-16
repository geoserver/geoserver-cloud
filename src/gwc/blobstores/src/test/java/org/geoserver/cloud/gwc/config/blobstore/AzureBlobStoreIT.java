/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.config.blobstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.geowebcache.GeoWebCacheEnvironment;
import org.geowebcache.GeoWebCacheExtensions;
import org.geowebcache.azure.AzureBlobStore;
import org.geowebcache.azure.AzureBlobStoreInfo;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.locks.MemoryLockProvider;
import org.geowebcache.storage.BlobStore;
import org.geowebcache.storage.StorageException;
import org.geowebcache.storage.TileObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verify {@link AzureBlobStore} works by connecting to an Azurite container
 *
 * @see AzuriteContainer
 */
@Testcontainers(disabledWithoutDocker = true)
class AzureBlobStoreIT {

    @Container
    static AzuriteContainer azurite = new AzuriteContainer();

    protected AzureBlobStoreInfo newAzureBlobStoreInfo() {
        AzureBlobStoreInfo bsi = new AzureBlobStoreInfo();

        String accountName = azurite.getAccountName();
        String accountKey = azurite.getAccountKey();
        String blobsUrl = azurite.getBlobsUrl();

        bsi.setAccountName(accountName);
        bsi.setAccountKey(accountKey);
        bsi.setServiceURL(blobsUrl);
        bsi.setUseHTTPS(false);
        bsi.setContainer("azureblobstoretest");
        bsi.setName("AzureBlobStoreTest");
        bsi.setEnabled(true);
        return bsi;
    }

    static StaticApplicationContext stubAppContext;

    static @BeforeAll void setUpContext() {
        GeoWebCacheExtensions extensions = new GeoWebCacheExtensions();
        GeoWebCacheEnvironment environment = new GeoWebCacheEnvironment();

        stubAppContext = new StaticApplicationContext();
        stubAppContext.registerBean(GeoWebCacheExtensions.class, () -> extensions);
        stubAppContext.registerBean(GeoWebCacheEnvironment.class, () -> environment);
        stubAppContext.refresh();
    }

    static @AfterAll void closeContext() {
        stubAppContext.close();
    }

    @Test
    void createBlobStore() throws StorageException {
        AzureBlobStoreInfo info = newAzureBlobStoreInfo();
        BlobStore store = info.createInstance(mock(TileLayerDispatcher.class), new MemoryLockProvider());
        assertThat(store).isInstanceOf(AzureBlobStore.class);
    }

    @Test
    void testPutGet() throws Exception {
        TileLayerDispatcher layers = mock(TileLayerDispatcher.class);

        AzureBlobStoreInfo info = newAzureBlobStoreInfo();
        AzureBlobStore store = (AzureBlobStore) info.createInstance(layers, new MemoryLockProvider());

        final String layerName = "FakeLayer";
        final long[] xyz = new long[] {0, 0, 0};
        final String gridSetId = "EPSG:3857";
        final String format = "image/png";
        final Map<String, String> parameters = null;
        byte[] contents = new byte[] {1, 2, 3, 4, 5, 6, 7};
        final Resource blob = new ByteArrayResource(contents);

        TileLayer tileLayer = mock(TileLayer.class);
        when(tileLayer.getId()).thenReturn(layerName);
        when(layers.getTileLayer(layerName)).thenReturn(tileLayer);

        TileObject tile = TileObject.createCompleteTileObject(layerName, xyz, gridSetId, format, parameters, blob);
        store.put(tile);

        TileObject query = TileObject.createQueryTileObject(layerName, xyz, gridSetId, format, parameters);
        assertThat(store.get(query)).isTrue();
        assertThat(query.getBlob()).isNotNull();
        byte[] readContents = IOUtils.toByteArray(query.getBlob().getInputStream());
        assertThat(readContents).isEqualTo(contents);
    }
}
