/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.cloud.event.PropertyDiff.Change;
import org.geoserver.cloud.event.catalog.LocalCatalogAddEvent;
import org.geoserver.cloud.event.catalog.LocalCatalogPostModifyEvent;
import org.geoserver.cloud.event.catalog.LocalCatalogPreModifyEvent;
import org.geoserver.cloud.event.catalog.LocalCatalogRemoveEvent;
import org.geoserver.cloud.event.config.LocalConfigAddEvent;
import org.geoserver.cloud.event.config.LocalConfigPostModifyEvent;
import org.geoserver.cloud.event.config.LocalConfigPreModifyEvent;
import org.geoserver.cloud.event.config.LocalConfigRemoveEvent;
import org.geoserver.cloud.test.ApplicationEventCapturingListener;
import org.geoserver.cloud.test.TestConfigurationAutoConfiguration;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.CoverageAccessInfo.QueueType;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.CoverageAccessInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.wms.WMSInfoImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(
    classes = {TestConfigurationAutoConfiguration.class, ApplicationEventCapturingListener.class}
)
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
public class LocalApplicationEventsAutoConfigurationTest {

    private @Autowired GeoServer geoserver;
    private @Autowired Catalog catalog;

    private @Autowired ApplicationEventCapturingListener listener;

    public @Rule CatalogTestData testData = CatalogTestData.empty(() -> catalog, () -> geoserver);

    public @Before void before() {
        listener.setCapureEventsOf(LocalInfoEvent.class);
        catalog.dispose();
        listener.clear();
    }

    public @Test void testCatalogEventBroadcasterHasSetUpItself() {
        Optional<CatalogListener> publiherListener =
                catalog.getListeners()
                        .stream()
                        .filter(
                                l ->
                                        l
                                                instanceof
                                                LocalApplicationEventPublisher
                                                        .LocalCatalogEventPublisher)
                        .findFirst();
        assertTrue(publiherListener.isPresent());
    }

    public @Test void testConfigEventBroadcasterHasSetUpItself() {
        Optional<ConfigurationListener> publiherListener =
                geoserver
                        .getListeners()
                        .stream()
                        .filter(
                                l ->
                                        l
                                                instanceof
                                                LocalApplicationEventPublisher
                                                        .LocalConfigEventPublisher)
                        .findFirst();
        assertTrue(publiherListener.isPresent());
    }

    public @Test void testCatalogSetDefaultWorkspace() {
        listener.stop();
        catalog.add(testData.workspaceA);
        catalog.add(testData.workspaceB);
        catalog.add(testData.workspaceC);
        listener.start();

        listener.clear();
        catalog.setDefaultWorkspace(testData.workspaceC);
        testPrePostModify(catalog, "defaultWorkspace", testData.workspaceA, testData.workspaceC);

        listener.clear();
        catalog.setDefaultWorkspace(testData.workspaceB);
        testPrePostModify(catalog, "defaultWorkspace", testData.workspaceC, testData.workspaceB);
    }

    public @Test void testCatalogSetDefaultNamespace() {
        listener.stop();
        catalog.add(testData.namespaceA);
        catalog.add(testData.namespaceB);
        catalog.add(testData.namespaceC);
        listener.start();

        listener.clear();
        catalog.setDefaultNamespace(testData.namespaceC);
        testPrePostModify(catalog, "defaultNamespace", testData.namespaceA, testData.namespaceC);

        listener.clear();
        catalog.setDefaultNamespace(testData.namespaceB);
        testPrePostModify(catalog, "defaultNamespace", testData.namespaceC, testData.namespaceB);
    }

    public @Test void testCatalogAddedEvents() {
        Class<LocalCatalogAddEvent> eventType = LocalCatalogAddEvent.class;
        testAddEvent(testData.workspaceA, catalog::add, eventType);
        testAddEvent(testData.namespaceA, catalog::add, eventType);
        testAddEvent(testData.coverageStoreA, catalog::add, eventType);
        testAddEvent(testData.dataStoreA, catalog::add, eventType);
        testAddEvent(testData.coverageA, catalog::add, eventType);
        testAddEvent(testData.featureTypeA, catalog::add, eventType);
        testAddEvent(testData.layerFeatureTypeA, catalog::add, eventType);
        testAddEvent(testData.layerGroup1, catalog::add, eventType);
        testAddEvent(testData.style1, catalog::add, eventType);
    }

