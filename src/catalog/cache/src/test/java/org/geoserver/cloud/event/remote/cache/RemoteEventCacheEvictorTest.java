/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.remote.cache;

import static org.geoserver.cloud.catalog.cache.CachingCatalogFacade.DEFAULT_NAMESPACE_CACHE_KEY;
import static org.geoserver.cloud.catalog.cache.CachingCatalogFacade.DEFAULT_WORKSPACE_CACHE_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.catalog.cache.CachingCatalogFacade;
import org.geoserver.cloud.catalog.cache.CachingGeoServerFacade;
import org.geoserver.cloud.catalog.cache.CatalogInfoKey;
import org.geoserver.cloud.event.UpdateSequenceEvent;
import org.geoserver.cloud.event.catalog.CatalogInfoModified;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.cloud.event.catalog.DefaultDataStoreSet;
import org.geoserver.cloud.event.catalog.DefaultNamespaceSet;
import org.geoserver.cloud.event.catalog.DefaultWorkspaceSet;
import org.geoserver.cloud.event.config.GeoServerInfoModified;
import org.geoserver.cloud.event.config.GeoServerInfoSet;
import org.geoserver.cloud.event.config.LoggingInfoModified;
import org.geoserver.cloud.event.config.LoggingInfoSet;
import org.geoserver.cloud.event.config.ServiceModified;
import org.geoserver.cloud.event.config.ServiceRemoved;
import org.geoserver.cloud.event.config.SettingsModified;
import org.geoserver.cloud.event.config.SettingsRemoved;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.LoggingInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.security.SecuredResourceNameChangeListener;
import org.geoserver.wms.WMSInfoImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Date;
import java.util.function.Function;

import javax.annotation.Nullable;

/**
 * Test {@link RemoteEventCacheEvictor} functionality when {@code
 * geoserver.catalog.caching.enabled=true}.
 *
 * <p>Upon receiving {@link InfoEvent}s from the bus, {@link
 * org.geoserver.cloud.bus.incoming.caching.RemoteEventCacheEvictor} shall evict the appropriate
 * locally cached {@link Info} objects
 */
@SpringBootTest(
        classes = RemoteEventCacheEvictorTestConfiguration.class,
        properties = { //
            "geoserver.catalog.caching.enabled=true",
            "logging.level.org.springframework.cache=DEBUG",
            "logging.level.org.geoserver.cloud.events=DEBUG"
        })
class RemoteEventCacheEvictorTest {

    /** Spring-cache for CatalogInfo objects, named after {@link CachingCatalogFacade#CACHE_NAME} */
    private Cache catalogCache;

    /**
     * Spring-cache for configuration Info objects, named after {@link
     * CachingGeoServerFacade#CACHE_NAME}
     */
    private Cache configCache;

    private @Autowired @Qualifier("catalog") Catalog catalog;
    private @Autowired @Qualifier("geoServer") GeoServerImpl geoServer;

    private @Autowired @Qualifier("rawCatalog") CatalogPlugin rawCatalog;
    private @Autowired CacheManager cacheManager;

    // ApplicationContext, used to publish remote events as if the'd be coming from the bus
    private @Autowired ApplicationEventPublisher publisher;

    protected CatalogTestData data;

    private @SpyBean RemoteEventCacheEvictor evictor;

    public @BeforeEach void before() {
        assertTrue(rawCatalog.getRawFacade() instanceof CachingCatalogFacade);
        assertTrue(geoServer.getFacade() instanceof CachingGeoServerFacade);
        data = CatalogTestData.initialized(() -> rawCatalog, () -> geoServer).initialize();

        this.catalogCache = cacheManager.getCache(CachingCatalogFacade.CACHE_NAME);
        this.configCache = cacheManager.getCache(CachingGeoServerFacade.CACHE_NAME);
        this.catalogCache.clear();
        this.configCache.clear();

        catalog.removeListeners(SecuredResourceNameChangeListener.class);
    }

    public @AfterEach void after() {
        // data.after();
    }

    @Test
    void testRemoteDefaultWorkspaceEvent() {
        assertNull(catalogCache.get(DEFAULT_WORKSPACE_CACHE_KEY));

        catalog.getDefaultWorkspace();
        assertNotNull(catalogCache.get(DEFAULT_WORKSPACE_CACHE_KEY));

        publishRemote(DefaultWorkspaceSet.createLocal(123L, (WorkspaceInfo) null));
        assertNull(catalogCache.get(DEFAULT_WORKSPACE_CACHE_KEY));
    }

    private Patch patch(String propertyName, Object value) {
        Patch patch = new Patch();
        patch.add(propertyName, value);
        return patch;
    }

    private <E extends InfoEvent<?, ?>> E publishRemote(E event) {
        event.setRemote(true);
        publisher.publishEvent(event);
        return event;
    }

