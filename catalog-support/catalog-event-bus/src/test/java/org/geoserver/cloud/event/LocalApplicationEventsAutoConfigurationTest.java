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
import org.geoserver.cloud.test.CatalogTestData;
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

    public @Rule CatalogTestData testData = CatalogTestData.empty(() -> catalog);

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
        catalog.add(testData.ws);
        catalog.add(testData.wsA);
        catalog.add(testData.wsB);
        listener.start();

        listener.clear();
        catalog.setDefaultWorkspace(testData.wsB);
        testPrePostModify(catalog, "defaultWorkspace", testData.ws, testData.wsB);

        listener.clear();
        catalog.setDefaultWorkspace(testData.wsA);
        testPrePostModify(catalog, "defaultWorkspace", testData.wsB, testData.wsA);
    }

    public @Test void testCatalogSetDefaultNamespace() {
        listener.stop();
        catalog.add(testData.ns);
        catalog.add(testData.nsA);
        catalog.add(testData.nsB);
        listener.start();

        listener.clear();
        catalog.setDefaultNamespace(testData.nsB);
        testPrePostModify(catalog, "defaultNamespace", testData.ns, testData.nsB);

        listener.clear();
        catalog.setDefaultNamespace(testData.nsA);
        testPrePostModify(catalog, "defaultNamespace", testData.nsB, testData.nsA);
    }

    public @Test void testCatalogAddedEvents() {
        Class<LocalCatalogAddEvent> eventType = LocalCatalogAddEvent.class;
        testAddEvent(testData.ws, catalog::add, eventType);
        testAddEvent(testData.ns, catalog::add, eventType);
        testAddEvent(testData.cs, catalog::add, eventType);
        testAddEvent(testData.ds, catalog::add, eventType);
        testAddEvent(testData.cv, catalog::add, eventType);
        testAddEvent(testData.ft, catalog::add, eventType);
        testAddEvent(testData.layer, catalog::add, eventType);
        testAddEvent(testData.layerGroup, catalog::add, eventType);
        testAddEvent(testData.style, catalog::add, eventType);
    }

    public @Test void testCatalogPrePostModifyEvents() {
        listener.stop();
        testData.addObjects();
        listener.start();

        Class<LocalCatalogPreModifyEvent> preEventType = LocalCatalogPreModifyEvent.class;
        Class<LocalCatalogPostModifyEvent> postEventType = LocalCatalogPostModifyEvent.class;
        testModify(
                testData.ws,
                ws -> {
                    ws.setName("newName");
                    ws.setIsolated(true);
                },
                catalog::save,
                preEventType,
                postEventType);

        testModify(
                testData.cs,
                cs -> {
                    cs.setName("newCoverageStoreName");
                    cs.setDescription("new description");
                    cs.setWorkspace(testData.wsB);
                },
                catalog::save,
                preEventType,
                postEventType);
    }

    public @Test void testCatalogRemoveEvents() {
        listener.stop();
        testData.addObjects();
        listener.start();

        testRemove(testData.layerGroup, catalog::remove, LocalCatalogRemoveEvent.class);
    }

    public @Test void testConfigAddEvents() {
        catalog.add(testData.wsA);

        Class<LocalConfigAddEvent> eventType = LocalConfigAddEvent.class;

        WMSInfoImpl service = new WMSInfoImpl();
        service.setName("WMS");
        testAddEvent(service, geoserver::add, eventType);

        WMSInfoImpl workspaceService = new WMSInfoImpl();
        workspaceService.setName("WMS");
        workspaceService.setWorkspace(testData.wsA);
        testAddEvent(workspaceService, geoserver::add, eventType);

        SettingsInfoImpl workspaceSettings = new SettingsInfoImpl();
        workspaceSettings.setWorkspace(testData.wsA);
        testAddEvent(workspaceSettings, geoserver::add, eventType);
    }

    public @Test void testConfigPrePostModifyEvents_GeoServerInfo() {
        catalog.add(testData.ws);
        catalog.add(testData.wsA);

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
        catalog.add(testData.ws);
        catalog.add(testData.wsA);

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
        workspaceSettings.setWorkspace(testData.ws);
        geoserver.add(workspaceSettings);
        workspaceSettings = geoserver.getSettings(testData.ws);

        testModify(
                workspaceSettings,
                s -> {
                    s.setCharset("ISO-8869-1");
                    s.setWorkspace(testData.wsA);
                },
                geoserver::save,
                preEventType,
                postEventType);
    }

    public @Test void testConfigPrePostModifyEvents_LoggingInfo() {
        catalog.add(testData.ws);
        catalog.add(testData.wsA);

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
