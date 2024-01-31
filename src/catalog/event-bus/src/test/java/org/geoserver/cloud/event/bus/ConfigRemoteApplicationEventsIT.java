/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.bus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.event.config.GeoServerInfoModified;
import org.geoserver.cloud.event.config.ServiceAdded;
import org.geoserver.cloud.event.config.ServiceModified;
import org.geoserver.cloud.event.config.ServiceRemoved;
import org.geoserver.cloud.event.config.SettingsAdded;
import org.geoserver.cloud.event.config.SettingsRemoved;
import org.geoserver.cog.CogSettings;
import org.geoserver.cog.CogSettings.RangeReaderType;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.CoverageAccessInfo.QueueType;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.CoverageAccessInfoImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.wms.CacheConfiguration;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.geoserver.wps.WPSInfo;
import org.geoserver.wps.WPSInfoImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

class ConfigRemoteApplicationEventsIT extends BusAmqpIntegrationTests {

    @BeforeEach
    public void before() {
        super.before();
        super.setupClean();
    }

    @Test
    void testConfigAddEvent_ServiceInfo() {
        WMSInfoImpl service = new WMSInfoImpl();
        service.setName("WMS");
        testRemoteAddEvent(service, geoserver::add, ServiceAdded.class);
    }

    @Test
    void testConfigAddEvent_WPSInfo() {
        WPSInfoImpl service = new WPSInfoImpl();
        service.setName("WPS");
        service.setTitle("My WPS");
        testRemoteAddEvent(service, geoserver::add, ServiceAdded.class);
    }

    @Test
    void testConfigAddEvent_ServiceInfo_Workspace() {
        WMSInfoImpl workspaceService = new WMSInfoImpl();
        workspaceService.setName("WMS");
        workspaceService.setWorkspace(testData.workspaceB);
        testRemoteAddEvent(workspaceService, geoserver::add, ServiceAdded.class);
    }

    @Test
    void testConfigRemoteModifyEvent_ServiceInfo_Workspace() {
        WMSInfo service = testData.faker().serviceInfo("WMS_WS_TEST", WMSInfoImpl::new);
        service.setWorkspace(testData.workspaceB);
        geoserver.add(service);

        CacheConfiguration cacheCfg = new CacheConfiguration(true, 1000, 100);

        testRemoteModifyEvent(
                service,
                s -> {
                    s.setCacheConfiguration(cacheCfg);
                    s.getAuthorityURLs().addAll(testData.faker().authUrls(2));
                },
                geoserver::save,
                ServiceModified.class);
    }

    @Test
    void testConfigRemoteModifyEvent_WPSInfo() {
        WPSInfo service = testData.faker().serviceInfo("WPS_WS_TEST", WPSInfoImpl::new);
        service.setWorkspace(testData.workspaceB);
        geoserver.add(service);

        testRemoteModifyEvent(
                service,
                s -> {
                    s.setTitle("My WPS");
                },
                geoserver::save,
                ServiceModified.class);
    }

    @Test
    void testConfigAddEvent_SettingsInfo() {
        catalog.add(testData.workspaceB);

        SettingsInfoImpl workspaceSettings = new SettingsInfoImpl();
        workspaceSettings.setWorkspace(testData.workspaceB);
        testRemoteAddEvent(workspaceSettings, geoserver::add, SettingsAdded.class);
    }

    @Test
    void testConfigRemoteModifyEvents_GeoServerInfo() {
        GeoServerInfo global = geoserver.getGlobal();
        assertNotNull(global);

        CoverageAccessInfo coverageInfo = new CoverageAccessInfoImpl();
        coverageInfo.setCorePoolSize(10);
        coverageInfo.setQueueType(QueueType.UNBOUNDED);

        Patch patch =
                testRemoteModifyEvent(
                        global,
                        gs -> {
                            gs.setAdminUsername("administrador");
                            gs.setAdminPassword("secret");
                            gs.setCoverageAccess(coverageInfo);
                            gs.getSettings().setCharset("UTF-16");
                            gs.getSettings().setProxyBaseUrl("http://test2.com");
                            gs.getSettings().getContact().setContactEmail("john.doe@test.com");
                            gs.getSettings().getContact().setAddressCity("Buenos Aires");
                        },
                        geoserver::save,
                        GeoServerInfoModified.class);

        SettingsInfo settings = (SettingsInfo) patch.get("settings").orElseThrow().getValue();
        assertThat(settings.getContact()).isNotNull();
        assertThat(settings.getCharset()).isEqualTo("UTF-16");
        assertThat(settings.getProxyBaseUrl()).isEqualTo("http://test2.com");
        assertThat(settings.getContact().getContactEmail()).isEqualTo("john.doe@test.com");
        assertThat(settings.getContact().getAddressCity()).isEqualTo("Buenos Aires");
    }

