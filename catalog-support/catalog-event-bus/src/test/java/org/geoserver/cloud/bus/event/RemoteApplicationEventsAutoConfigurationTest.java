/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.cloud.bus.GeoServerBusProperties;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogAddEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogModifyEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogRemoveEvent;
import org.geoserver.cloud.bus.event.config.RemoteConfigAddEvent;
import org.geoserver.cloud.bus.event.config.RemoteConfigModifyEvent;
import org.geoserver.cloud.bus.event.config.RemoteConfigRemoveEvent;
import org.geoserver.cloud.event.PropertyDiffTestSupport;
import org.geoserver.cloud.test.ApplicationEventCapturingListener;
import org.geoserver.cloud.test.TestConfigurationAutoConfiguration;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.bus.SpringCloudBusClient;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Uses {@code spring-cloud-stream-test-support} (must be a test dependency) to collect the
 * out-bound messages with a {@link MessageCollector}, but from the {@link
 * SpringCloudBusClient#OUTPUT} channel.
 *
 * <p>See <a href=
 * "https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/_testing.html">spring
 * streams testing</a> docs for reference.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = {TestConfigurationAutoConfiguration.class, ApplicationEventCapturingListener.class}
)
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
public class RemoteApplicationEventsAutoConfigurationTest {

    /**
     * Spring-cloud-stream test message collector used to capture out-bound {@link RemoteInfoEvent}s
     */
    private @Autowired MessageCollector messageCollector;

    /**
     * Spring-cloud-bus auto-configured message channels for spring-cloud-stream {@link
     * SpringCloudBusClient#springCloudBusOutput()}
     */
    private @Autowired SpringCloudBusClient springCloudBusChannels;

    /**
     * Message converter registered by spring-cloud-bus, used to parse the json message sent to the
     * bus and captured by {@link #messageCollector}
     */
    private @Autowired @Qualifier("busJsonConverter") AbstractMessageConverter busJsonConverter;

    private @Autowired GeoServer geoserver;
    private @Autowired Catalog catalog;

    private @Autowired ApplicationEventCapturingListener localRemoteEventsListener;

    private @Autowired GeoServerBusProperties geoserverBusProperties;

    private @Autowired RemoteEventPayloadCodec remoteEventPayloadCodec;

    public @Rule CatalogTestData testData = CatalogTestData.empty(() -> catalog, () -> geoserver);

    private BusChannelEventCollector outBoundEvents;

    public @Before void before() {
        // restore default settings
        disablePayload();
        localRemoteEventsListener.stop();
        localRemoteEventsListener.setCapureEventsOf(RemoteInfoEvent.class);

        localRemoteEventsListener.clear();
        BlockingQueue<Message<?>> outChannel =
                messageCollector.forChannel(springCloudBusChannels.springCloudBusOutput());
        outBoundEvents =
                new BusChannelEventCollector(outChannel, busJsonConverter, remoteEventPayloadCodec);

        localRemoteEventsListener.start();
    }

    public @After void after() {
        localRemoteEventsListener.stop();
        geoserver.getServices().forEach(geoserver::remove);
        for (WorkspaceInfo ws : catalog.getWorkspaces()) {
            SettingsInfo settings = geoserver.getSettings(ws);
            if (settings != null) {
                geoserver.remove(settings);
            }
        }
        catalog.dispose();
    }

    private void disablePayload() {
        geoserverBusProperties.setSendDiff(false);
        geoserverBusProperties.setSendObject(false);
    }

    private void enablePayload() {
        geoserverBusProperties.setSendDiff(true);
        geoserverBusProperties.setSendObject(true);
    }