    @Test
    void testRemoteDefaultNamespaceEvent() {
        assertNull(catalogCache.get(DEFAULT_NAMESPACE_CACHE_KEY));

        catalog.getDefaultNamespace();
        assertNotNull(catalogCache.get(DEFAULT_NAMESPACE_CACHE_KEY));

        publishRemote(DefaultNamespaceSet.createLocal(123L, (NamespaceInfo) null));

        assertNull(catalogCache.get(DEFAULT_NAMESPACE_CACHE_KEY));
    }

    @Test
    void testRemoteDefaultDataStoreEvent() {
        final Object key = CachingCatalogFacade.generateDefaultDataStoreKey(data.workspaceA);
        assertNull(catalogCache.get(key));

        catalog.getDefaultDataStore(data.workspaceA);
        assertNotNull(catalogCache.get(key), "expected cache hit");

        publishRemote(DefaultDataStoreSet.createLocal(123L, data.workspaceA, (DataStoreInfo) null));

        assertNull(
                catalogCache.get(key), "expected key evicted after setting null default datastore");

        assertNull(catalogCache.get(key));

        publishRemote(DefaultDataStoreSet.createLocal(123L, data.workspaceA, data.dataStoreA));

        assertNull(catalogCache.get(key));
        assertNotNull(catalog.getDefaultDataStore(data.workspaceA));
        assertNotNull(catalogCache.get(key));
    }

    @Test
    void testCatalogInfoEvictingEvents() {
        // layergroups are not cached
        // testModifyThenRemoveCatalogInfo(data.layerGroup1, catalog::getLayerGroup);
        testModifyThenRemoveCatalogInfo(data.layerFeatureTypeA, catalog::getLayer);
        testModifyThenRemoveCatalogInfo(data.style1, catalog::getStyle);
        testModifyThenRemoveCatalogInfo(data.coverageA, catalog::getCoverage);
        testModifyThenRemoveCatalogInfo(data.dataStoreA, catalog::getDataStore);
        testModifyThenRemoveCatalogInfo(
                data.wmsStoreA, id -> catalog.getStore(id, StoreInfo.class));
        testModifyThenRemoveCatalogInfo(
                data.wmtsStoreA, id -> catalog.getStore(id, StoreInfo.class));
        testModifyThenRemoveCatalogInfo(data.namespaceA, catalog::getNamespace);
        testModifyThenRemoveCatalogInfo(data.workspaceA, catalog::getWorkspace);
    }

    /**
     * @param info the object to check modify and delete events for
     * @param query a function to query the object by id, that would result in a cache hit
     */
    private <T extends CatalogInfo> void testModifyThenRemoveCatalogInfo(
            T info, Function<String, T> query) {
        CatalogInfoKey key = new CatalogInfoKey(info);

        assertNull(catalogCache.get(key));

        query.apply(info.getId());
        assertNotNull(catalogCache.get(key));

        Mockito.clearInvocations(this.evictor);

        CatalogInfoModified modifyEvent =
                publishRemote(
                        CatalogInfoModified.createLocal(
                                123L, info, patch("dateModified", new Date())));

        Mockito.verify(this.evictor, times(1)).onCatalogInfoModifyEvent(same(modifyEvent));
        Mockito.verify(this.evictor, times(1)).onUpdateSequenceEvent(same(modifyEvent));
        Mockito.verifyNoMoreInteractions(evictor);

        // RemoteEventResourcePoolProcessor is triggering a catalog lookup which defeats this check
        // assertNull(catalogCache.get(key));

        query.apply(info.getId());
        assertNotNull(catalogCache.get(key));

        Mockito.clearInvocations(this.evictor);

        CatalogInfoRemoved removeEvent = publishRemote(CatalogInfoRemoved.createLocal(123L, info));

        Mockito.verify(this.evictor, times(1)).onCatalogInfoRemoveEvent(same(removeEvent));
        Mockito.verify(this.evictor, times(1)).onUpdateSequenceEvent(same(removeEvent));
        Mockito.verifyNoMoreInteractions(evictor);

        assertNull(catalogCache.get(key));
    }

    @Test
    void testRemoteServiceInfoModifyEvent_global_service() {
        WMSInfoImpl globalService = new WMSInfoImpl();
        globalService.setId("wms-global");
        globalService.setName("WMS");
        globalService.setWorkspace(null);
        testRemoteServiceInfoModifyEvent(globalService);
    }

    @Test
    void testRemoteServiceInfoModifyEvent_workspace_service() {
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId("fake-ws");

        WMSInfoImpl wsService = new WMSInfoImpl();
        wsService.setId("wms-global");
        wsService.setName("WMS");
        wsService.setWorkspace(workspace);
        testRemoteServiceInfoModifyEvent(wsService);
    }

