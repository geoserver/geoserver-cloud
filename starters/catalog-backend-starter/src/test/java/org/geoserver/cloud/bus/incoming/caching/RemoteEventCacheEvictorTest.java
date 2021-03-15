/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.incoming.caching;

import static org.geoserver.cloud.catalog.caching.CachingCatalogFacade.DEFAULT_NAMESPACE_CACHE_KEY;
import static org.geoserver.cloud.catalog.caching.CachingCatalogFacade.DEFAULT_WORKSPACE_CACHE_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;

import java.util.Date;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.autoconfigure.testconfiguration.AutoConfigurationTestConfiguration;
import org.geoserver.cloud.bus.event.RemoteInfoEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogInfoModifyEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogInfoRemoveEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteDefaultDataStoreEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteDefaultNamespaceEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteDefaultWorkspaceEvent;
import org.geoserver.cloud.bus.event.config.RemoteGeoServerInfoModifyEvent;
import org.geoserver.cloud.bus.event.config.RemoteGeoServerInfoSetEvent;
import org.geoserver.cloud.bus.event.config.RemoteLoggingInfoModifyEvent;
import org.geoserver.cloud.bus.event.config.RemoteLoggingInfoSetEvent;
import org.geoserver.cloud.bus.event.config.RemoteServiceInfoModifyEvent;
import org.geoserver.cloud.bus.event.config.RemoteServiceInfoRemoveEvent;
import org.geoserver.cloud.bus.event.config.RemoteSettingsInfoModifyEvent;
import org.geoserver.cloud.bus.event.config.RemoteSettingsInfoRemoveEvent;
import org.geoserver.cloud.catalog.caching.CachingCatalogFacade;
import org.geoserver.cloud.catalog.caching.CachingGeoServerFacade;
import org.geoserver.cloud.catalog.caching.CatalogInfoKey;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.LoggingInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.security.SecuredResourceNameChangeListener;
import org.geoserver.wms.WMSInfoImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test {@link org.geoserver.cloud.bus.incoming.caching.RemoteEventCacheEvictor} functionality when
 * {@code geoserver.catalog.caching.enabled=true}.
 *
 * <p>Upon receiving {@link RemoteInfoEvent}s from the bus, {@link
 * org.geoserver.cloud.bus.incoming.caching.RemoteEventCacheEvictor} shall evict the appropriate
 * locally cached {@link Info} objects
 */
@SpringBootTest(
    classes = AutoConfigurationTestConfiguration.class,
    properties = { //
        "geoserver.bus.enabled=true",
        "geoserver.bus.send-events=false", // disable automatic publishing of remote events
        "geoserver.catalog.caching.enabled=true",
        "geoserver.backend.data-directory.enabled=true",
        "geoserver.backend.data-directory.location=/tmp/data_dir_autoconfiguration_test"
    }
)
@RunWith(SpringRunner.class)
public class RemoteEventCacheEvictorTest {

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
    private @Autowired BeanFactory beanFactory;
    private final String originService = "fake-origin-service";
    private final String destinationService = "**";

    public @Rule CatalogTestData data =
            CatalogTestData.initialized(() -> rawCatalog, () -> geoServer);

    private @SpyBean RemoteEventCacheEvictor evictor;

    public @Before void before() {
        assertTrue(rawCatalog.getRawFacade() instanceof CachingCatalogFacade);
        assertTrue(geoServer.getFacade() instanceof CachingGeoServerFacade);
        this.catalogCache = cacheManager.getCache(CachingCatalogFacade.CACHE_NAME);
        this.configCache = cacheManager.getCache(CachingGeoServerFacade.CACHE_NAME);
        this.catalogCache.clear();
        this.configCache.clear();

        catalog.removeListeners(SecuredResourceNameChangeListener.class);
    }

    public @Test void testRemoteDefaultWorkspaceEvent() {
        assertNull(catalogCache.get(DEFAULT_WORKSPACE_CACHE_KEY));

        catalog.getDefaultWorkspace();
        assertNotNull(catalogCache.get(DEFAULT_WORKSPACE_CACHE_KEY));

        publish(
                RemoteDefaultWorkspaceEvent.class,
                catalog,
                patch("defaultWorkspace", null),
                originService,
                destinationService);
        assertNull(catalogCache.get(DEFAULT_WORKSPACE_CACHE_KEY));
    }