    public @Test void testCatalogPrePostModifyEvents() {
        listener.stop();
        testData.addObjects();
        listener.start();

        Class<LocalCatalogPreModifyEvent> preEventType = LocalCatalogPreModifyEvent.class;
        Class<LocalCatalogPostModifyEvent> postEventType = LocalCatalogPostModifyEvent.class;
        testModify(
                testData.workspaceA,
                ws -> {
                    ws.setName("newName");
                    ws.setIsolated(true);
                },
                catalog::save,
                preEventType,
                postEventType);

        testModify(
                testData.coverageStoreA,
                cs -> {
                    cs.setName("newCoverageStoreName");
                    cs.setDescription("new description");
                    cs.setWorkspace(testData.workspaceC);
                },
                catalog::save,
                preEventType,
                postEventType);
    }

    public @Test void testCatalogRemoveEvents() {
        listener.stop();
        testData.addObjects();
        listener.start();

        testRemove(testData.layerGroup1, catalog::remove, LocalCatalogRemoveEvent.class);
    }

    public @Test void testConfigAddEvents() {
        catalog.add(testData.workspaceB);

        Class<LocalConfigAddEvent> eventType = LocalConfigAddEvent.class;

        WMSInfoImpl service = new WMSInfoImpl();
        service.setName("WMS");
        testAddEvent(service, geoserver::add, eventType);

        WMSInfoImpl workspaceService = new WMSInfoImpl();
        workspaceService.setName("WMS");
        workspaceService.setWorkspace(testData.workspaceB);
        testAddEvent(workspaceService, geoserver::add, eventType);

        SettingsInfoImpl workspaceSettings = new SettingsInfoImpl();
        workspaceSettings.setWorkspace(testData.workspaceB);
        testAddEvent(workspaceSettings, geoserver::add, eventType);
    }

