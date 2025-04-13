/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.catalog.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.geoserver.cloud.catalog.cache.CachingGeoServerFacade.CACHE_NAME;
import static org.geoserver.cloud.catalog.cache.CachingGeoServerFacade.GEOSERVERINFO_KEY;
import static org.geoserver.cloud.catalog.cache.CachingGeoServerFacade.LOGGINGINFO_KEY;
import static org.geoserver.cloud.event.info.ConfigInfoType.GEOSERVER;
import static org.geoserver.cloud.event.info.ConfigInfoType.LOGGING;
import static org.geoserver.cloud.event.info.ConfigInfoType.NAMESPACE;
import static org.geoserver.cloud.event.info.ConfigInfoType.SERVICE;
import static org.geoserver.cloud.event.info.ConfigInfoType.SETTINGS;
import static org.geoserver.cloud.event.info.ConfigInfoType.WORKSPACE;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.cloud.autoconfigure.catalog.event.LocalCatalogEventsAutoConfiguration;
import org.geoserver.cloud.event.UpdateSequenceEvent;
import org.geoserver.cloud.event.catalog.CatalogInfoAdded;
import org.geoserver.cloud.event.catalog.CatalogInfoModified;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.cloud.event.config.GeoServerInfoModified;
import org.geoserver.cloud.event.config.GeoServerInfoSet;
import org.geoserver.cloud.event.config.LoggingInfoModified;
import org.geoserver.cloud.event.config.LoggingInfoSet;
import org.geoserver.cloud.event.config.ServiceModified;
import org.geoserver.cloud.event.config.ServiceRemoved;
import org.geoserver.cloud.event.config.SettingsModified;
import org.geoserver.cloud.event.config.SettingsRemoved;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.cloud.event.security.SecurityConfigChanged;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.wms.WMSInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@SpringBootTest(classes = GeoServerBackendCacheConfiguration.class)
@EnableAutoConfiguration(exclude = LocalCatalogEventsAutoConfiguration.class)
class CachingGeoServerFacadeTest {

    private @MockBean @Qualifier("defaultUpdateSequence") UpdateSequence updateSequence;
    private @MockBean @Qualifier("rawCatalog") CatalogPlugin rawCatalog;
    private @MockBean @Qualifier("geoServer") GeoServerImpl rawGeoServer;
    private @MockBean @Qualifier("catalogFacade") ExtendedCatalogFacade catalogFacade;

    private @MockBean @Qualifier("geoserverFacade") GeoServerFacade mock;
    private @Autowired @Qualifier("cachingGeoServerFacade") CachingGeoServerFacade caching;

    private @Autowired CacheManager cacheManager;
    private Cache cache;
    private GeoServerInfo global;
    private WorkspaceInfo workspace;
    private SettingsInfo settings;
    private TestService1 service1;
    private TestService2 service2;
    private TestService1 wsService1;
    private TestService2 wsService2;
    private LoggingInfo logging;

    public static interface TestService1 extends ServiceInfo {}

    public static interface TestService2 extends ServiceInfo {}