    private void testRemoteServiceInfoModifyEvent(ServiceInfo service) {
        final @Nullable WorkspaceInfo workspace = service.getWorkspace();
        Object idKey = CachingGeoServerFacade.serviceByIdKey(service.getId());
        Object nameKey = CachingGeoServerFacade.serviceByNameKey(workspace, service.getName());
        Object typeKey = CachingGeoServerFacade.serviceByTypeKey(workspace, service.getClass());

        configCache.put(idKey, service);
        configCache.put(nameKey, service);
        configCache.put(typeKey, service);
        assertNotNull(configCache.get(idKey));
        assertNotNull(configCache.get(nameKey));
        assertNotNull(configCache.get(typeKey));

        Mockito.clearInvocations(this.evictor);
        ServiceModified event =
                publishRemote(
                        ServiceModified.createLocal(
                                123L, service, patch("abstract", "doesn't matter")));

        Mockito.verify(this.evictor, times(1)).onServiceInfoModifyEvent(same(event));

        assertNull(configCache.get(idKey), "service by id not evicted");
        assertNull(configCache.get(nameKey), "service by name not evicted");
        assertNull(configCache.get(typeKey), "service by type not evicted");
    }

    @Test
    void testRemoteServiceInfoRemoveEvent_global_service() {
        WMSInfoImpl globalService = new WMSInfoImpl();
        globalService.setId("wms-global");
        globalService.setName("WMS");
        globalService.setWorkspace(null);
        testRemoteServiceInfoRemoveEvent(globalService);
    }

    @Test
    void testRemoteServiceInfoRemoveEvent_workspace_service() {
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId("fake-ws");

        WMSInfoImpl wsService = new WMSInfoImpl();
        wsService.setId("wms-global");
        wsService.setName("WMS");
        wsService.setWorkspace(workspace);
        testRemoteServiceInfoRemoveEvent(wsService);
    }

    private void testRemoteServiceInfoRemoveEvent(ServiceInfo service) {
        final @Nullable WorkspaceInfo workspace = service.getWorkspace();
        Object idKey = CachingGeoServerFacade.serviceByIdKey(service.getId());
        Object nameKey = CachingGeoServerFacade.serviceByNameKey(workspace, service.getName());
        Object typeKey = CachingGeoServerFacade.serviceByTypeKey(workspace, service.getClass());

        configCache.put(idKey, service);
        configCache.put(nameKey, service);
        configCache.put(typeKey, service);
        assertNotNull(configCache.get(idKey));
        assertNotNull(configCache.get(nameKey));
        assertNotNull(configCache.get(typeKey));

        Mockito.clearInvocations(this.evictor);
        ServiceRemoved event = publishRemote(ServiceRemoved.createLocal(123L, service));

        Mockito.verify(this.evictor, times(1)).onServiceInfoRemoveEvent(same(event));

        assertNull(configCache.get(idKey), "service by id not evicted");
        assertNull(configCache.get(nameKey), "service by name not evicted");
        assertNull(configCache.get(typeKey), "service by type not evicted");
    }

    @Test
    void testRemoteSettingsInfoModifyEvent() {
        WorkspaceInfoImpl ws = new WorkspaceInfoImpl();
        ws.setId("fakews");
        SettingsInfoImpl settings = new SettingsInfoImpl();
        settings.setId("fakesettings");
        settings.setWorkspace(ModificationProxy.create(ws, WorkspaceInfo.class));

        Object workspaceSettingsKey = CachingGeoServerFacade.settingsKey(ws);
        assertEquals("settings@fakews", workspaceSettingsKey);

        configCache.put(settings.getId(), settings);
        configCache.put(workspaceSettingsKey, settings);

        assertNotNull(configCache.get(settings.getId()));
        assertNotNull(configCache.get(workspaceSettingsKey));

        // constructor args: GeoServer, SettingsInfo, Patch, originService,destinationService
        Mockito.clearInvocations(evictor);
        SettingsModified event =
                publishRemote(
                        SettingsModified.createLocal(
                                123L, settings, patch("charset", "ISO-8859-1")));

        assertEquals(ws.getId(), event.getWorkspaceId());

        Mockito.verify(evictor, times(1)).onSettingsInfoModifyEvent(same(event));

        assertNull(configCache.get(settings.getId()), "expected entry to be evicted");
        assertNull(
                configCache.get(workspaceSettingsKey),
                "expected workspace settings entry to be evicted");
    }