    public @Test void testConfigPrePostModifyEvents_GeoServerInfo() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.workspaceB);

        GeoServerInfo global = geoserver.getGlobal();

        Class<LocalConfigPreModifyEvent> preEventType = LocalConfigPreModifyEvent.class;
        Class<LocalConfigPostModifyEvent> postEventType = LocalConfigPostModifyEvent.class;

        CoverageAccessInfo coverageInfo = new CoverageAccessInfoImpl();
        coverageInfo.setCorePoolSize(10);
        coverageInfo.setQueueType(QueueType.UNBOUNDED);

        testModify(
                global,
                gs -> {
                    gs.setAdminUsername("admin");
                    gs.setAdminPassword("secret");
                    gs.setCoverageAccess(coverageInfo);
                },
                geoserver::save,
                preEventType,
                postEventType);
    }

    public @Test void testConfigPrePostModifyEvents_SettingsInfo() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.workspaceB);

        Class<LocalConfigPreModifyEvent> preEventType = LocalConfigPreModifyEvent.class;
        Class<LocalConfigPostModifyEvent> postEventType = LocalConfigPostModifyEvent.class;

        // odd API weirdness here, can't modify global settings through GeoSever.save(SettingsInfo),
        // complains settings must be part of a workspace, although you can get the global settings
        // through GeoServer.getSettings();
        testModify(
                geoserver.getGlobal(),
                g -> {
                    g.getSettings().setCharset("ISO-8869-1");
                    g.getSettings().setProxyBaseUrl("http://test.com");
                },
                geoserver::save,
                preEventType,
                postEventType);

        SettingsInfo workspaceSettings = new SettingsInfoImpl();
        workspaceSettings.setWorkspace(testData.workspaceA);
        geoserver.add(workspaceSettings);
        workspaceSettings = geoserver.getSettings(testData.workspaceA);

        testModify(
                workspaceSettings,
                s -> {
                    s.setCharset("ISO-8869-1");
                    s.setWorkspace(testData.workspaceB);
                },
                geoserver::save,
                preEventType,
                postEventType);
    }

    public @Test void testConfigPrePostModifyEvents_LoggingInfo() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.workspaceB);

        Class<LocalConfigPreModifyEvent> preEventType = LocalConfigPreModifyEvent.class;
        Class<LocalConfigPostModifyEvent> postEventType = LocalConfigPostModifyEvent.class;

        LoggingInfo globalLogging = geoserver.getLogging();
        testModify(
                globalLogging,
                logging -> {
                    logging.setLevel("TRACE");
                    logging.setStdOutLogging(!logging.isStdOutLogging());
                },
                geoserver::save,
                preEventType,
                postEventType);
    }


    public @Test void testConfigRemoveEvents() {
        listener.stop();
        ServiceInfo service = new WMSInfoImpl();
        geoserver.add(service);
        listener.start();

        testRemove(service, geoserver::remove, LocalConfigRemoveEvent.class);
    }

    private <T extends Info> void testRemove(
            T info,
            Consumer<T> remover,
            @SuppressWarnings("rawtypes") Class<? extends LocalRemoveEvent> eventType) {
        listener.clear();
        listener.start();
        remover.accept(info);
        @SuppressWarnings("unchecked")
        LocalRemoveEvent<?, T> event = listener.expectOne(eventType);
        assertEquals(info, event.getObject());
    }

    @SuppressWarnings("unchecked")
    private <T extends Info> void testModify(
            T info,
            Consumer<T> modifier,
            Consumer<T> saver,
            @SuppressWarnings("rawtypes") Class<? extends LocalPreModifyEvent> preEventType,
            @SuppressWarnings("rawtypes") Class<? extends LocalPostModifyEvent> postEventType) {

        Class<T> type;
        ClassMappings classMappings =
                ClassMappings.fromImpl(ModificationProxy.unwrap(info).getClass());
        if (classMappings != null) {
            type = classMappings.getInterface();
        } else if (info instanceof GeoServerInfo) {
            type = (Class<T>) GeoServerInfo.class;
        } else if (info instanceof SettingsInfo) {
            type = (Class<T>) SettingsInfo.class;
        } else if (info instanceof LoggingInfo) {
            type = (Class<T>) LoggingInfo.class;
        } else {
            throw new IllegalArgumentException("uknown Info type: " + info);
        }
        T proxy = ModificationProxy.create(ModificationProxy.unwrap(info), type);
        modifier.accept(proxy);

        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(proxy);
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();
        assertFalse("Test should change at least one property", propertyNames.isEmpty());

        PropertyDiff expected = PropertyDiff.valueOf(propertyNames, oldValues, newValues);

        listener.clear();
        listener.start();
        saver.accept(proxy);

        LocalPreModifyEvent<?, T> pre = listener.expectOne(preEventType);
        testData.assertEqualsLenientConnectionParameters(info, pre.getObject());

        assertEquals(expected, pre.getDiff());

        LocalPostModifyEvent<?, T> post = listener.expectOne(postEventType);
        testData.assertEqualsLenientConnectionParameters(proxy, post.getObject());
        assertEquals(expected, post.getDiff());
    }

    private void testPrePostModify(
            CatalogInfo objectChanged,
            String propertyName,
            CatalogInfo oldValue,
            CatalogInfo newValue) {
        LocalCatalogPreModifyEvent pre = listener.expectOne(LocalCatalogPreModifyEvent.class);
        LocalCatalogPostModifyEvent post = listener.expectOne(LocalCatalogPostModifyEvent.class);

        assertSame(objectChanged, pre.getObject());
        assertSame(objectChanged, post.getObject());

        assertFalse(pre.getDiff().isEmpty());
        assertFalse(post.getDiff().isEmpty());
        assertTrue(pre.getDiff().get(propertyName).isPresent());

        Change preChange = pre.getDiff().get(propertyName).get();

        assertEquals(oldValue, preChange.getOldValue());
        assertEquals(newValue, preChange.getNewValue());

        assertTrue(post.getDiff().get(propertyName).isPresent());
        Change postChange = post.getDiff().get(propertyName).get();
        assertEquals(oldValue, postChange.getOldValue());
        assertEquals(newValue, postChange.getNewValue());
    }

    @SuppressWarnings({"rawtypes"})
    private <T extends Info> void testAddEvent(
            T info, Consumer<T> addOp, Class<? extends LocalAddEvent> eventType) {
        listener.clear();
        addOp.accept(info);
        LocalAddEvent event = listener.expectOne(eventType);

        assertEquals(info, event.getObject());
    }
}