    public @BeforeEach void before() {
        global = stub(GeoServerInfo.class);
        workspace = stub(WorkspaceInfo.class);
        settings = stub(SettingsInfo.class);
        when(settings.getWorkspace()).thenReturn(workspace);
        logging = stub(LoggingInfo.class);

        service1 = stub(TestService1.class, 1);
        service2 = stub(TestService2.class, 1);
        when(service1.getName()).thenReturn("service1");
        when(service2.getName()).thenReturn("service2");

        wsService1 = stub(TestService1.class, 2);
        wsService2 = stub(TestService2.class, 2);
        when(wsService1.getName()).thenReturn("service1");
        when(wsService2.getName()).thenReturn("service2");
        when(wsService1.getWorkspace()).thenReturn(workspace);
        when(wsService2.getWorkspace()).thenReturn(workspace);

        when(mock.getGlobal()).thenReturn(global);
        when(mock.getLogging()).thenReturn(logging);
        when(mock.getService(TestService1.class)).thenReturn(service1);
        when(mock.getService(TestService2.class)).thenReturn(service2);

        when(mock.getService(service1.getId(), ServiceInfo.class)).thenReturn(service1);
        when(mock.getService(service1.getId(), TestService1.class)).thenReturn(service1);
        when(mock.getService(service2.getId(), ServiceInfo.class)).thenReturn(service2);
        when(mock.getService(service2.getId(), TestService2.class)).thenReturn(service2);
        doReturn(Arrays.asList(service1, service2)).when(mock).getServices();

        when(mock.getService(workspace, TestService1.class)).thenReturn(wsService1);
        when(mock.getService(workspace, TestService2.class)).thenReturn(wsService2);
        doReturn(Arrays.asList(wsService1, wsService2)).when(mock).getServices(workspace);

        when(mock.getServiceByName(service1.getName(), ServiceInfo.class)).thenReturn(service1);
        when(mock.getServiceByName(service1.getName(), TestService1.class)).thenReturn(service1);
        when(mock.getServiceByName(service2.getName(), ServiceInfo.class)).thenReturn(service1);
        when(mock.getServiceByName(service2.getName(), TestService2.class)).thenReturn(service2);

        when(mock.getServiceByName(wsService1.getName(), workspace, ServiceInfo.class))
                .thenReturn(wsService1);
        when(mock.getServiceByName(wsService1.getName(), workspace, TestService1.class))
                .thenReturn(wsService1);
        when(mock.getServiceByName(wsService2.getName(), workspace, ServiceInfo.class))
                .thenReturn(wsService1);
        when(mock.getServiceByName(wsService2.getName(), workspace, TestService2.class))
                .thenReturn(wsService2);

        when(mock.getSettings(workspace)).thenReturn(settings);
        this.cache = cacheManager.getCache(CACHE_NAME);
        this.cache.clear();
    }

    @Test
    void onUpdateSequenceEvent() {
        testUpdateSequenceEvent(UpdateSequenceEvent.class);
        testUpdateSequenceEvent(SecurityConfigChanged.class);
        testUpdateSequenceEvent(CatalogInfoAdded.class);
        testUpdateSequenceEvent(CatalogInfoRemoved.class);
        testUpdateSequenceEvent(CatalogInfoModified.class);

        testUpdateSequenceEvent(ServiceModified.class);
        testUpdateSequenceEvent(ServiceRemoved.class);
        testUpdateSequenceEvent(SettingsRemoved.class);

        testUpdateSequenceEvent(SettingsModified.class);
        testUpdateSequenceEvent(SettingsRemoved.class);
        testUpdateSequenceEvent(SettingsRemoved.class);
    }

    public <E extends UpdateSequenceEvent> void testUpdateSequenceEvent(Class<E> eventType) {
        E event = mock(eventType);
        when(event.getUpdateSequence()).thenReturn(1000L);
        when(event.isRemote()).thenReturn(true);
        when(this.global.getUpdateSequence()).thenReturn(999L);

        assertSameTimesN(global, caching::getGlobal, 3);
        assertThat(cache.get(GEOSERVERINFO_KEY)).isNotNull();

        caching.onUpdateSequenceEvent(event);
        assertThat(cache.get(GEOSERVERINFO_KEY)).isNull();
    }

    @Test
    void onGeoServerInfoModifyEvent() {
        caching.getGlobal();
        assertThat(cache.get(GEOSERVERINFO_KEY)).isNotNull();

        GeoServerInfoModified event = event(GeoServerInfoModified.class, "global", GEOSERVER);
        caching.onUpdateSequenceEvent(event);
        assertThat(cache.get(GEOSERVERINFO_KEY)).isNull();
    }

    @Test
    void onGeoServerInfoSetEvent() {
        caching.getGlobal();
        assertThat(cache.get(GEOSERVERINFO_KEY)).isNotNull();

        GeoServerInfoSet event = event(GeoServerInfoSet.class, "global", GEOSERVER);
        caching.onUpdateSequenceEvent(event);
        assertThat(cache.get(GEOSERVERINFO_KEY)).isNull();
    }

    @Test
    void onLoggingInfoModifyEvent() {
        caching.getLogging();
        assertThat(cache.get(LOGGINGINFO_KEY)).isNotNull();

        LoggingInfoModified event = event(LoggingInfoModified.class, "logging", LOGGING);
        caching.onUpdateSequenceEvent(event);
        assertThat(cache.get(LOGGINGINFO_KEY)).isNull();
    }

