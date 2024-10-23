/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.repository;

import static org.geoserver.cloud.gwc.event.GeoWebCacheEvent.Type.CREATED;
import static org.geoserver.cloud.gwc.event.GeoWebCacheEvent.Type.DELETED;
import static org.geoserver.cloud.gwc.event.GeoWebCacheEvent.Type.MODIFIED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.geoserver.cloud.gwc.event.BlobStoreEvent;
import org.geoserver.cloud.gwc.event.GeoWebCacheEvent;
import org.geoserver.cloud.gwc.event.GridsetEvent;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.config.BlobStoreConfigurationListener;
import org.geowebcache.config.BlobStoreInfo;
import org.geowebcache.config.ConfigurationResourceProvider;
import org.geowebcache.config.DefaultGridsets;
import org.geowebcache.config.FileBlobStoreInfo;
import org.geowebcache.config.XMLFileResourceProvider;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.storage.DefaultStorageFinder;
import org.geowebcache.util.ApplicationContextProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @since 1.0
 */
class CloudGwcXmlConfigurationTest {

    private CloudGwcXmlConfiguration config;

    private AtomicReference<GeoWebCacheEvent> mockEventPublisher;

    private @TempDir Path tmpdir;
    private Path configFile;

    private final DefaultGridsets defaultGridsets = new DefaultGridsets(false, false);

    @BeforeEach
    void setUp() throws IOException, GeoWebCacheException {
        mockEventPublisher = new AtomicReference<>();

        configFile = tmpdir.resolve("geowebcache.xml");
        Files.copy(getClass().getResourceAsStream("/geowebcache-empty.xml"), configFile);

        config = createStubConfig();
    }

    private CloudGwcXmlConfiguration createStubConfig() throws GeoWebCacheException {
        ApplicationContextProvider appCtx = mock(ApplicationContextProvider.class);

        ConfigurationResourceProvider inFac = new XMLFileResourceProvider(
                "geowebcache.xml", appCtx, tmpdir.toAbsolutePath().toString(), (DefaultStorageFinder) null);

        CloudGwcXmlConfiguration xmlConfig = new CloudGwcXmlConfiguration(appCtx, inFac, mockEventPublisher::set);
        GridSetBroker broker = new GridSetBroker(List.of(defaultGridsets));
        xmlConfig.setGridSetBroker(broker);
        xmlConfig.afterPropertiesSet();
        return xmlConfig;
    }

    @Test
    void testReinitialize() throws Exception {
        assertTrue(config.getGridSets().isEmpty());
        GridSet gridSet = defaultGridsets.worldCRS84Quad();
        config.addGridSet(gridSet);
        assertEquals(Set.of(gridSet.getName()), config.getGridSetNames());
        config.reinitialize();
        assertEquals(Set.of(gridSet.getName()), config.getGridSetNames());
        assertEquals(gridSet, config.getGridSet(gridSet.getName()).orElse(null));
    }

    @Test
    void testGridSetAdd() {
        GridSet gridset = defaultGridsets.worldCRS84Quad();
        assertNull(config.getGridSet(gridset.getName()).orElse(null));
        config.addGridSet(gridset);
        expect(new GridsetEvent(config, CREATED, gridset.getName()));
        assertEquals(gridset, config.getGridSet(gridset.getName()).orElse(null));
    }

    @Test
    void testGridSetModify() {
        GridSet gridset = defaultGridsets.worldEpsg3857();
        config.addGridSet(gridset);
        gridset.setDescription("modified");

        config.modifyGridSet(gridset);
        expect(new GridsetEvent(config, MODIFIED, gridset.getName()));
        assertEquals(gridset, config.getGridSet(gridset.getName()).orElse(null));
    }

    @Test
    void testGridSetRemove() {
        GridSet gridset = defaultGridsets.worldCRS84Quad();
        config.addGridSet(gridset);
        assertEquals(gridset, config.getGridSet(gridset.getName()).orElse(null));

        config.removeGridSet(gridset.getName());
        expect(new GridsetEvent(config, DELETED, gridset.getName()));
        assertFalse(config.getGridSet(gridset.getName()).isPresent());
    }

