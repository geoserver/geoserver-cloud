/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event;

import org.geoserver.cloud.bus.event.config.RemoteGeoServerInfoModifyEvent;
import org.geoserver.cloud.bus.event.config.RemoteLoggingInfoModifyEvent;
import org.geoserver.cloud.bus.event.config.RemoteServiceInfoAddEvent;
import org.geoserver.cloud.bus.event.config.RemoteServiceInfoModifyEvent;
import org.geoserver.cloud.bus.event.config.RemoteServiceInfoRemoveEvent;
import org.geoserver.cloud.bus.event.config.RemoteSettingsInfoAddEvent;
import org.geoserver.cloud.bus.event.config.RemoteSettingsInfoModifyEvent;
import org.geoserver.cloud.bus.event.config.RemoteSettingsInfoRemoveEvent;
import org.geoserver.cloud.test.ApplicationEventCapturingListener;
import org.geoserver.cloud.test.TestConfigurationAutoConfiguration;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.CoverageAccessInfo.QueueType;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.CoverageAccessInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.wms.CacheConfiguration;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = {
        TestConfigurationAutoConfiguration.class,
        ApplicationEventCapturingListener.class,
        TestSpringCloudBusConfig.class
    }
)
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
public class ConfigRemoteApplicationEventsTest extends AbstractRemoteApplicationEventsTest {

    public @Test void testConfigAddEvent_ServiceInfo() {
        testConfigAddEvent_ServiceInfo(false);
    }

    @Ignore("revisit, not all payloads work, possibly a misconfigured ObjectMapper for the bus")
    public @Test void testConfigAddEvent_ServiceInfo_Payload() {
        testConfigAddEvent_ServiceInfo(true);
    }

    private void testConfigAddEvent_ServiceInfo(boolean payload) {
        Class<RemoteServiceInfoAddEvent> eventType = RemoteServiceInfoAddEvent.class;

        WMSInfoImpl service = new WMSInfoImpl();
        service.setName("WMS");
        enablePayload(payload);
        testRemoteAddEvent(service, geoserver::add, eventType);
    }

    public @Test void testConfigAddEvent_ServiceInfo_Workspace() {
        testConfigAddEvent_ServiceInfo_Workspace(false);
    }

    @Ignore("revisit, not all payloads work, possibly a misconfigured ObjectMapper for the bus")
    public @Test void testConfigAddEvent_ServiceInfo_Workspace_Payload() {
        testConfigAddEvent_ServiceInfo_Workspace(true);
    }

    private void testConfigAddEvent_ServiceInfo_Workspace(boolean payload) {
        Class<RemoteServiceInfoAddEvent> eventType = RemoteServiceInfoAddEvent.class;

        catalog.add(testData.workspaceB);
        WMSInfoImpl workspaceService = new WMSInfoImpl();
        workspaceService.setName("WMS");
        workspaceService.setWorkspace(testData.workspaceB);
        enablePayload(payload);
        testRemoteAddEvent(workspaceService, geoserver::add, eventType);
    }

    public @Test void testConfigRemoteModifyEvent_ServiceInfo_Workspace() {
        testConfigRemoteModifyEvent_ServiceInfo_Workspace(false);
    }

    @Ignore("revisit, not all payloads work, possibly a misconfigured ObjectMapper for the bus")
    public @Test void testConfigRemoteModifyEvent_ServiceInfo_Workspace_Payload() {
        testConfigRemoteModifyEvent_ServiceInfo_Workspace(true);
    }

    private void testConfigRemoteModifyEvent_ServiceInfo_Workspace(boolean payload) {
        catalog.add(testData.workspaceB);

        WMSInfo service = testData.createService("wms", WMSInfoImpl::new);
        service.setWorkspace(testData.workspaceB);
        geoserver.add(service);

        enablePayload(payload);

        CacheConfiguration cacheCfg = new CacheConfiguration(true, 1000, 100);
        Class<RemoteServiceInfoModifyEvent> eventType = RemoteServiceInfoModifyEvent.class;
        testRemoteModifyEvent(
                service,
                s -> {
                    s.setCacheConfiguration(cacheCfg);
                    s.getAuthorityURLs().addAll(testData.authUrls(2));
                },
                geoserver::save,
                eventType);
    }

    public @Test void testConfigAddEvent_SettingsInfo() {
        testConfigAddEvent_SettingsInfo(false);
    }

    @Ignore("revisit, not all payloads work, possibly a misconfigured ObjectMapper for the bus")
    public @Test void testConfigAddEvent_SettingsInfo_Payload() {
        testConfigAddEvent_SettingsInfo(true);
    }

    private void testConfigAddEvent_SettingsInfo(boolean payload) {
        Class<RemoteSettingsInfoAddEvent> eventType = RemoteSettingsInfoAddEvent.class;

        catalog.add(testData.workspaceB);

        SettingsInfoImpl workspaceSettings = new SettingsInfoImpl();
        workspaceSettings.setWorkspace(testData.workspaceB);
        if (payload) enablePayload();
        testRemoteAddEvent(workspaceSettings, geoserver::add, eventType);
    }