    @Test
    void testConfigRemotetModifyEvents_GloabalSettingsInfo() {
        testData.initConfig(true).initConfig();
        // odd API weirdness here, can't modify global settings through
        // GeoServer.save(SettingsInfo),
        // complains settings must be part of a workspace, although you can get the global settings
        // through GeoServer.getSettings();
        GeoServerInfo global = new GeoServerInfoImpl();
        SettingsInfo settings = new SettingsInfoImpl();
        settings.setCharset("ISO-8859-1");
        settings.setContact(testData.faker().contactInfo());

        global.setSettings(settings);
        geoserver.setGlobal(global);

        // patch parsed from remote event
        Patch patch =
                testConfigInfoModifyEvent(
                        global,
                        g -> {
                            g.getSettings().setCharset("ISO-8869-1");
                            g.getSettings().setProxyBaseUrl("http://test.com");
                            g.getSettings().getContact().setContactEmail("john@doe.com");
                            g.getSettings().getContact().setAddressCity("Rosario");
                        },
                        geoserver::save);
        SettingsInfo newSettings = (SettingsInfo) patch.get("settings").orElseThrow().getValue();
        assertThat(newSettings.getContact()).isNotNull();
        assertThat(newSettings.getContact().getContactEmail()).isEqualTo("john@doe.com");
        assertThat(newSettings.getContact().getAddressCity()).isEqualTo("Rosario");
    }

    @Test
    void testConfigRemotetModifyEvents_SettingsInfo() {
        SettingsInfo workspaceSettings = new SettingsInfoImpl();
        WorkspaceInfo workspace = testData.workspaceC;
        workspaceSettings.setWorkspace(workspace);
        geoserver.add(workspaceSettings);
        workspaceSettings = geoserver.getSettings(workspace);

        testConfigInfoModifyEvent(
                workspaceSettings,
                s -> {
                    s.setCharset("ISO-8869-1");
                    s.setDefaultLocale(Locale.CHINESE);
                },
                geoserver::save);
    }

    @Test
    void testConfigRemoteModifyEvents_LoggingInfo() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.workspaceB);

        LoggingInfo globalLogging = geoserver.getLogging();

        testConfigInfoModifyEvent(
                globalLogging,
                logging -> {
                    logging.setLevel("TRACE");
                    logging.setStdOutLogging(!logging.isStdOutLogging());
                },
                geoserver::save);
    }

    @Test
    void testConfigRemoteRemoveEvent_SettingsInfo() {
        SettingsInfo settings = new SettingsInfoImpl();
        settings.setWorkspace(testData.workspaceC);
        geoserver.add(settings);

        SettingsRemoved event =
                testRemoteRemoveEvent(settings, geoserver::remove, SettingsRemoved.class);
        assertEquals(testData.workspaceC.getId(), event.getWorkspaceId());
    }

    @Test
    void testConfigRemoveEvent_ServiceInfo() {
        ServiceInfo service = new WMSInfoImpl();
        service.setName("MyWMS");
        geoserver.add(service);

        eventsCaptor.clear().start();
        ServiceRemoved event =
                testRemoteRemoveEvent(service, geoserver::remove, ServiceRemoved.class);
        assertThat(event.getWorkspaceId()).isNull();
    }

    @Test
    void testConfigRemoveEvent_ServiceInfo_Workspace() {
        ServiceInfo service = new WMSInfoImpl();
        service.setName("WMS");
        service.setWorkspace(testData.workspaceC);
        geoserver.add(service);

        eventsCaptor.clear().start();
        ServiceRemoved event =
                testRemoteRemoveEvent(service, geoserver::remove, ServiceRemoved.class);

        assertThat(event.getWorkspaceId()).isEqualTo(testData.workspaceC.getId());
    }

    @Test
    void testGeoServerInfoMetadatamapWithCogSettings() {
        GeoServerInfo global = geoserver.getGlobal();
        assertNotNull(global);

        CogSettings cogSettings = new CogSettings();
        cogSettings.setRangeReaderSettings(RangeReaderType.S3);
        cogSettings.setUseCachingStream(true);

        Patch patch =
                testConfigInfoModifyEventNoEquals(
                        global,
                        gs -> gs.getMetadata().put("cogSettings", cogSettings),
                        geoserver::save);

        Map<?, ?> md = (Map<?, ?>) patch.get("metadata").orElseThrow().getValue();
        CogSettings settings = (CogSettings) md.get("cogSettings");
        assertThat(settings).isNotNull();
        assertThat(settings.isUseCachingStream()).isTrue();
        assertThat(settings.getRangeReaderSettings()).isEqualTo(RangeReaderType.S3);
    }
}