    private Patch patch(String propertyName, Object value) {
        Patch patch = new Patch();
        patch.add(propertyName, value);
        return patch;
    }

    private <E extends RemoteInfoEvent<?, ?>> E publish(Class<E> eventType, Object... args) {
        E event = this.beanFactory.getBean(eventType, args);
        publisher.publishEvent(event);
        return event;
    }

    public @Test void testRemoteDefaultNamespaceEvent() {
        assertNull(catalogCache.get(DEFAULT_NAMESPACE_CACHE_KEY));

        catalog.getDefaultNamespace();
        assertNotNull(catalogCache.get(DEFAULT_NAMESPACE_CACHE_KEY));

        publish(
                RemoteDefaultNamespaceEvent.class,
                catalog,
                patch("defaultNamespace", null),
                originService,
                destinationService);
        assertNull(catalogCache.get(DEFAULT_NAMESPACE_CACHE_KEY));
    }

    public @Test void testRemoteDefaultDataStoreEvent() {
        final Object key = CachingCatalogFacade.generateDefaultDataStoreKey(data.workspaceA);
        assertNull(catalogCache.get(key));

        catalog.getDefaultDataStore(data.workspaceA);
        assertNotNull("expected cache hit", catalogCache.get(key));

        publish(
                RemoteDefaultDataStoreEvent.class,
                catalog,
                data.workspaceA,
                patch("defaultDataStore", null),
                originService,
                destinationService);
        assertNull(
                "expected key evicted after setting null default datastore", catalogCache.get(key));

        assertNull(catalog.getDefaultDataStore(data.workspaceA));
        assertNull(catalogCache.get(key));

        publish(
                RemoteDefaultDataStoreEvent.class,
                catalog,
                data.workspaceA,
                patch("defaultDataStore", data.dataStoreA),
                originService,
                destinationService);
        assertNull(catalogCache.get(key));
        assertNotNull(catalog.getDefaultDataStore(data.workspaceA));
        assertNotNull(catalogCache.get(key));
    }

    public @Test void testCatalogInfoEvictingEvents() {
        testModifyRemoveCatalogInfo(data.layerGroup1, catalog::getLayerGroup);
        testModifyRemoveCatalogInfo(data.layerFeatureTypeA, catalog::getLayer);
        testModifyRemoveCatalogInfo(data.style1, catalog::getStyle);
        testModifyRemoveCatalogInfo(data.coverageA, catalog::getCoverage);
        testModifyRemoveCatalogInfo(data.dataStoreA, catalog::getDataStore);
        testModifyRemoveCatalogInfo(data.wmsStoreA, id -> catalog.getStore(id, StoreInfo.class));
        testModifyRemoveCatalogInfo(data.wmtsStoreA, id -> catalog.getStore(id, StoreInfo.class));
        testModifyRemoveCatalogInfo(data.namespaceA, catalog::getNamespace);
        testModifyRemoveCatalogInfo(data.workspaceA, catalog::getWorkspace);
    }

    /**
     * @param info the object to check modify and delete events for
     * @param query a function to query the object by id, that would result in a cache hit
     */
    private <T extends CatalogInfo> void testModifyRemoveCatalogInfo(
            T info, Function<String, T> query) {
        CatalogInfoKey key = new CatalogInfoKey(info);

        assertNull(catalogCache.get(key));

        query.apply(info.getId());
        assertNotNull(catalogCache.get(key));

        Mockito.clearInvocations(this.evictor);

        RemoteCatalogInfoModifyEvent modifyEvent =
                publish(
                        RemoteCatalogInfoModifyEvent.class,
                        catalog,
                        info,
                        patch("dateModified", new Date()),
                        originService,
                        destinationService);
        Mockito.verify(this.evictor, times(1)).onCatalogInfoModifyEvent(same(modifyEvent));
        Mockito.verifyNoMoreInteractions(evictor);

        assertNull(catalogCache.get(key));

        query.apply(info.getId());
        assertNotNull(catalogCache.get(key));

        Mockito.clearInvocations(this.evictor);

        RemoteCatalogInfoRemoveEvent removeEvent =
                publish(
                        RemoteCatalogInfoRemoveEvent.class,
                        catalog,
                        info,
                        originService,
                        destinationService);
        Mockito.verify(this.evictor, times(1)).onCatalogInfoRemoveEvent(same(removeEvent));
        Mockito.verifyNoMoreInteractions(evictor);

        assertNull(catalogCache.get(key));
    }