    @Test
    void onLoggingInfoSetEvent() {
        caching.getLogging();
        assertThat(cache.get(LOGGINGINFO_KEY)).isNotNull();

        LoggingInfoSet event = event(LoggingInfoSet.class, "logging", LOGGING);
        caching.onUpdateSequenceEvent(event);
        assertThat(cache.get(LOGGINGINFO_KEY)).isNull();
    }

    @Test
    void onSettingsInfoModifyEvent() {
        caching.getSettings(workspace);
        final Object idKey = settings.getId();
        final Object wsKey = CachingGeoServerFacade.settingsKey(workspace);
        assertThat(cache.get(idKey)).isNotNull();
        assertThat(cache.get(wsKey)).isNotNull();

        SettingsModified event = event(SettingsModified.class, settings.getId(), SETTINGS);
        final String wsid = workspace.getId();
        when(event.getWorkspaceId()).thenReturn(wsid);

        caching.onUpdateSequenceEvent(event);
        assertThat(cache.get(idKey)).isNull();
        assertThat(cache.get(wsKey)).isNull();
    }

    @Test
    void onSettingsInfoRemoveEvent() {
        caching.getSettings(workspace);
        final Object idKey = settings.getId();
        final Object wsKey = CachingGeoServerFacade.settingsKey(workspace);
        assertThat(cache.get(idKey)).isNotNull();
        assertThat(cache.get(wsKey)).isNotNull();

        SettingsRemoved event = event(SettingsRemoved.class, settings.getId(), SETTINGS);
        final String wsid = workspace.getId();
        when(event.getWorkspaceId()).thenReturn(wsid);

        caching.onUpdateSequenceEvent(event);
        assertThat(cache.get(idKey)).isNull();
        assertThat(cache.get(wsKey)).isNull();
    }

    @Test
    void onServiceInfoModifyEvent() {
        TestService1 service = wsService1;
        WorkspaceInfo ws1 = service.getWorkspace();
        when(mock.getService(ws1, ServiceInfo.class)).thenReturn(service);
        when(mock.getService(ws1, TestService1.class)).thenReturn(service);

        ServiceInfoKey idKey = ServiceInfoKey.byId(service.getId());
        ServiceInfoKey nameKey = ServiceInfoKey.byName(service.getWorkspace(), service.getName());
        ServiceInfoKey typeKey = ServiceInfoKey.byType(service.getWorkspace(), service.getClass());

        final ServiceModified event = event(ServiceModified.class, service.getId(), SERVICE);

        // query as ServiceInfo.class
        caching.getService(ws1, ServiceInfo.class);
        assertThat(cache.get(idKey)).isNotNull();
        assertThat(cache.get(nameKey)).isNotNull();
        assertThat(cache.get(typeKey)).isNotNull();

        caching.onUpdateSequenceEvent(event);

        assertThat(cache.get(idKey)).isNull();
        assertThat(cache.get(nameKey)).isNull();
        assertThat(cache.get(typeKey)).isNull();

        // query as TestService1.class
        caching.getService(ws1, TestService1.class);
        assertThat(cache.get(idKey)).isNotNull();
        assertThat(cache.get(nameKey)).isNotNull();
        assertThat(cache.get(typeKey)).isNotNull();

        caching.onUpdateSequenceEvent(event);

        assertThat(cache.get(idKey)).isNull();
        assertThat(cache.get(nameKey)).isNull();
        assertThat(cache.get(typeKey)).isNull();

        // query as WMSInfo.class
        caching.getService(ws1, WMSInfo.class);
        assertThat(cache.get(idKey)).isNull();
        assertThat(cache.get(nameKey)).isNull();
        assertThat(cache.get(typeKey)).isNull();
    }