    public @Test void testConfigRemoteModifyEvents_GeoServerInfo() {
        testConfigRemoteModifyEvents_GeoServerInfo(false);
    }

    @Ignore("revisit, not all payloads work, possibly a misconfigured ObjectMapper for the bus")
    public @Test void testConfigRemoteModifyEvents_GeoServerInfo_Payload() {
        testConfigRemoteModifyEvents_GeoServerInfo(true);
    }

    private void testConfigRemoteModifyEvents_GeoServerInfo(boolean payload) {
        catalog.add(testData.workspaceA);
        catalog.add(testData.workspaceB);

        GeoServerInfo global = geoserver.getGlobal();

        Class<RemoteGeoServerInfoModifyEvent> eventType = RemoteGeoServerInfoModifyEvent.class;

        CoverageAccessInfo coverageInfo = new CoverageAccessInfoImpl();
        coverageInfo.setCorePoolSize(10);
        coverageInfo.setQueueType(QueueType.UNBOUNDED);

        enablePayload(payload);

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
        testConfigRemotetModifyEvents_GloabalSettingsInfo(false);
    }

    @Ignore("revisit, not all payloads work, possibly a misconfigured ObjectMapper for the bus")
    public @Test void testConfigRemotetModifyEvents_GloabalSettingsInfo_Payload() {
        testConfigRemotetModifyEvents_GloabalSettingsInfo(true);
    }

    private void testConfigRemotetModifyEvents_GloabalSettingsInfo(boolean payload) {
        Class<RemoteGeoServerInfoModifyEvent> eventType = RemoteGeoServerInfoModifyEvent.class;
        testData.initConfig(true).initConfig();
        enablePayload(payload);
        // odd API weirdness here, can't modify global settings through
        // GeoServer.save(SettingsInfo),
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

    @Ignore("revisit, settings already exist for workspace wsName")
    public @Test void testConfigRemotetModifyEvents_SettingsInfo() {
        testConfigRemotetModifyEvents_SettingsInfo(false);
    }

    public @Test void testConfigRemotetModifyEvents_SettingsInfo_Payload() {
        testConfigRemotetModifyEvents_SettingsInfo(true);
    }

    private void testConfigRemotetModifyEvents_SettingsInfo(boolean payload) {
        Class<RemoteSettingsInfoModifyEvent> eventType = RemoteSettingsInfoModifyEvent.class;

        catalog.add(testData.workspaceA);
        catalog.add(testData.workspaceB);

        SettingsInfo workspaceSettings = new SettingsInfoImpl();
        workspaceSettings.setWorkspace(testData.workspaceA);
        geoserver.add(workspaceSettings);
        workspaceSettings = geoserver.getSettings(testData.workspaceA);

        enablePayload(payload);
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
        testConfigRemoteModifyEvents_LoggingInfo(false);
    }

    public @Test void testConfigRemoteModifyEvents_LoggingInfo_Payload() {
        testConfigRemoteModifyEvents_LoggingInfo(false);
    }

    private void testConfigRemoteModifyEvents_LoggingInfo(boolean payload) {
        catalog.add(testData.workspaceA);
        catalog.add(testData.workspaceB);

        Class<RemoteLoggingInfoModifyEvent> eventType = RemoteLoggingInfoModifyEvent.class;

        LoggingInfo globalLogging = geoserver.getLogging();

        enablePayload(payload);

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
        testConfigRemoteRemoveEvent_SettingsInfo(false);
    }

    @Ignore("revisit, not all payloads work, possibly a misconfigured ObjectMapper for the bus")
    public @Test void testConfigRemoteRemoveEvent_SettingsInfo_Payload() {
        testConfigRemoteRemoveEvent_SettingsInfo(true);
    }

    private void testConfigRemoteRemoveEvent_SettingsInfo(boolean payload) {
        localRemoteEventsListener.stop();

        catalog.add(testData.workspaceC);
        SettingsInfo settings = new SettingsInfoImpl();
        settings.setWorkspace(testData.workspaceC);
        geoserver.add(settings);

        localRemoteEventsListener.start();
        enablePayload(payload);
        testRemoteRemoveEvent(settings, geoserver::remove, RemoteSettingsInfoRemoveEvent.class);
    }

    public @Test void testConfigRemoveEvent_ServiceInfo() {
        testConfigRemoveEvent_ServiceInfo(false);
    }

    @Ignore("revisit, not all payloads work, possibly a misconfigured ObjectMapper for the bus")
    public @Test void testConfigRemoveEvent_ServiceInfo_Payload() {
        testConfigRemoveEvent_ServiceInfo(true);
    }

    private void testConfigRemoveEvent_ServiceInfo(boolean payload) {
        localRemoteEventsListener.stop();
        ServiceInfo service = new WMSInfoImpl();
        geoserver.add(service);

        localRemoteEventsListener.start();
        enablePayload(payload);
        testRemoteRemoveEvent(service, geoserver::remove, RemoteServiceInfoRemoveEvent.class);
    }
}