    public @Test void testRemoteServiceInfoModifyEvent_global_service() {
        WMSInfoImpl globalService = new WMSInfoImpl();
        globalService.setId("wms-global");
        globalService.setName("WMS");
        globalService.setWorkspace(null);
        testRemoteServiceInfoModifyEvent(globalService);
    }

    public @Test void testRemoteServiceInfoModifyEvent_workspace_service() {
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
        RemoteServiceInfoModifyEvent event =
                publish(
                        RemoteServiceInfoModifyEvent.class,
                        geoServer,
                        service,
                        patch("abstract", "doesn't matter"),
                        originService,
                        destinationService);

        Mockito.verify(this.evictor, times(1)).onServiceInfoModifyEvent(same(event));

        assertNull("service by id not evicted", configCache.get(idKey));
        assertNull("service by name not evicted", configCache.get(nameKey));
        assertNull("service by type not evicted", configCache.get(typeKey));
    }

    public @Test void testRemoteServiceInfoRemoveEvent_global_service() {
        WMSInfoImpl globalService = new WMSInfoImpl();
        globalService.setId("wms-global");
        globalService.setName("WMS");
        globalService.setWorkspace(null);
        testRemoteServiceInfoRemoveEvent(globalService);
    }

    public @Test void testRemoteServiceInfoRemoveEvent_workspace_service() {
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
        RemoteServiceInfoRemoveEvent event =
                publish(
                        RemoteServiceInfoRemoveEvent.class,
                        geoServer,
                        service,
                        originService,
                        destinationService);

        Mockito.verify(this.evictor, times(1)).onServiceInfoRemoveEvent(same(event));

        assertNull("service by id not evicted", configCache.get(idKey));
        assertNull("service by name not evicted", configCache.get(nameKey));
        assertNull("service by type not evicted", configCache.get(typeKey));
    }

    public @Test void testRemoteSettingsInfoModifyEvent() {
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
        RemoteSettingsInfoModifyEvent event =
                publish(
                        RemoteSettingsInfoModifyEvent.class,
                        geoServer,
                        settings,
                        patch("charset", "ISO-8859-1"),
                        originService,
                        destinationService);

        assertEquals(ws.getId(), event.getWorkspaceId());

        Mockito.verify(evictor, times(1)).onSettingsInfoModifyEvent(same(event));

        assertNull("expected entry to be evicted", configCache.get(settings.getId()));
        assertNull(
                "expected workspace settings entry to be evicted",
                configCache.get(workspaceSettingsKey));
    }

    public @Test void testRemoteSettingsInfoRemoveEvent() {
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
        RemoteSettingsInfoRemoveEvent event =
                publish(
                        RemoteSettingsInfoRemoveEvent.class,
                        geoServer,
                        settings,
                        originService,
                        destinationService);

        assertEquals(ws.getId(), event.getWorkspaceId());

        Mockito.verify(evictor, times(1)).onSettingsInfoRemoveEvent(same(event));

        assertNull("expected entry to be evicted", configCache.get(settings.getId()));
        assertNull(
                "expected workspace settings entry to be evicted",
                configCache.get(workspaceSettingsKey));
    }

    public @Test void testRemoteLoggingInfoSetEvent() {
        LoggingInfo info = geoServer.getLogging();
        assertNotNull(info);
        final String key = CachingGeoServerFacade.LOGGINGINFO_KEY;
        assertNotNull(configCache.get(key));

        LoggingInfoImpl newLogging = new LoggingInfoImpl();

        Mockito.clearInvocations(this.evictor);

        RemoteLoggingInfoSetEvent event =
                publish(
                        RemoteLoggingInfoSetEvent.class,
                        this.geoServer,
                        newLogging,
                        originService,
                        destinationService);
        Mockito.verify(evictor, times(1)).onSetLoggingInfoEvent(same(event));

        assertNull("logging not evicted", configCache.get(key));
    }