    @Test
    void onServiceInfoRemoveEvent() {
        TestService1 service = wsService1;
        WorkspaceInfo ws = service.getWorkspace();
        when(mock.getService(ws, ServiceInfo.class)).thenReturn(service);
        when(mock.getService(ws, TestService1.class)).thenReturn(service);

        ServiceInfoKey idKey = ServiceInfoKey.byId(service.getId());
        ServiceInfoKey nameKey = ServiceInfoKey.byName(service.getWorkspace(), service.getName());
        ServiceInfoKey typeKey = ServiceInfoKey.byType(service.getWorkspace(), service.getClass());

        final ServiceRemoved event = event(ServiceRemoved.class, service.getId(), SERVICE);

        caching.getService(ws, ServiceInfo.class);
        assertThat(cache.get(idKey)).isNotNull();
        assertThat(cache.get(nameKey)).isNotNull();
        assertThat(cache.get(typeKey)).isNotNull();

        caching.onUpdateSequenceEvent(event);

        assertThat(cache.get(idKey)).isNull();
        assertThat(cache.get(nameKey)).isNull();
        assertThat(cache.get(typeKey)).isNull();
    }

    @Test
    void onWorkspaceRemoved() {
        TestService1 service = wsService1;
        WorkspaceInfo ws = service.getWorkspace();
        when(mock.getService(ws, ServiceInfo.class)).thenReturn(service);

        caching.getGlobal();
        caching.getLogging();
        caching.getSettings(ws);
        caching.getService(ws, ServiceInfo.class);

        com.github.benmanes.caffeine.cache.Cache<?, ?> nativeCache =
                (com.github.benmanes.caffeine.cache.Cache<?, ?>) cache.getNativeCache();

        long initialSize = nativeCache.estimatedSize();
        assertThat(initialSize).isGreaterThan(3);

        // not a workspace removed event, shall not
        CatalogInfoRemoved event = event(CatalogInfoRemoved.class, "fakeid", NAMESPACE);
        caching.onUpdateSequenceEvent(event);

        // this listener shall work with both local and remote events
        event = event(CatalogInfoRemoved.class, ws.getId(), WORKSPACE);
        when(event.isRemote()).thenReturn(false);
        caching.onUpdateSequenceEvent(event);
        await().atMost(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(nativeCache.asMap()).isEmpty());

        caching.getGlobal();
        caching.getLogging();
        caching.getSettings(ws);
        caching.getService(ws, ServiceInfo.class);
        assertThat(nativeCache.estimatedSize()).isGreaterThan(3);
        when(event.isRemote()).thenReturn(true);
        caching.onUpdateSequenceEvent(event);
        await().atMost(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(nativeCache.asMap()).isEmpty());
    }

    @Test
    void testGetGlobal() {
        assertSameTimesN(global, caching::getGlobal, 3);
        verify(mock, times(1)).getGlobal();
    }

    @Test
    void testSetGlobal() {
        assertSameTimesN(global, caching::getGlobal, 3);
        assertNotNull(cache.get(GEOSERVERINFO_KEY));

        caching.setGlobal(global);
        assertNull(cache.get(GEOSERVERINFO_KEY));
    }

    @Test
    void testSaveGeoServerInfo() {
        assertSameTimesN(global, caching::getGlobal, 3);
        assertNotNull(cache.get(GEOSERVERINFO_KEY));

        caching.save(global);
        assertNull(cache.get(GEOSERVERINFO_KEY));
    }

    @Test
    void testGetSettings() {
        assertSameTimesN(settings, () -> caching.getSettings(workspace), 3);
        verify(mock, times(1)).getSettings(workspace);
        assertNotNull(cache.get(CachingGeoServerFacade.settingsKey(workspace)));
    }

    @Test
    void testSaveSettingsInfo() {
        assertSameTimesN(settings, () -> caching.getSettings(workspace), 3);
        verify(mock, times(1)).getSettings(workspace);
        assertNotNull(cache.get(CachingGeoServerFacade.settingsKey(workspace)));

        caching.save(settings);
        assertNull(cache.get(CachingGeoServerFacade.settingsKey(workspace)));
    }

    @Test
    void testRemoveSettingsInfo() {
        assertSameTimesN(settings, () -> caching.getSettings(workspace), 3);
        verify(mock, times(1)).getSettings(workspace);
        assertNotNull(cache.get(CachingGeoServerFacade.settingsKey(workspace)));

        caching.remove(settings);
        assertNull(cache.get(CachingGeoServerFacade.settingsKey(workspace)));
    }

