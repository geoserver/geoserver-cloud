/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.events;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.NonNull;

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
import org.geoserver.cloud.event.catalog.CatalogInfoAdded;
import org.geoserver.cloud.event.catalog.CatalogInfoModified;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.cloud.event.config.ConfigInfoAdded;
import org.geoserver.cloud.event.config.ConfigInfoModified;
import org.geoserver.cloud.event.config.ServiceRemoved;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoAdded;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.cloud.event.info.InfoModified;
import org.geoserver.cloud.event.info.InfoRemoved;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@SpringBootTest(
        classes = {
            TestConfigurationAutoConfiguration.class,
            ApplicationEventCapturingListener.class
        })
@EnableAutoConfiguration
public class CatalogApplicationEventsConfigurationTest {

    private @Autowired GeoServer geoserver;
    private @Autowired Catalog catalog;

    private @Autowired ApplicationEventCapturingListener listener;

    private CatalogTestData testData;

    public @BeforeEach void before() {
        listener.setCapureEventsOf(InfoEvent.class);
        catalog.dispose();
        listener.clear();
        testData = CatalogTestData.empty(() -> catalog, () -> geoserver).initialize();
    }

    public @AfterEach void after() {
        testData.after();
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
        testModify(catalog, "defaultWorkspace", testData.workspaceA, testData.workspaceC);

        listener.clear();
        catalog.setDefaultWorkspace(testData.workspaceB);
        testModify(catalog, "defaultWorkspace", testData.workspaceC, testData.workspaceB);
    }

    public @Test void testCatalogSetDefaultNamespace() {
        listener.stop();
        catalog.add(testData.namespaceA);
        catalog.add(testData.namespaceB);
        catalog.add(testData.namespaceC);
        listener.start();

        listener.clear();
        catalog.setDefaultNamespace(testData.namespaceC);
        testModify(catalog, "defaultNamespace", testData.namespaceA, testData.namespaceC);

        listener.clear();
        catalog.setDefaultNamespace(testData.namespaceB);
        testModify(catalog, "defaultNamespace", testData.namespaceC, testData.namespaceB);
    }

    public @Test void testCatalogAddedEvents() {
        Class<CatalogInfoAdded> eventType = CatalogInfoAdded.class;
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

    public @Test void testCatalogRemoveEvents() {
        listener.stop();
        testData.addObjects();
        listener.start();

        testRemove(testData.layerGroup1, catalog::remove, CatalogInfoRemoved.class);
    }

    public @Test void testConfigAddEvents() {
        catalog.add(testData.workspaceB);
        assertNotNull(catalog.getWorkspace(testData.workspaceB.getId()));
        assertSame(catalog, geoserver.getCatalog());

        @SuppressWarnings("rawtypes")
        Class<ConfigInfoAdded> eventType = ConfigInfoAdded.class;

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

    public @Test void testConfigModifyEvents_GeoServerInfo() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.workspaceB);
        geoserver.setGlobal(testData.global);

        GeoServerInfo global = geoserver.getGlobal();

        @SuppressWarnings("rawtypes")
        Class<ConfigInfoModified> eventType = ConfigInfoModified.class;

        CoverageAccessInfo coverageInfo = new CoverageAccessInfoImpl();
        coverageInfo.setCorePoolSize(10);
        coverageInfo.setQueueType(QueueType.UNBOUNDED);

        testModify(
                global,
                gs -> {
                    gs.setAdminUsername("new_name_" + gs.getAdminUsername());
                    gs.setAdminPassword("secret");
                    gs.setCoverageAccess(coverageInfo);
                },
                geoserver::save,
                eventType);
    }