    public @Test void testRemoteLoggingInfoModifyEvent() {
        LoggingInfo info = geoServer.getLogging();
        assertNotNull(info);
        final String key = CachingGeoServerFacade.LOGGINGINFO_KEY;
        assertNotNull(configCache.get(key));

        Mockito.clearInvocations(this.evictor);

        RemoteLoggingInfoModifyEvent event =
                publish(
                        RemoteLoggingInfoModifyEvent.class,
                        this.geoServer,
                        info,
                        patch("level", "DEBUG"),
                        originService,
                        destinationService);
        Mockito.verify(evictor, times(1)).onLoggingInfoModifyEvent(same(event));

        assertNull("logging not evicted", configCache.get(key));
    }

    public @Test void testRemoteGeoServerInfoSetEvent() {
        GeoServerInfo info = geoServer.getGlobal();
        assertNotNull(info);
        final String key = CachingGeoServerFacade.GEOSERVERINFO_KEY;
        assertNotNull(configCache.get(key));

        GeoServerInfo newGlobal = new GeoServerInfoImpl();

        Mockito.clearInvocations(this.evictor);
        // GeoServer source, Info object, String originService, String destinationService
        RemoteGeoServerInfoSetEvent event =
                publish(
                        RemoteGeoServerInfoSetEvent.class,
                        this.geoServer,
                        newGlobal,
                        originService,
                        destinationService);
        Mockito.verify(evictor, times(1)).onSetGlobalInfoEvent(same(event));

        assertNull("global not evicted", configCache.get(key));
    }

    public @Test void testRemoteGeoServerInfoModifyEvent() {
        GeoServerInfo info = geoServer.getGlobal();
        assertNotNull(info);
        final String key = CachingGeoServerFacade.GEOSERVERINFO_KEY;
        assertNotNull(configCache.get(key));

        Mockito.clearInvocations(this.evictor);
        // GeoServer source, GeoServerInfo object, Patch patch, originService,destinationService
        RemoteGeoServerInfoModifyEvent event =
                publish(
                        RemoteGeoServerInfoModifyEvent.class,
                        this.geoServer,
                        info,
                        patch("xmlExternalEntitiesEnabled", true),
                        originService,
                        destinationService);
        Mockito.verify(evictor, times(1)).onGeoServerInfoModifyEvent(same(event));

        assertNull("global not evicted", configCache.get(key));
    }

    /**
     * {@link GeoServerInfo#getUpdateSequence() update sequence} events are triggered upon each
     * configuration and catalog modification just for the sake of incrementing the update sequence.
     *
     * <p>{@link RemoteGeoServerInfoModifyEvent#isUpdateSequenceEvent()} is meant to be checked by
     * {@link RemoteEventCacheEvictor} and set the updated sequence number to the cached object
     * instead of evicting it.
     */
    public @Test void
            testRemoteGeoServerInfoModifyEvent_do_not_evict_and_apply_update_sequence_in_place() {
        GeoServerInfo local = geoServer.getGlobal();
        assertNotNull(local);
        final String key = CachingGeoServerFacade.GEOSERVERINFO_KEY;

        assertNotNull(configCache.get(key));

        GeoServerInfoImpl remote = new GeoServerInfoImpl();
        local.setUpdateSequence(999L);
        remote.setUpdateSequence(1000L);

        // Constructor args:
        // GeoServer source,GeoServerInfo object,Patch patch,String originService,String
        // destinationService
        Mockito.clearInvocations(this.evictor);
        RemoteGeoServerInfoModifyEvent event =
                publish(
                        RemoteGeoServerInfoModifyEvent.class,
                        geoServer,
                        remote,
                        patch("updateSequence", 1000L),
                        originService,
                        destinationService);

        Mockito.verify(evictor, times(1)).onGeoServerInfoModifyEvent(same(event));

        assertNotNull(
                "updateSequence event shouldn't evict the global config", configCache.get(key));
        GeoServerInfo updateSequenceAppliedInPlace = (GeoServerInfo) configCache.get(key).get();
        assertSame(local, updateSequenceAppliedInPlace);
        assertEquals(
                "updateSequence expected to be updated in place",
                1000L,
                updateSequenceAppliedInPlace.getUpdateSequence());
    }
}