    @Test
    void testGetLogging() {
        assertSameTimesN(logging, caching::getLogging, 3);
        verify(mock, times(1)).getLogging();
        assertNotNull(cache.get(LOGGINGINFO_KEY));
    }

    @Test
    void testSetLogging() {
        assertSameTimesN(logging, caching::getLogging, 3);
        verify(mock, times(1)).getLogging();
        assertNotNull(cache.get(LOGGINGINFO_KEY));
        caching.setLogging(logging);
        assertNull(cache.get(LOGGINGINFO_KEY));
    }

    @Test
    void testSaveLoggingInfo() {
        assertSameTimesN(logging, caching::getLogging, 3);
        verify(mock, times(1)).getLogging();
        assertNotNull(cache.get(LOGGINGINFO_KEY));
        caching.save(logging);
        assertNull(cache.get(LOGGINGINFO_KEY));
    }

    @Test
    void testRemoveServiceInfo() {
        testEvictsServiceInfo(service1, () -> caching.remove(service1));

        assertNotNull(wsService1.getWorkspace(), "preflight check failure");

        testEvictsServiceInfo(wsService1, () -> caching.remove(wsService1));
    }

    @Test
    void testSaveServiceInfo() {
        testEvictsServiceInfo(service1, () -> caching.save(service1));

        assertNotNull(wsService1.getWorkspace(), "preflight check failure");

        testEvictsServiceInfo(wsService1, () -> caching.save(wsService1));
    }

    private void testEvictsServiceInfo(ServiceInfo service, Runnable task) {
        cache.invalidate();

        ServiceInfoKey idKey = ServiceInfoKey.byId(service.getId());
        ServiceInfoKey nameKey = ServiceInfoKey.byName(service.getWorkspace(), service.getName());
        ServiceInfoKey typeKey = ServiceInfoKey.byType(service.getWorkspace(), service.getClass());

        caching.cachePut(cache, service);
        assertNotNull(cache.get(idKey));
        assertNotNull(cache.get(nameKey));
        assertNotNull(cache.get(typeKey));

        task.run();

        assertNull(cache.get(idKey));
        assertNull(cache.get(nameKey));
        assertNull(cache.get(typeKey));
    }

    /** {@link GeoServerFacade#getService(Class)} */
    @Test
    void testGetServiceByType() {
        ServiceInfo service = service1;
        ServiceInfoKey idKey = ServiceInfoKey.byId(service.getId());
        ServiceInfoKey nameKey = ServiceInfoKey.byName(service.getWorkspace(), service.getName());
        ServiceInfoKey typeKey = ServiceInfoKey.byType(service.getWorkspace(), service.getClass());

        assertNull(cache.get(idKey));
        assertNull(cache.get(nameKey));
        assertNull(cache.get(typeKey));

        assertSameTimesN(service, () -> caching.getService(TestService1.class), 3);
        verify(mock, times(1)).getService(TestService1.class);

        assertNotNull(cache.get(idKey));
        assertNotNull(cache.get(nameKey));
        assertNotNull(cache.get(typeKey));
    }

    /**
     * {@link GeoServerFacade#getService(WorkspaceInfo, Class)
     */
    @Test
    void testGetServiceByWorkspaceAndType() {
        TestService1 service = wsService1;
        WorkspaceInfo ws = service.getWorkspace();
        assertNotNull(ws, "preflight check failure");
        when(mock.getService(ws, TestService1.class)).thenReturn(service);

        ServiceInfoKey idKey = ServiceInfoKey.byId(service.getId());
        ServiceInfoKey nameKey = ServiceInfoKey.byName(service.getWorkspace(), service.getName());
        ServiceInfoKey typeKey = ServiceInfoKey.byType(service.getWorkspace(), service.getClass());

        assertNull(cache.get(idKey));
        assertNull(cache.get(nameKey));
        assertNull(cache.get(typeKey));

        assertSameTimesN(service, () -> caching.getService(ws, TestService1.class), 3);
        verify(mock, times(1)).getService(ws, TestService1.class);

        assertNotNull(cache.get(idKey));
        assertNotNull(cache.get(nameKey));
        assertNotNull(cache.get(typeKey));
    }

