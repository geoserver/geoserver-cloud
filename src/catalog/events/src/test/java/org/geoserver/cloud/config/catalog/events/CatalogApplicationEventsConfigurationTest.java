/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.events;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Patch.Property;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.cloud.event.catalog.CatalogInfoAddEvent;
import org.geoserver.cloud.event.catalog.CatalogInfoModifyEvent;
import org.geoserver.cloud.event.catalog.CatalogInfoPreModifyEvent;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoveEvent;
import org.geoserver.cloud.event.config.ConfigInfoAddEvent;
import org.geoserver.cloud.event.config.ConfigInfoModifyEvent;
import org.geoserver.cloud.event.config.ConfigInfoPreModifyEvent;
import org.geoserver.cloud.event.config.ServiceInfoRemoveEvent;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoAddEvent;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.cloud.event.info.InfoPostModifyEvent;
import org.geoserver.cloud.event.info.InfoPreModifyEvent;
import org.geoserver.cloud.event.info.InfoRemoveEvent;
import org.geoserver.cloud.test.ApplicationEventCapturingListener;
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

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@SpringBootTest(
        classes = {
            TestConfigurationAutoConfiguration.class,
            ApplicationEventCapturingListener.class
        })
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
public class CatalogApplicationEventsConfigurationTest {

    private @Autowired GeoServer geoserver;
    private @Autowired Catalog catalog;

    private @Autowired ApplicationEventCapturingListener listener;

    public @Rule CatalogTestData testData = CatalogTestData.empty(() -> catalog, () -> geoserver);

    public @Before void before() {
        listener.setCapureEventsOf(InfoEvent.class);
        catalog.dispose();
        listener.clear();
    }

    public @Test void testCatalogEventBroadcasterHasSetUpItself() {
        Optional<CatalogListener> publiherListener =
                catalog.getListeners().stream()
                        .filter(
                                l ->
                                        l
                                                instanceof
                                                CatalogApplicationEventPublisher
                                                        .LocalCatalogEventPublisher)
                        .findFirst();
        assertTrue(publiherListener.isPresent());
    }

    public @Test void testConfigEventBroadcasterHasSetUpItself() {
        Optional<ConfigurationListener> publiherListener =
                geoserver.getListeners().stream()
                        .filter(
                                l ->
                                        l
                                                instanceof
                                                CatalogApplicationEventPublisher
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
        Class<CatalogInfoAddEvent> eventType = CatalogInfoAddEvent.class;
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

        Class<CatalogInfoPreModifyEvent> preEventType = CatalogInfoPreModifyEvent.class;
        Class<CatalogInfoModifyEvent> postEventType = CatalogInfoModifyEvent.class;
        testModify(
                catalog.getWorkspace(testData.workspaceA.getId()),
                ws -> {
                    ws.setName("newName");
                },
                catalog::save,
                preEventType,
                postEventType);

        testModify(
                catalog.getNamespace(testData.namespaceA.getId()),
                n -> {
                    n.setPrefix("new-prefix");
                },
                catalog::save,
                preEventType,
                postEventType);
    }

    public @Test void testCatalogRemoveEvents() {
        listener.stop();
        testData.addObjects();
        listener.start();

        testRemove(testData.layerGroup1, catalog::remove, CatalogInfoRemoveEvent.class);
    }

    public @Test void testConfigAddEvents() {
        catalog.add(testData.workspaceB);

        @SuppressWarnings("rawtypes")
        Class<ConfigInfoAddEvent> eventType = ConfigInfoAddEvent.class;

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

        @SuppressWarnings("rawtypes")
        Class<ConfigInfoPreModifyEvent> preEventType = ConfigInfoPreModifyEvent.class;
        @SuppressWarnings("rawtypes")
        Class<ConfigInfoModifyEvent> postEventType = ConfigInfoModifyEvent.class;

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
        geoserver.setGlobal(testData.global);
        @SuppressWarnings("rawtypes")
        Class<ConfigInfoPreModifyEvent> preEventType = ConfigInfoPreModifyEvent.class;
        @SuppressWarnings("rawtypes")
        Class<ConfigInfoModifyEvent> postEventType = ConfigInfoModifyEvent.class;

        // odd API weirdness here, can't modify global settings through
        // GeoServer.save(SettingsInfo),
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
        geoserver.setLogging(testData.logging);

        @SuppressWarnings("rawtypes")
        Class<ConfigInfoPreModifyEvent> preEventType = ConfigInfoPreModifyEvent.class;
        @SuppressWarnings("rawtypes")
        Class<ConfigInfoModifyEvent> postEventType = ConfigInfoModifyEvent.class;

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

    public @Test void testConfigPrePostModifyEvents_ServiceInfo() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.workspaceB);

        geoserver.add(testData.wmsService);
        geoserver.add(testData.wfsService);

        testData.wcsService.setWorkspace(testData.workspaceA);
        testData.wpsService.setWorkspace(testData.workspaceB);
        geoserver.add(testData.wcsService);
        geoserver.add(testData.wpsService);

        testConfigPrePostModifyService(testData.wmsService);
        testConfigPrePostModifyService(testData.wfsService);
        // WCSInfoImpl.equals() doesn't work
        // testConfigPrePostModifyService(testData.wcsService);
        // WPSInfoImpl.equals() doesn't work
        // testConfigPrePostModifyService(testData.wpsService);
    }

