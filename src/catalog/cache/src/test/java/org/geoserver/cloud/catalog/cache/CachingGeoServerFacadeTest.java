package org.geoserver.cloud.catalog.cache;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.cloud.autoconfigure.catalog.event.LocalCatalogEventsAutoConfiguration;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.plugin.GeoServerImpl;
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

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

@SpringBootTest(classes = GeoServerBackendCacheConfiguration.class)
@EnableAutoConfiguration(exclude = LocalCatalogEventsAutoConfiguration.class)
public class CachingGeoServerFacadeTest {

    private @MockBean @Qualifier("rawCatalog") CatalogPlugin rawCatalog;
    private @MockBean @Qualifier("geoServer") GeoServerImpl rawGeoServer;
    private @MockBean @Qualifier("catalogFacade") ExtendedCatalogFacade catalogFacade;

    private @MockBean @Qualifier("geoserverFacade") GeoServerFacade mock;
    private @Autowired @Qualifier("cachingGeoServerFacade") CachingGeoServerFacade caching;

    private @Autowired CacheManager cacheManager;
    private Cache cache;
    private GeoServerInfo global;
    private WorkspaceInfo ws;
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
        ws = stub(WorkspaceInfo.class);
        settings = stub(SettingsInfo.class);
        when(settings.getWorkspace()).thenReturn(ws);
        logging = stub(LoggingInfo.class);

        service1 = stub(TestService1.class, 1);
        service2 = stub(TestService2.class, 1);
        when(service1.getName()).thenReturn("service1");
        when(service2.getName()).thenReturn("service2");

        wsService1 = stub(TestService1.class, 2);
        wsService2 = stub(TestService2.class, 2);
        when(wsService1.getName()).thenReturn("service1");
        when(wsService2.getName()).thenReturn("service2");
        when(wsService1.getWorkspace()).thenReturn(ws);
        when(wsService2.getWorkspace()).thenReturn(ws);

        when(mock.getGlobal()).thenReturn(global);
        when(mock.getLogging()).thenReturn(logging);
        when(mock.getService(TestService1.class)).thenReturn(service1);
        when(mock.getService(TestService2.class)).thenReturn(service2);

        when(mock.getService(eq(service1.getId()), eq(ServiceInfo.class))).thenReturn(service1);
        when(mock.getService(eq(service1.getId()), eq(TestService1.class))).thenReturn(service1);
        when(mock.getService(eq(service2.getId()), eq(ServiceInfo.class))).thenReturn(service2);
        when(mock.getService(eq(service2.getId()), eq(TestService2.class))).thenReturn(service2);
        doReturn(Arrays.asList(service1, service2)).when(mock).getServices();

        when(mock.getService(same(ws), eq(TestService1.class))).thenReturn(wsService1);
        when(mock.getService(same(ws), eq(TestService2.class))).thenReturn(wsService2);
        doReturn(Arrays.asList(wsService1, wsService2)).when(mock).getServices(same(ws));

        when(mock.getServiceByName(eq(service1.getName()), eq(ServiceInfo.class)))
                .thenReturn(service1);
        when(mock.getServiceByName(eq(service1.getName()), eq(TestService1.class)))
                .thenReturn(service1);
        when(mock.getServiceByName(eq(service2.getName()), eq(ServiceInfo.class)))
                .thenReturn(service1);
        when(mock.getServiceByName(eq(service2.getName()), eq(TestService2.class)))
                .thenReturn(service2);

        when(mock.getServiceByName(eq(wsService1.getName()), same(ws), eq(ServiceInfo.class)))
                .thenReturn(wsService1);
        when(mock.getServiceByName(eq(wsService1.getName()), same(ws), eq(TestService1.class)))
                .thenReturn(wsService1);
        when(mock.getServiceByName(eq(wsService2.getName()), same(ws), eq(ServiceInfo.class)))
                .thenReturn(wsService1);
        when(mock.getServiceByName(eq(wsService2.getName()), same(ws), eq(TestService2.class)))
                .thenReturn(wsService2);