    /**
     * {@link GeoServerFacade#getService(String, Class)
     */
    @Test
    void testGetServiceByIdAndType() {
        TestService1 service = service1;
        when(mock.getService(service.getId(), TestService1.class)).thenReturn(service);

        ServiceInfoKey idKey = ServiceInfoKey.byId(service.getId());
        ServiceInfoKey nameKey = ServiceInfoKey.byName(service.getWorkspace(), service.getName());
        ServiceInfoKey typeKey = ServiceInfoKey.byType(service.getWorkspace(), service.getClass());

        assertNull(cache.get(idKey));
        assertNull(cache.get(nameKey));
        assertNull(cache.get(typeKey));

        assertSameTimesN(service, () -> caching.getService(service.getId(), TestService1.class), 3);
        verify(mock, times(1)).getService(service.getId(), TestService1.class);

        assertNotNull(cache.get(idKey));
        assertNotNull(cache.get(nameKey));
        assertNotNull(cache.get(typeKey));
    }

    @Test
    void testGetServiceByNameAndType() {
        TestService1 service = service1;
        String name = service.getName();
        assertNotNull("preflight check failure", name);
        when(mock.getServiceByName(name, TestService1.class)).thenReturn(service);

        ServiceInfoKey idKey = ServiceInfoKey.byId(service.getId());
        ServiceInfoKey nameKey = ServiceInfoKey.byName(service.getWorkspace(), service.getName());
        ServiceInfoKey typeKey = ServiceInfoKey.byType(service.getWorkspace(), service.getClass());

        assertNull(cache.get(idKey));
        assertNull(cache.get(nameKey));
        assertNull(cache.get(typeKey));

        assertSameTimesN(service, () -> caching.getServiceByName(name, TestService1.class), 3);
        verify(mock, times(1)).getServiceByName(name, TestService1.class);

        assertNotNull(cache.get(idKey));
        assertNotNull(cache.get(nameKey));
        assertNotNull(cache.get(typeKey));
    }

    @Test
    void testGetServiceByNameAndWorkspaceAndType() {
        TestService1 service = wsService1;
        String name = service.getName();
        WorkspaceInfo ws = service.getWorkspace();
        assertNotNull(name, "preflight check failure");
        assertNotNull(ws, "preflight check failure");
        when(mock.getServiceByName(name, ws, TestService1.class)).thenReturn(service);

        ServiceInfoKey idKey = ServiceInfoKey.byId(service.getId());
        ServiceInfoKey nameKey = ServiceInfoKey.byName(service.getWorkspace(), service.getName());
        ServiceInfoKey typeKey = ServiceInfoKey.byType(service.getWorkspace(), service.getClass());

        assertNull(cache.get(idKey));
        assertNull(cache.get(nameKey));
        assertNull(cache.get(typeKey));

        assertSameTimesN(service, () -> caching.getServiceByName(name, ws, TestService1.class), 3);
        verify(mock, times(1)).getServiceByName(name, ws, TestService1.class);

        assertNotNull(cache.get(idKey));
        assertNotNull(cache.get(nameKey));
        assertNotNull(cache.get(typeKey));
    }

    private <T extends Info> void assertSameTimesN(T info, Supplier<T> query, int times) {
        assertSameTimesN(info, id -> query.get(), times);
    }

    private <T extends Info> void assertSameTimesN(T info, Function<String, T> query, int times) {
        for (int i = 0; i < times; i++) {
            T result = query.apply(info.getId());
            assertSame(info, result);
        }
    }

    private <T extends Info> T stub(Class<T> type) {
        return stub(type, 1);
    }

    private <T extends Info> T stub(Class<T> type, int id) {
        T info = Mockito.mock(type);
        String sid = type.getSimpleName() + "." + id;
        when(info.getId()).thenReturn(sid);
        return info;
    }

    private <E extends InfoEvent> E event(Class<E> type, String id, ConfigInfoType objectType) {
        E event = mock(type);
        when(event.isRemote()).thenReturn(true);
        when(event.getObjectId()).thenReturn(id);
        when(event.getObjectType()).thenReturn(objectType);
        return event;
    }
}