    @Test
    void testBlobStoreAdd() {
        BlobStoreInfo bs = new FileBlobStoreInfo("testbs");
        config.addBlobStore(bs);
        expect(new BlobStoreEvent(config, CREATED, bs.getName()));
        assertEquals(bs, config.getBlobStore(bs.getName()).orElse(null));
    }

    @Test
    void testBlobStoreModify() {
        BlobStoreInfo bs = new FileBlobStoreInfo("testbs");
        config.addBlobStore(bs);

        bs.setEnabled(true);
        bs.setDefault(true);
        bs.setName("newname");
        config.modifyBlobStore(bs);
        expect(new BlobStoreEvent(config, MODIFIED, bs.getName()));
        assertEquals(bs, config.getBlobStore(bs.getName()).orElse(null));
    }

    @Test
    void testBlobStoreRename() {
        BlobStoreInfo bs = new FileBlobStoreInfo("testbs");
        config.addBlobStore(bs);

        config.renameBlobStore("testbs", "newbsname");
        expect(new BlobStoreEvent(config, MODIFIED, "newbsname"));

        assertFalse(config.getBlobStore("testbs").isPresent());
        assertTrue(config.getBlobStore("newbsname").isPresent());

        BlobStoreInfo expected = (BlobStoreInfo) bs.clone();
        expected.setName("newbsname");
        assertEquals(expected, config.getBlobStore("newbsname").orElse(null));
    }

    @Test
    void testBlobStoreRemove() {
        config.addBlobStore(new FileBlobStoreInfo("testbs"));
        assertTrue(config.getBlobStore("testbs").isPresent());
        config.removeBlobStore("testbs");
        expect(new BlobStoreEvent(config, DELETED, "testbs"));
        assertFalse(config.getBlobStore("testbs").isPresent());
    }

    @Test
    void testOnGridsetEvent_ignore_self_issued_event() throws Exception {
        // if event.getSource() == config, ignore it
        assertFalse(config.onGridsetEvent(new GridsetEvent(config)), "self issued event should be ignored");
    }

    @Test
    void testOnGridsetEvent_Created() throws Exception {
        final CloudGwcXmlConfiguration local = this.config;
        final CloudGwcXmlConfiguration remote = createStubConfig();

        GridSet gridset = defaultGridsets.worldEpsg4326x2();

        assertFalse(local.getGridSet(gridset.getName()).isPresent());
        assertFalse(remote.getGridSet(gridset.getName()).isPresent());

        remote.addGridSet(gridset);
        assertFalse(local.getGridSet(gridset.getName()).isPresent());
        assertTrue(remote.getGridSet(gridset.getName()).isPresent());

        final Object unknownSource = new Object();
        GridsetEvent remoteEvent = new GridsetEvent(unknownSource);
        remoteEvent.setGridsetId(gridset.getName());
        remoteEvent.setEventType(CREATED);

        assertTrue(local.onGridsetEvent(remoteEvent));
        GridSet actual = local.getGridSet(gridset.getName()).orElse(null);
        assertEquals(gridset, actual);
    }

    @Test
    void testOnGridsetEvent_Modified() throws Exception {
        GridSet gridset = defaultGridsets.worldEpsg4326x2();

        final CloudGwcXmlConfiguration local = this.config;
        final CloudGwcXmlConfiguration remote = createStubConfig();
        remote.addGridSet(gridset);
        local.deinitialize();
        local.afterPropertiesSet();
        assertTrue(local.getGridSet(gridset.getName()).isPresent());
        assertTrue(remote.getGridSet(gridset.getName()).isPresent());

        gridset.setDescription("modified");
        remote.modifyGridSet(gridset);

        final Object unknownSource = new Object();
        GridsetEvent event = new GridsetEvent(unknownSource);
        event.setGridsetId(gridset.getName());
        event.setEventType(MODIFIED);

        local.onGridsetEvent(event);
        GridSet actual = local.getGridSet(gridset.getName()).orElse(null);
        assertEquals(gridset, actual);
    }