        when(mock.getSettings(same(ws))).thenReturn(settings);
        this.cache = cacheManager.getCache(CachingGeoServerFacade.CACHE_NAME);
        this.cache.clear();
    }

    public @Test void testGetGlobal() {
        assertSameTimesN(global, caching::getGlobal, 3);
        verify(mock, times(1)).getGlobal();
    }

    public @Test void testSetGlobal() {
        assertSameTimesN(global, caching::getGlobal, 3);
        assertNotNull(cache.get(CachingGeoServerFacade.GEOSERVERINFO_KEY));

        caching.setGlobal(global);
        assertNull(cache.get(CachingGeoServerFacade.GEOSERVERINFO_KEY));
    }

    public @Test void testSaveGeoServerInfo() {
        assertSameTimesN(global, caching::getGlobal, 3);
        assertNotNull(cache.get(CachingGeoServerFacade.GEOSERVERINFO_KEY));

        caching.save(global);
        assertNull(cache.get(CachingGeoServerFacade.GEOSERVERINFO_KEY));
    }

    public @Test void testGetSettings() {
        assertSameTimesN(settings, () -> caching.getSettings(ws), 3);
        verify(mock, times(1)).getSettings(same(ws));
        assertNotNull(cache.get(CachingGeoServerFacade.settingsKey(ws)));
    }

    public @Test void testSaveSettingsInfo() {
        assertSameTimesN(settings, () -> caching.getSettings(ws), 3);
        verify(mock, times(1)).getSettings(same(ws));
        assertNotNull(cache.get(CachingGeoServerFacade.settingsKey(ws)));

        caching.save(settings);
        assertNull(cache.get(CachingGeoServerFacade.settingsKey(ws)));
    }

    public @Test void testRemoveSettingsInfo() {
        assertSameTimesN(settings, () -> caching.getSettings(ws), 3);
        verify(mock, times(1)).getSettings(same(ws));
        assertNotNull(cache.get(CachingGeoServerFacade.settingsKey(ws)));

        caching.remove(settings);
        assertNull(cache.get(CachingGeoServerFacade.settingsKey(ws)));
    }

    public @Test void testGetLogging() {
        assertSameTimesN(logging, caching::getLogging, 3);
        verify(mock, times(1)).getLogging();
        assertNotNull(cache.get(CachingGeoServerFacade.LOGGINGINFO_KEY));
    }

    public @Test void testSetLogging() {
        assertSameTimesN(logging, caching::getLogging, 3);
        verify(mock, times(1)).getLogging();
        assertNotNull(cache.get(CachingGeoServerFacade.LOGGINGINFO_KEY));
        caching.setLogging(logging);
        assertNull(cache.get(CachingGeoServerFacade.LOGGINGINFO_KEY));
    }

    public @Test void testSaveLoggingInfo() {
        assertSameTimesN(logging, caching::getLogging, 3);
        verify(mock, times(1)).getLogging();
        assertNotNull(cache.get(CachingGeoServerFacade.LOGGINGINFO_KEY));
        caching.save(logging);
        assertNull(cache.get(CachingGeoServerFacade.LOGGINGINFO_KEY));
    }

    public @Test void testRemoveServiceInfo() {
        testEvictsServiceInfo(service1, () -> caching.remove(service1));

        assertNotNull(wsService1.getWorkspace(), "preflight check failure");

        testEvictsServiceInfo(wsService1, () -> caching.remove(wsService1));
    }

    public @Test void testSaveServiceInfo() {
        testEvictsServiceInfo(service1, () -> caching.save(service1));

        assertNotNull(wsService1.getWorkspace(), "preflight check failure");

        testEvictsServiceInfo(wsService1, () -> caching.save(wsService1));
    }

    private void testEvictsServiceInfo(ServiceInfo service, Runnable task) {
        cache.invalidate();

        ServiceInfoKey idKey = ServiceInfoKey.byId(service.getId());
        ServiceInfoKey nameKey = ServiceInfoKey.byName(service.getWorkspace(), service.getName());
        ServiceInfoKey typeKey = ServiceInfoKey.byType(service.getWorkspace(), service.getClass());

        CachingGeoServerFacadeImpl.cachePut(cache, service);
        assertNotNull(cache.get(idKey));
        assertNotNull(cache.get(nameKey));
        assertNotNull(cache.get(typeKey));

        task.run();

        assertNull(cache.get(idKey));
        assertNull(cache.get(nameKey));
        assertNull(cache.get(typeKey));
    }

    /** {@link GeoServerFacade#getService(Class)} */
    public @Test void testGetServiceByType() {
        ServiceInfo service = service1;
        ServiceInfoKey idKey = ServiceInfoKey.byId(service.getId());
        ServiceInfoKey nameKey = ServiceInfoKey.byName(service.getWorkspace(), service.getName());
        ServiceInfoKey typeKey = ServiceInfoKey.byType(service.getWorkspace(), service.getClass());

        assertNull(cache.get(idKey));
        assertNull(cache.get(nameKey));
        assertNull(cache.get(typeKey));

        assertSameTimesN(service, () -> caching.getService(TestService1.class), 3);
        verify(mock, times(1)).getService(eq(TestService1.class));

        assertNotNull(cache.get(idKey));
        assertNotNull(cache.get(nameKey));
        assertNotNull(cache.get(typeKey));
    }

    /**
     * {@link GeoServerFacade#getService(WorkspaceInfo, Class)
     */
    public @Test void testGetServiceByWorkspaceAndType() {
        TestService1 service = wsService1;
        WorkspaceInfo ws = service.getWorkspace();
        assertNotNull(ws, "preflight check failure");
        when(mock.getService(same(ws), eq(TestService1.class))).thenReturn(service);

        ServiceInfoKey idKey = ServiceInfoKey.byId(service.getId());
        ServiceInfoKey nameKey = ServiceInfoKey.byName(service.getWorkspace(), service.getName());
        ServiceInfoKey typeKey = ServiceInfoKey.byType(service.getWorkspace(), service.getClass());

        assertNull(cache.get(idKey));
        assertNull(cache.get(nameKey));
        assertNull(cache.get(typeKey));

        assertSameTimesN(service, () -> caching.getService(ws, TestService1.class), 3);
        verify(mock, times(1)).getService(same(ws), eq(TestService1.class));

        assertNotNull(cache.get(idKey));
        assertNotNull(cache.get(nameKey));
        assertNotNull(cache.get(typeKey));
    }

    /**
     * {@link GeoServerFacade#getService(String, Class)
     */
    public @Test void testGetServiceByIdAndType() {
        TestService1 service = service1;
        when(mock.getService(eq(service.getId()), eq(TestService1.class))).thenReturn(service);

        ServiceInfoKey idKey = ServiceInfoKey.byId(service.getId());
        ServiceInfoKey nameKey = ServiceInfoKey.byName(service.getWorkspace(), service.getName());
        ServiceInfoKey typeKey = ServiceInfoKey.byType(service.getWorkspace(), service.getClass());

        assertNull(cache.get(idKey));
        assertNull(cache.get(nameKey));
        assertNull(cache.get(typeKey));

        assertSameTimesN(service, () -> caching.getService(service.getId(), TestService1.class), 3);
        verify(mock, times(1)).getService(eq(service.getId()), eq(TestService1.class));

        assertNotNull(cache.get(idKey));
        assertNotNull(cache.get(nameKey));
        assertNotNull(cache.get(typeKey));
    }

    public @Test void testGetServiceByNameAndType() {
        TestService1 service = service1;
        String name = service.getName();
        assertNotNull("preflight check failure", name);
        when(mock.getServiceByName(eq(name), eq(TestService1.class))).thenReturn(service);

        ServiceInfoKey idKey = ServiceInfoKey.byId(service.getId());
        ServiceInfoKey nameKey = ServiceInfoKey.byName(service.getWorkspace(), service.getName());
        ServiceInfoKey typeKey = ServiceInfoKey.byType(service.getWorkspace(), service.getClass());

        assertNull(cache.get(idKey));
        assertNull(cache.get(nameKey));
        assertNull(cache.get(typeKey));

        assertSameTimesN(service, () -> caching.getServiceByName(name, TestService1.class), 3);
        verify(mock, times(1)).getServiceByName(eq(name), eq(TestService1.class));

        assertNotNull(cache.get(idKey));
        assertNotNull(cache.get(nameKey));
        assertNotNull(cache.get(typeKey));
    }

    public @Test void testGetServiceByNameAndWorkspaceAndType() {
        TestService1 service = wsService1;
        String name = service.getName();
        WorkspaceInfo ws = service.getWorkspace();
        assertNotNull(name, "preflight check failure");
        assertNotNull(ws, "preflight check failure");
        when(mock.getServiceByName(eq(name), eq(ws), eq(TestService1.class))).thenReturn(service);

        ServiceInfoKey idKey = ServiceInfoKey.byId(service.getId());
        ServiceInfoKey nameKey = ServiceInfoKey.byName(service.getWorkspace(), service.getName());
        ServiceInfoKey typeKey = ServiceInfoKey.byType(service.getWorkspace(), service.getClass());

        assertNull(cache.get(idKey));
        assertNull(cache.get(nameKey));
        assertNull(cache.get(typeKey));

        assertSameTimesN(service, () -> caching.getServiceByName(name, ws, TestService1.class), 3);
        verify(mock, times(1)).getServiceByName(eq(name), eq(ws), eq(TestService1.class));

        assertNotNull(cache.get(idKey));
        assertNotNull(cache.get(nameKey));
        assertNotNull(cache.get(typeKey));
    }

    /** {@link CachingGeoServerFacade#evict(Info)} manual eviction aid */
    public @Test void testEvict_GeoServerInfo() {
        GeoServerInfo gsProxy = ResolvingProxy.create("someid", GeoServerInfo.class);
        assertFalse(caching.evict(gsProxy));
        cache.put(CachingGeoServerFacade.GEOSERVERINFO_KEY, global);
        assertTrue(caching.evict(gsProxy));
        assertFalse(caching.evict(gsProxy));
        assertNull(cache.get(CachingGeoServerFacade.GEOSERVERINFO_KEY));
    }

    /** {@link CachingGeoServerFacade#evict(Info)} manual eviction aid */
    public @Test void testEvict_LoggingInfo() {
        LoggingInfo loggingProxy = ResolvingProxy.create(settings.getId(), LoggingInfo.class);
        assertFalse(caching.evict(loggingProxy));
        cache.put(CachingGeoServerFacade.LOGGINGINFO_KEY, logging);
        assertTrue(caching.evict(loggingProxy));
        assertFalse(caching.evict(loggingProxy));
        assertNull(cache.get(CachingGeoServerFacade.LOGGINGINFO_KEY));
    }

    /** {@link CachingGeoServerFacade#evict(Info)} manual eviction aid */
    public @Test void testEvict_SettingsInfo() {
        assertNotNull(settings.getWorkspace());
        Object wsKey = CachingGeoServerFacade.settingsKey(settings.getWorkspace());
        final String id = settings.getId();

        SettingsInfo settingsProxy = ResolvingProxy.create(id, SettingsInfo.class);
        assertFalse(caching.evict(settingsProxy));

        cache.put(id, settings);
        cache.put(wsKey, settings);

        assertNotNull(cache.get(id));
        assertNotNull(cache.get(wsKey));

        assertTrue(caching.evict(settingsProxy));
        assertFalse(caching.evict(settingsProxy));

        assertNull(cache.get(id));
        assertNull(cache.get(wsKey));
    }

    /** {@link CachingGeoServerFacade#evict(Info)} manual eviction aid */
    public @Test void testEvict_ServiceInfo() {
        TestService1 service = service1;
        ServiceInfoKey idKey = ServiceInfoKey.byId(service.getId());
        ServiceInfoKey nameKey = ServiceInfoKey.byName(service.getWorkspace(), service.getName());
        ServiceInfoKey typeKey = ServiceInfoKey.byType(service.getWorkspace(), service.getClass());

        ServiceInfo serviceProxy = ResolvingProxy.create(service.getId(), TestService1.class);
        assertFalse(caching.evict(serviceProxy));

        cache.put(idKey, service);
        cache.put(nameKey, service);
        cache.put(typeKey, service);

        assertTrue(caching.evict(serviceProxy));
        assertFalse(caching.evict(serviceProxy));

        assertNull(cache.get(idKey));
        assertNull(cache.get(nameKey));
        assertNull(cache.get(typeKey));
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
}