    private void testConfigPrePostModifyService(ServiceInfo service) {
        service = geoserver.getService(service.getId(), ServiceInfo.class);

        @SuppressWarnings("rawtypes")
        Class<ConfigInfoPreModifyEvent> preEventType = ConfigInfoPreModifyEvent.class;
        @SuppressWarnings("rawtypes")
        Class<ConfigInfoModifyEvent> postEventType = ConfigInfoModifyEvent.class;

        testModify(
                service,
                s -> {
                    s.setName(s.getName() + "_changed");
                    s.setFees("SOME");
                    s.setEnabled(false);
                    s.getKeywords().clear();
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

        testRemove(service, geoserver::remove, ServiceInfoRemoveEvent.class);
    }

    private <T extends Info> void testRemove(
            T info,
            Consumer<T> remover,
            @SuppressWarnings("rawtypes") Class<? extends InfoRemoveEvent> eventType) {
        listener.clear();
        listener.start();
        remover.accept(info);
        @SuppressWarnings("unchecked")
        InfoRemoveEvent<?, ?, T> event = listener.expectOne(eventType);
        assertEquals(info.getId(), event.getObjectId());
        assertEquals(ConfigInfoType.valueOf(info), event.getObjectType());
    }

    @SuppressWarnings("unchecked")
    private <T extends Info> void testModify(
            T info,
            Consumer<T> modifier,
            Consumer<T> saver,
            @SuppressWarnings("rawtypes") Class<? extends InfoPreModifyEvent> preEventType,
            @SuppressWarnings("rawtypes") Class<? extends InfoPostModifyEvent> postEventType) {
        if (null == ModificationProxy.handler(info))
            throw new IllegalArgumentException("Expected a ModificationProxy");

        Class<T> type;
        ClassMappings classMappings =
                ClassMappings.fromImpl(ModificationProxy.unwrap(info).getClass());
        if (classMappings != null) {
            type = (Class<T>) classMappings.getInterface();
        } else if (info instanceof GeoServerInfo) {
            type = (Class<T>) GeoServerInfo.class;
        } else if (info instanceof SettingsInfo) {
            type = (Class<T>) SettingsInfo.class;
        } else if (info instanceof LoggingInfo) {
            type = (Class<T>) LoggingInfo.class;
        } else {
            throw new IllegalArgumentException("Unknown Info type: " + info);
        }
        // apply the changes to a new proxy to build the expected PropertyDiff
        T proxy = ModificationProxy.create(ModificationProxy.unwrap(info), type);
        modifier.accept(proxy);

        ModificationProxy h = ModificationProxy.handler(proxy);
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();
        assertFalse("Test should change at least one property", propertyNames.isEmpty());

        Patch expected = PropertyDiff.valueOf(propertyNames, oldValues, newValues).toPatch();

        listener.clear();
        listener.start();
        // apply the changes to the actual object and save it
        modifier.accept(info);
        saver.accept(info);

        InfoPreModifyEvent<?, ?, T> pre = listener.expectOne(preEventType);
        assertEquals(info.getId(), pre.getObjectId());

        assertEquals(expected, pre.getPatch());

        InfoPostModifyEvent<?, ?, T> post = listener.expectOne(postEventType);
        assertEquals(proxy.getId(), post.getObjectId());
        assertEquals(expected, post.getPatch());
    }

    private void testPrePostModify(
            CatalogInfo objectChanged,
            String propertyName,
            CatalogInfo oldValue,
            CatalogInfo newValue) {
        CatalogInfoPreModifyEvent pre = listener.expectOne(CatalogInfoPreModifyEvent.class);
        CatalogInfoModifyEvent post = listener.expectOne(CatalogInfoModifyEvent.class);

        assertSame(objectChanged.getId(), pre.getObjectId());
        assertSame(objectChanged.getId(), post.getObjectId());

        assertFalse(pre.getPatch().isEmpty());
        assertFalse(post.getPatch().isEmpty());
        assertTrue(pre.getPatch().get(propertyName).isPresent());

        Property preChange = pre.getPatch().get(propertyName).get();

        assertEquals(newValue, preChange.getValue());

        assertTrue(post.getPatch().get(propertyName).isPresent());
        Property postChange = post.getPatch().get(propertyName).get();
        assertEquals(newValue, postChange.getValue());
    }

    @SuppressWarnings({"rawtypes"})
    private <T extends Info> void testAddEvent(
            T info, Consumer<T> addOp, Class<? extends InfoAddEvent> eventType) {
        listener.clear();
        addOp.accept(info);
        InfoAddEvent event = listener.expectOne(eventType);

        assertEquals(info, event.getObject());
    }
}