    public @Test void testConfigPrePostModifyEvents_SettingsInfo() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.workspaceB);
        geoserver.setGlobal(testData.global);

        @SuppressWarnings("rawtypes")
        Class<ConfigInfoModified> eventType = ConfigInfoModified.class;

        // odd API weirdness here, can't modify global settings through
        // GeoServer.save(SettingsInfo),
        // complains settings must be part of a workspace, although you can get the global settings
        // through GeoServer.getSettings();
        testModify(
                geoserver.getGlobal(),
                g -> {
                    SettingsInfo settings = g.getSettings();
                    settings.setCharset("ISO-8869-1");
                    settings.setProxyBaseUrl("http://test.com");
                },
                geoserver::save,
                eventType);

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
                eventType);
    }

    public @Test void testConfigModifyEvents_LoggingInfo() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.workspaceB);
        geoserver.setLogging(testData.logging);

        @SuppressWarnings("rawtypes")
        Class<ConfigInfoModified> eventType = ConfigInfoModified.class;

        LoggingInfo globalLogging = geoserver.getLogging();
        testModify(
                globalLogging,
                logging -> {
                    logging.setLevel("TRACE");
                    logging.setStdOutLogging(!logging.isStdOutLogging());
                },
                geoserver::save,
                eventType);
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

        testConfigModifyService(testData.wmsService);
        testConfigModifyService(testData.wfsService);
        // WCSInfoImpl.equals() doesn't work
        // testConfigModifyService(testData.wcsService);
        // WPSInfoImpl.equals() doesn't work
        // testConfigModifyService(testData.wpsService);
    }

    private void testConfigModifyService(ServiceInfo service) {
        service = geoserver.getService(service.getId(), ServiceInfo.class);

        @SuppressWarnings("rawtypes")
        Class<ConfigInfoModified> postEventType = ConfigInfoModified.class;

        testModify(
                service,
                s -> {
                    s.setName(s.getName() + "_changed");
                    s.setFees("SOME");
                    s.setEnabled(false);
                    s.getKeywords().clear();
                },
                geoserver::save,
                postEventType);
    }

    public @Test void testConfigRemoveEvents() {
        listener.stop();
        ServiceInfo service = new WMSInfoImpl();
        geoserver.add(service);
        listener.start();

        testRemove(service, geoserver::remove, ServiceRemoved.class);
    }

    private <T extends Info> void testRemove(
            T info,
            Consumer<T> remover,
            @SuppressWarnings("rawtypes") Class<? extends InfoRemoved> eventType) {
        listener.clear();
        listener.start();
        remover.accept(info);
        @SuppressWarnings("unchecked")
        InfoRemoved<?, T> event = listener.expectOne(eventType);
        assertEquals(info.getId(), event.getObjectId());
        assertEquals(ConfigInfoType.valueOf(info), event.getObjectType());
    }

    @SuppressWarnings("unchecked")
    private <T extends Info> void testModify(
            @NonNull T info,
            @NonNull Consumer<T> modifier,
            @NonNull Consumer<T> saver,
            @NonNull @SuppressWarnings("rawtypes") Class<? extends InfoModified> postEventType) {
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
        assertFalse(propertyNames.isEmpty(), "Test should change at least one property");

        Patch expected = PropertyDiff.valueOf(propertyNames, oldValues, newValues).toPatch();

        listener.clear();
        listener.start();
        // apply the changes to the actual object and save it
        modifier.accept(info);
        saver.accept(info);

        InfoModified<?, T> post = listener.expectOne(postEventType);
        assertEquals(proxy.getId(), post.getObjectId());
        assertEquals(expected, post.getPatch());
    }

    private void testModify(
            CatalogInfo objectChanged,
            String propertyName,
            CatalogInfo oldValue,
            CatalogInfo newValue) {

        CatalogInfoModified post = listener.expectOne(CatalogInfoModified.class);

        assertSame(objectChanged.getId(), post.getObjectId());

        assertFalse(post.getPatch().isEmpty());

        assertTrue(post.getPatch().get(propertyName).isPresent());
        Property postChange = post.getPatch().get(propertyName).get();
        assertEquals(newValue, postChange.getValue());
    }

    @SuppressWarnings({"rawtypes"})
    private <T extends Info> void testAddEvent(
            T info, Consumer<T> addOp, Class<? extends InfoAdded> eventType) {
        listener.clear();
        addOp.accept(info);
        InfoAdded event = listener.expectOne(eventType);

        assertEquals(info, event.getObject());
    }
}