    @Test
    void testOnGridsetEvent_Removed() throws Exception {
        GridSet gridset = defaultGridsets.worldEpsg4326x2();

        final CloudGwcXmlConfiguration local = this.config;
        final CloudGwcXmlConfiguration remote = createStubConfig();
        remote.addGridSet(gridset);

        assertTrue(remote.getGridSet(gridset.getName()).isPresent());
        assertFalse(local.getGridSet(gridset.getName()).isPresent());

        local.deinitialize();
        local.afterPropertiesSet();
        assertTrue(local.getGridSet(gridset.getName()).isPresent());

        remote.removeGridSet(gridset.getName());
        assertFalse(remote.getGridSet(gridset.getName()).isPresent());
        assertTrue(local.getGridSet(gridset.getName()).isPresent());

        final Object unknownSource = new Object();
        GridsetEvent event = new GridsetEvent(unknownSource);
        event.setGridsetId(gridset.getName());
        event.setEventType(DELETED);

        local.onGridsetEvent(event);
        assertFalse(local.getGridSet(gridset.getName()).isPresent());
    }

    @Test
    void testOnBlobStoreEvent_Created() throws Exception {
        final CloudGwcXmlConfiguration local = this.config;
        final CloudGwcXmlConfiguration remote = createStubConfig();

        BlobStoreConfigurationListener localListener = mock(BlobStoreConfigurationListener.class);
        BlobStoreConfigurationListener remoteListener = mock(BlobStoreConfigurationListener.class);
        local.addBlobStoreListener(localListener);
        remote.addBlobStoreListener(remoteListener);

        BlobStoreInfo bsi = new FileBlobStoreInfo("test");

        assertFalse(remote.getBlobStore(bsi.getName()).isPresent());
        assertFalse(local.getBlobStore(bsi.getName()).isPresent());

        remote.addBlobStore(bsi);
        assertTrue(remote.getBlobStore(bsi.getName()).isPresent());
        assertFalse(local.getBlobStore(bsi.getName()).isPresent());
        verify(remoteListener).handleAddBlobStore(bsi);

        final Object unknownSource = new Object();
        BlobStoreEvent event = new BlobStoreEvent(unknownSource);
        event.setBlobStoreId(bsi.getName());
        event.setEventType(CREATED);

        local.onBlobStoreEvent(event);
        BlobStoreInfo actual = local.getBlobStore(bsi.getName()).orElse(null);
        assertEquals(bsi, actual);
        verify(localListener).handleAddBlobStore(bsi);
    }

    @Test
    void testOnBlobStoreEvent_Modified_FileBlobStore() throws Exception {
        final CloudGwcXmlConfiguration local = this.config;
        final CloudGwcXmlConfiguration remote = createStubConfig();
        BlobStoreConfigurationListener localListener = mock(BlobStoreConfigurationListener.class);
        BlobStoreConfigurationListener remoteListener = mock(BlobStoreConfigurationListener.class);
        local.addBlobStoreListener(localListener);
        remote.addBlobStoreListener(remoteListener);

        FileBlobStoreInfo bsi = new FileBlobStoreInfo("test");
        remote.addBlobStore(bsi);
        local.deinitialize();
        local.afterPropertiesSet();

        assertTrue(remote.getBlobStore(bsi.getName()).isPresent());
        assertTrue(local.getBlobStore(bsi.getName()).isPresent());

        bsi.setDefault(!bsi.isDefault());
        bsi.setEnabled(!bsi.isEnabled());
        bsi.setBaseDirectory("/tmp/newdir");

        remote.modifyBlobStore(bsi);
        verify(remoteListener).handleModifyBlobStore(bsi);

        final Object unknownSource = new Object();
        BlobStoreEvent event = new BlobStoreEvent(unknownSource);
        event.setBlobStoreId(bsi.getName());
        event.setEventType(MODIFIED);

        local.onBlobStoreEvent(event);
        BlobStoreInfo actual = local.getBlobStore(bsi.getName()).orElse(null);
        assertEquals(bsi, actual);
        verify(localListener).handleModifyBlobStore(bsi);
    }