    public @Test void testCatalogSetDefaultWorkspace() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.workspaceC);

        disablePayload();
        PropertyDiff expected = PropertyDiff.empty();
        testCatalogModifiedEvent(
                catalog, c -> c.setDefaultWorkspace(testData.workspaceC), expected);
    }

    public @Test void testCatalogSetDefaultWorkspace_Payload() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.workspaceC);

        PropertyDiff expected =
                new PropertyDiffTestSupport()
                        .createTestDiff(
                                "defaultWorkspace", testData.workspaceA, testData.workspaceC);

        enablePayload();
        testCatalogModifiedEvent(
                catalog, c -> c.setDefaultWorkspace(testData.workspaceC), expected);
    }

    public @Test void testCatalogSetDefaultNamespace() {
        catalog.add(testData.namespaceA);
        catalog.add(testData.namespaceB);

        PropertyDiff expected = PropertyDiff.empty();
        disablePayload();
        testCatalogModifiedEvent(
                catalog, c -> c.setDefaultNamespace(testData.namespaceB), expected);
    }

    public @Test void testCatalogSetDefaultNamespace_Payload() {
        catalog.add(testData.namespaceA);
        catalog.add(testData.namespaceB);

        PropertyDiff expected =
                new PropertyDiffTestSupport()
                        .createTestDiff(
                                "defaultNamespace", testData.namespaceA, testData.namespaceB);
        enablePayload();
        testCatalogModifiedEvent(
                catalog, c -> c.setDefaultNamespace(testData.namespaceB), expected);
    }

    public @Test void testCatalogAddedEvents_Workspace() {
        disablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.workspaceA, catalog::add, eventType);
    }

    public @Test void testCatalogAddedEvents_Workspace_Payload() {
        enablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.workspaceA, catalog::add, eventType);
    }

    public @Test void testCatalogAddedEvents_Namespace() {
        disablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.namespaceA, catalog::add, eventType);
    }

    public @Test void testCatalogAddedEvents_Namespace_Payload() {
        enablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.namespaceA, catalog::add, eventType);
    }

    public @Test void testCatalogAddedEvents_CoverageStore() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        disablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.coverageStoreA, catalog::add, eventType);
    }

    public @Test void testCatalogAddedEvents_CoverageStore_Payload() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        enablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.coverageStoreA, catalog::add, eventType);
    }

    public @Test void testCatalogAddedEvents_DataStore() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        disablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.dataStoreA, catalog::add, eventType);
    }

    public @Test void testCatalogAddedEvents_DataStore_Payload() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        enablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.dataStoreA, catalog::add, eventType);
    }

    public @Test void testCatalogAddedEvents_Coverage() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.coverageStoreA);
        disablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.coverageA, catalog::add, eventType);
    }

    public @Test void testCatalogAddedEvents_Coverage_Payload() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.namespaceB);
        catalog.add(testData.coverageStoreA);
        enablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.coverageA, catalog::add, eventType);
    }

    public @Test void testCatalogAddedEvents_FeatureType() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.dataStoreA);
        disablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.featureTypeA, catalog::add, eventType);
    }

    public @Test void testCatalogAddedEvents_FeatureType_Payload() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.dataStoreA);
        enablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.featureTypeA, catalog::add, eventType);
    }

    public @Test void testCatalogAddedEvents_Layer() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.dataStoreA);
        catalog.add(testData.featureTypeA);
        catalog.add(testData.style1);
        disablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.layerFeatureTypeA, catalog::add, eventType);
    }

    public @Test void testCatalogAddedEvents_Layer_Payload() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.dataStoreA);
        catalog.add(testData.featureTypeA);
        catalog.add(testData.style1);
        enablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.layerFeatureTypeA, catalog::add, eventType);
    }

    public @Test void testCatalogAddedEvents_LayerGroup() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.dataStoreA);
        catalog.add(testData.featureTypeA);
        catalog.add(testData.layerFeatureTypeA);
        catalog.add(testData.style1);
        disablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.layerGroup1, catalog::add, eventType);
    }

    public @Test void testCatalogAddedEvents_LayerGroup_Payload() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.dataStoreA);
        catalog.add(testData.featureTypeA);
        catalog.add(testData.layerFeatureTypeA);
        catalog.add(testData.style1);
        enablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.layerGroup1, catalog::add, eventType);
    }

    public @Test void testCatalogAddedEvents_Style() {
        disablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.style1, catalog::add, eventType);
    }

    public @Test void testCatalogAddedEvents_Style_Payload() {
        enablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.style1, catalog::add, eventType);
    }

    public @Test void testCatalogRemoteModifyEvents() {
        localRemoteEventsListener.stop();
        testData.addObjects();
        localRemoteEventsListener.start();

        Class<RemoteCatalogModifyEvent> eventType = RemoteCatalogModifyEvent.class;
        testRemoteModifyEvent(
                testData.workspaceA,
                ws -> {
                    ws.setName("newName");
                    ws.setIsolated(true);
                },
                catalog::save,
                eventType);
    }

    public @Test void testCatalogRemoveEvents() {
        localRemoteEventsListener.stop();
        testData.addObjects();
        localRemoteEventsListener.start();

        Class<RemoteCatalogRemoveEvent> eventType = RemoteCatalogRemoveEvent.class;

        testRemoteRemoveEvent(testData.layerGroup1, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.layerFeatureTypeA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.featureTypeA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.coverageA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.style1, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.dataStoreA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.coverageStoreA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.wmsLayerA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.wmsStoreA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.wmtsLayerA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.wmtsStoreA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.namespaceA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.workspaceA, catalog::remove, eventType);
    }

    public @Test void testConfigAddEvent_ServiceInfo() {
        Class<RemoteConfigAddEvent> eventType = RemoteConfigAddEvent.class;

        WMSInfoImpl service = new WMSInfoImpl();
        service.setName("WMS");
        testRemoteAddEvent(service, geoserver::add, eventType);
    }

    public @Test void testConfigAddEvent_ServiceInfo_Workspace() {
        Class<RemoteConfigAddEvent> eventType = RemoteConfigAddEvent.class;

        catalog.add(testData.workspaceB);
        WMSInfoImpl workspaceService = new WMSInfoImpl();
        workspaceService.setName("WMS");
        workspaceService.setWorkspace(testData.workspaceB);
        testRemoteAddEvent(workspaceService, geoserver::add, eventType);
    }

    public @Test void testConfigAddEvent_SettingsInfo() {
        Class<RemoteConfigAddEvent> eventType = RemoteConfigAddEvent.class;

        catalog.add(testData.workspaceB);

        SettingsInfoImpl workspaceSettings = new SettingsInfoImpl();
        workspaceSettings.setWorkspace(testData.workspaceB);
        testRemoteAddEvent(workspaceSettings, geoserver::add, eventType);
    }

    public @Test void testConfigRemoteModifyEvents_GeoServerInfo() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.workspaceB);

        GeoServerInfo global = geoserver.getGlobal();

        Class<RemoteConfigModifyEvent> eventType = RemoteConfigModifyEvent.class;

        CoverageAccessInfo coverageInfo = new CoverageAccessInfoImpl();
        coverageInfo.setCorePoolSize(10);
        coverageInfo.setQueueType(QueueType.UNBOUNDED);

        testRemoteModifyEvent(
                global,
                gs -> {
                    gs.setAdminUsername("admin");
                    gs.setAdminPassword("secret");
                    gs.setCoverageAccess(coverageInfo);
                },
                geoserver::save,
                eventType);
    }

    public @Test void testConfigRemotetModifyEvents_GloabalSettingsInfo() {
        Class<RemoteConfigModifyEvent> eventType = RemoteConfigModifyEvent.class;

        // odd API weirdness here, can't modify global settings through GeoSever.save(SettingsInfo),
        // complains settings must be part of a workspace, although you can get the global settings
        // through GeoServer.getSettings();
        testRemoteModifyEvent(
                geoserver.getGlobal(),
                g -> {
                    g.getSettings().setCharset("ISO-8869-1");
                    g.getSettings().setProxyBaseUrl("http://test.com");
                },
                geoserver::save,
                eventType);
    }

    public @Test void testConfigRemotetModifyEvents_SettingsInfo() {
        Class<RemoteConfigModifyEvent> eventType = RemoteConfigModifyEvent.class;

        catalog.add(testData.workspaceA);
        catalog.add(testData.workspaceB);

        SettingsInfo workspaceSettings = new SettingsInfoImpl();
        workspaceSettings.setWorkspace(testData.workspaceA);
        geoserver.add(workspaceSettings);
        workspaceSettings = geoserver.getSettings(testData.workspaceA);

        testRemoteModifyEvent(
                workspaceSettings,
                s -> {
                    s.setCharset("ISO-8869-1");
                    s.setWorkspace(testData.workspaceB);
                },
                geoserver::save,
                eventType);
    }

    public @Test void testConfigRemoteModifyEvents_LoggingInfo() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.workspaceB);

        Class<RemoteConfigModifyEvent> eventType = RemoteConfigModifyEvent.class;

        LoggingInfo globalLogging = geoserver.getLogging();
        testRemoteModifyEvent(
                globalLogging,
                logging -> {
                    logging.setLevel("TRACE");
                    logging.setStdOutLogging(!logging.isStdOutLogging());
                },
                geoserver::save,
                eventType);
    }

    public @Test void testConfigRemoteRemoveEvent_SettingsInfo() {
        localRemoteEventsListener.stop();

        catalog.add(testData.workspaceC);
        SettingsInfo settings = new SettingsInfoImpl();
        settings.setWorkspace(testData.workspaceC);
        geoserver.add(settings);

        localRemoteEventsListener.start();

        testRemoteRemoveEvent(settings, geoserver::remove, RemoteConfigRemoveEvent.class);
    }

    public @Test void testConfigRemoveEvent_ServiceInfo() {
        localRemoteEventsListener.stop();
        ServiceInfo service = new WMSInfoImpl();
        geoserver.add(service);

        localRemoteEventsListener.start();

        testRemoteRemoveEvent(service, geoserver::remove, RemoteConfigRemoveEvent.class);
    }

    private <T extends Info> void testRemoteRemoveEvent(
            T info,
            Consumer<T> remover,
            @SuppressWarnings("rawtypes") Class<? extends RemoteRemoveEvent> eventType) {
        this.localRemoteEventsListener.clear();
        this.outBoundEvents.clear();
        remover.accept(info);
        @SuppressWarnings("unchecked")
        RemoteRemoveEvent<?, T> event = localRemoteEventsListener.expectOne(eventType);
        assertRemoteEvent(info, event);

        // local-remote event ok, check the one sent over the wire
        @SuppressWarnings("unchecked")
        RemoteRemoveEvent<?, T> parsedSentEvent = this.outBoundEvents.expectOne(eventType);
        assertRemoteEvent(info, parsedSentEvent);
    }

    @SuppressWarnings("unchecked")
    private <T extends Info> void testRemoteModifyEvent(
            T info,
            Consumer<T> modifier,
            Consumer<T> saver,
            @SuppressWarnings("rawtypes") Class<? extends RemoteModifyEvent> eventType) {

        Class<T> type = resolveInfoInterface(info);
        T proxy = ModificationProxy.create(ModificationProxy.unwrap(info), type);
        modifier.accept(proxy);

        PropertyDiff expected = resolveExpectedDiff(proxy);

        this.localRemoteEventsListener.clear();
        this.localRemoteEventsListener.start();
        this.outBoundEvents.clear();
        saver.accept(proxy);

        RemoteModifyEvent<?, T> localRemoteEvent = localRemoteEventsListener.expectOne(eventType);
        assertRemoteEvent(info, localRemoteEvent);

        RemoteModifyEvent<?, T> sentRemoteEvent = this.outBoundEvents.expectOne(eventType);
        assertRemoteEvent(info, sentRemoteEvent);

        if (this.geoserverBusProperties.isSendDiff()) {
            assertEquals(expected, localRemoteEvent.diff().get());
            assertEquals(expected, sentRemoteEvent.diff().get());
        }
        if (this.geoserverBusProperties.isSendObject()) {
            testData.assertEqualsLenientConnectionParameters(info, localRemoteEvent.object().get());
            testData.assertEqualsLenientConnectionParameters(info, sentRemoteEvent.object().get());
        }
    }

    private void testCatalogModifiedEvent(
            Catalog catalog, Consumer<Catalog> modifier, PropertyDiff expected) {
        this.localRemoteEventsListener.clear();
        this.localRemoteEventsListener.start();
        this.outBoundEvents.clear();

        modifier.accept(catalog);

        Class<RemoteCatalogModifyEvent> eventType = RemoteCatalogModifyEvent.class;
        RemoteModifyEvent<Catalog, CatalogInfo> localRemoteEvent;
        RemoteModifyEvent<Catalog, CatalogInfo> sentEvent;

        localRemoteEvent = localRemoteEventsListener.expectOne(eventType);
        sentEvent = outBoundEvents.expectOne(eventType);

        assertCatalogEvent(catalog, localRemoteEvent, expected);
        assertCatalogEvent(catalog, sentEvent, expected);
    }

    private void assertCatalogEvent(
            Catalog catalog, RemoteModifyEvent<Catalog, CatalogInfo> event, PropertyDiff expected) {
        assertNotNull(event.getId());
        assertEquals("**", event.getDestinationService());
        assertEquals("catalog", event.getObjectId());
        assertNotNull(event.getInfoType());
        assertEquals(Catalog.class, event.getInfoType().getType());
        assertTrue("Catalog changes should always have a null object", event.object().isEmpty());

        if (this.geoserverBusProperties.isSendDiff()) {
            assertNotNull(event.getSerializedDiff());
            assertEquals(expected, event.diff().get());
        }
    }

    private <T extends Info> PropertyDiff resolveExpectedDiff(T proxy) {
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(proxy);
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();
        assertFalse("Test should change at least one property", propertyNames.isEmpty());

        PropertyDiff expected = PropertyDiff.valueOf(propertyNames, oldValues, newValues);
        return expected;
    }

    @SuppressWarnings("unchecked")
    private <T extends Info> Class<T> resolveInfoInterface(T info) {
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
        return type;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T extends Info> void testRemoteAddEvent(
            T info, Consumer<T> addOp, Class<? extends RemoteAddEvent> eventType) {
        this.localRemoteEventsListener.clear();
        this.outBoundEvents.clear();
        addOp.accept(info);

        RemoteAddEvent<?, T> event = localRemoteEventsListener.expectOne(eventType);
        assertRemoteEvent(info, event);

        // ok, that's the event published to the local application context, and which
        // spring-cloud-bus took care of not re-publishing. Let's capture the actual out-bound
        // message from the bus channel
        RemoteAddEvent<?, T> parsedSentEvent = this.outBoundEvents.expectOne(eventType);
        assertRemoteEvent(info, parsedSentEvent);
    }

    private <T extends Info> void assertRemoteEvent(T info, RemoteInfoEvent<?, T> event) {
        assertNotNull(event.getId());
        assertEquals("**", event.getDestinationService());
        assertEquals(info.getId(), event.getObjectId());
        assertNotNull(event.getInfoType());
        assertTrue(event.getInfoType().getType().isInstance(info));

        if (geoserverBusProperties.isSendObject()) {
            assertNotNull(event.getSerializedObject());
            Optional<T> objectOpt = event.object();
            assertTrue(objectOpt.isPresent());
            T object = objectOpt.get();
            testData.assertEqualsLenientConnectionParameters(info, object);
        } else {
            assertNull(event.getSerializedObject());
            assertTrue(event.object().isEmpty());
        }

        if (event instanceof RemoteModifyEvent) {
            RemoteModifyEvent<?, T> modifyEvent = (RemoteModifyEvent<?, T>) event;
            if (geoserverBusProperties.isSendDiff()) {
                assertNotNull(modifyEvent.getSerializedDiff());
                Optional<PropertyDiff> diffOpt = modifyEvent.diff();
                assertTrue(diffOpt.isPresent());
                PropertyDiff diff = diffOpt.get();
                assertThat(diff.size(), greaterThan(0));
            } else {
                assertNull(modifyEvent.getSerializedDiff());
                assertTrue(modifyEvent.diff().isEmpty());
            }
        }
    }
}