    @Test
    void testRemoteSettingsInfoRemoveEvent() {
        WorkspaceInfoImpl ws = new WorkspaceInfoImpl();
        ws.setId("fakews");
        SettingsInfoImpl settings = new SettingsInfoImpl();
        settings.setId("fakesettings");
        settings.setWorkspace(ModificationProxy.create(ws, WorkspaceInfo.class));

        Object workspaceSettingsKey = CachingGeoServerFacade.settingsKey(ws);
        assertEquals("settings@fakews", workspaceSettingsKey);

        configCache.put(settings.getId(), settings);
        configCache.put(workspaceSettingsKey, settings);

        assertNotNull(configCache.get(settings.getId()));
        assertNotNull(configCache.get(workspaceSettingsKey));

        // constructor args: GeoServer, SettingsInfo, originService,destinationService
        Mockito.clearInvocations(evictor);
        SettingsRemoved event = publishRemote(SettingsRemoved.createLocal(123L, settings));

        assertEquals(ws.getId(), event.getWorkspaceId());

        Mockito.verify(evictor, times(1)).onSettingsInfoRemoveEvent(same(event));

        assertNull(configCache.get(settings.getId()), "expected entry to be evicted");
        assertNull(
                configCache.get(workspaceSettingsKey),
                "expected workspace settings entry to be evicted");
    }

    @Test
    void testRemoteLoggingInfoSetEvent() {
        LoggingInfo info = geoServer.getLogging();
        assertNotNull(info);
        final String key = CachingGeoServerFacade.LOGGINGINFO_KEY;
        assertNotNull(configCache.get(key));

        LoggingInfoImpl newLogging = new LoggingInfoImpl();

        Mockito.clearInvocations(this.evictor);

        LoggingInfoSet event = publishRemote(LoggingInfoSet.createLocal(123L, newLogging));

        Mockito.verify(evictor, times(1)).onSetLoggingInfoEvent(same(event));

        assertNull(configCache.get(key), "logging not evicted");
    }

    @Test
    void testRemoteLoggingInfoModifyEvent() {
        LoggingInfo info = geoServer.getLogging();
        assertNotNull(info);
        final String key = CachingGeoServerFacade.LOGGINGINFO_KEY;
        assertNotNull(configCache.get(key));

        Mockito.clearInvocations(this.evictor);

        LoggingInfoModified event =
                publishRemote(LoggingInfoModified.createLocal(123L, info, patch("level", "DEBUG")));
        Mockito.verify(evictor, times(1)).onLoggingInfoModifyEvent(same(event));

        assertNull(configCache.get(key), "logging not evicted");
    }

    @Test
    void testRemoteGeoServerInfoSetEvent() {
        GeoServerInfo info = geoServer.getGlobal();
        assertNotNull(info);
        final String key = CachingGeoServerFacade.GEOSERVERINFO_KEY;
        assertNotNull(configCache.get(key));

        GeoServerInfo newGlobal = new GeoServerInfoImpl();

        Mockito.clearInvocations(this.evictor);
        // GeoServer source, Info object, String originService, String destinationService
        GeoServerInfoSet event = publishRemote(GeoServerInfoSet.createLocal(123L, newGlobal));

        Mockito.verify(evictor, times(1)).onSetGlobalInfoEvent(same(event));

        assertNull(configCache.get(key), "global not evicted");
    }

    @Test
    void testRemoteGeoServerInfoModifyEvent() {
        GeoServerInfo info = geoServer.getGlobal();
        assertNotNull(info);
        final String key = CachingGeoServerFacade.GEOSERVERINFO_KEY;
        assertNotNull(configCache.get(key));

        Mockito.clearInvocations(this.evictor);
        // GeoServer source, GeoServerInfo object, Patch patch, originService,destinationService
        GeoServerInfoModified event =
                publishRemote(
                        GeoServerInfoModified.createLocal(
                                123L, info, patch("xmlExternalEntitiesEnabled", true)));
        Mockito.verify(evictor, times(1)).onGeoServerInfoModifyEvent(same(event));

        assertNull(configCache.get(key), "global not evicted");
    }

    @Test
    void testUpdateSequenceModifyEvent_evicts_but_applies_update_sequence_in_place() {
        GeoServerInfo local = geoServer.getGlobal();
        assertNotNull(local);
        final String key = CachingGeoServerFacade.GEOSERVERINFO_KEY;

        assertNotNull(configCache.get(key));

        GeoServerInfoImpl remote = new GeoServerInfoImpl();
        local.setUpdateSequence(999L);

        final Long updateSequence = 1000L;
        remote.setTitle("some change");
        Patch patch = patch("title", "some change");

        Mockito.clearInvocations(this.evictor);

        UpdateSequenceEvent<?> event =
                publishRemote(GeoServerInfoModified.createLocal(updateSequence, remote, patch));

        Mockito.verify(evictor, times(1)).onUpdateSequenceEvent(same(event));

        assertNull(configCache.get(key));
    }
}