    @Test
    void testOnBlobStoreEvent_Renamed() throws Exception {
        final CloudGwcXmlConfiguration local = this.config;
        final CloudGwcXmlConfiguration remote = createStubConfig();

        BlobStoreInfo bsi = new FileBlobStoreInfo("test");
        remote.addBlobStore(bsi);
        local.deinitialize();
        local.afterPropertiesSet();

        assertTrue(remote.getBlobStore(bsi.getName()).isPresent());
        assertTrue(local.getBlobStore(bsi.getName()).isPresent());

        final String oldName = bsi.getName();
        final String newName = "newname";
        BlobStoreInfo expected = (BlobStoreInfo) bsi.clone();
        expected.setName(newName);

        remote.renameBlobStore(oldName, newName);
        assertEquals(expected, remote.getBlobStore(newName).orElse(null));
        assertFalse(local.getBlobStore(newName).isPresent());

        final Object unknownSource = new Object();
        BlobStoreEvent event = new BlobStoreEvent(unknownSource);
        event.setBlobStoreId(newName);
        event.setEventType(MODIFIED);

        local.onBlobStoreEvent(event);
        assertEquals(expected, local.getBlobStore(newName).orElse(null));
    }

    @Test
    void testOnBlobStoreEvent_Deleted() throws Exception {
        final CloudGwcXmlConfiguration local = this.config;
        final CloudGwcXmlConfiguration remote = createStubConfig();

        BlobStoreConfigurationListener localListener = mock(BlobStoreConfigurationListener.class);
        BlobStoreConfigurationListener remoteListener = mock(BlobStoreConfigurationListener.class);
        local.addBlobStoreListener(localListener);
        remote.addBlobStoreListener(remoteListener);

        BlobStoreInfo bsi = new FileBlobStoreInfo("test");
        remote.addBlobStore(bsi);
        local.deinitialize();
        local.afterPropertiesSet();

        assertTrue(remote.getBlobStore(bsi.getName()).isPresent());
        assertTrue(local.getBlobStore(bsi.getName()).isPresent());

        remote.removeBlobStore(bsi.getName());
        assertFalse(remote.getBlobStore(bsi.getName()).isPresent());
        assertTrue(local.getBlobStore(bsi.getName()).isPresent());
        verify(remoteListener).handleRemoveBlobStore(bsi);

        final Object unknownSource = new Object();
        BlobStoreEvent event = new BlobStoreEvent(unknownSource);
        event.setBlobStoreId(bsi.getName());
        event.setEventType(DELETED);

        local.onBlobStoreEvent(event);
        assertFalse(local.getBlobStore(bsi.getName()).isPresent());
        verify(localListener).handleRemoveBlobStore(bsi);
    }

    private void expect(GridsetEvent expected) {
        GridsetEvent captured = assertEvent(expected);
        assertEquals(expected.getGridsetId(), captured.getGridsetId());
    }

    private void expect(BlobStoreEvent expected) {
        BlobStoreEvent captured = assertEvent(expected);
        assertEquals(expected.getBlobStoreId(), captured.getBlobStoreId());
    }

    @SuppressWarnings("unchecked")
    private <E extends GeoWebCacheEvent> E assertEvent(E expected) {
        GeoWebCacheEvent captured = this.mockEventPublisher.get();
        assertNotNull(captured);
        assertSame(expected.getClass(), captured.getClass());
        assertEquals(expected.getEventType(), captured.getEventType());
        assertEquals(expected.getId(), captured.getId());
        return (E) captured;
    }
}
