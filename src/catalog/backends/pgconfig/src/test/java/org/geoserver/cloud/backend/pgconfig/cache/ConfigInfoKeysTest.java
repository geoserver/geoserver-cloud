/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.geoserver.cloud.event.info.ConfigInfoType.GEOSERVER;
import static org.geoserver.cloud.event.info.ConfigInfoType.LOGGING;
import static org.geoserver.cloud.event.info.ConfigInfoType.SERVICE;
import static org.geoserver.cloud.event.info.ConfigInfoType.SETTINGS;
import static org.geoserver.cloud.event.info.ConfigInfoType.WORKSPACE;

import java.util.function.Predicate;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey.Kind;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.wms.WMSInfoImpl;
import org.junit.jupiter.api.Test;

// the repository of the configuration and the listener of remote events build their keys here: they must not change
class ConfigInfoKeysTest {

    @Test
    void globalAndLoggingKeys() {
        assertThat(ConfigInfoKeys.global()).isEqualTo(new CacheKey(GEOSERVER, Kind.ENTITY, "global"));
        assertThat(ConfigInfoKeys.logging()).isEqualTo(new CacheKey(LOGGING, Kind.ENTITY, "logging"));
    }

    @Test
    void settingsKeys() {
        assertThat(ConfigInfoKeys.settings("settings-1")).isEqualTo(new CacheKey(SETTINGS, Kind.ENTITY, "settings-1"));
        assertThat(ConfigInfoKeys.settingsByWorkspace("ws-1"))
                .isEqualTo(new CacheKey(SETTINGS, Kind.LOOKUP, "settings:ws-1"));
    }

    @Test
    void serviceKeys() {
        assertThat(ConfigInfoKeys.service("wms-1")).isEqualTo(new CacheKey(SERVICE, Kind.ENTITY, "wms-1"));
        assertThat(ConfigInfoKeys.serviceByName("WMS"))
                .isEqualTo(new CacheKey(SERVICE, Kind.LOOKUP, "service-name:WMS"));
        assertThat(ConfigInfoKeys.serviceByNameAndWorkspace("ws-1", "WMS"))
                .isEqualTo(new CacheKey(SERVICE, Kind.LOOKUP, "service-name:ws-1:WMS"));
    }

    @Test
    void serviceByClassKeys() {
        assertThat(ConfigInfoKeys.serviceByWorkspaceAndClass("ws-1", ServiceInfo.class))
                .isEqualTo(new CacheKey(SERVICE, Kind.LOOKUP, "service:ws-1:ServiceInfo"));
        assertThat(ConfigInfoKeys.serviceByWorkspaceAndClass(null, ServiceInfo.class))
                .isEqualTo(new CacheKey(SERVICE, Kind.LOOKUP, "service:global:ServiceInfo"));
    }

    @Test
    void keysOfSettings() {
        SettingsInfoImpl settings = new SettingsInfoImpl();
        settings.setId("settings-1");
        settings.setWorkspace(workspace("ws-1"));

        assertThat(ConfigInfoKeys.keysOf(settings))
                .containsExactlyInAnyOrder(
                        ConfigInfoKeys.settings("settings-1"), ConfigInfoKeys.settingsByWorkspace("ws-1"));
    }

    // a lookup by class keeps its answer under the class asked by the caller: a service has a key per class
    @Test
    void keysOfWorkspaceService() {
        WMSInfoImpl wms = wms(workspace("ws-1"));

        assertThat(ConfigInfoKeys.keysOf(wms))
                .containsExactlyInAnyOrder(
                        new CacheKey(SERVICE, Kind.ENTITY, "wms-1"),
                        new CacheKey(SERVICE, Kind.LOOKUP, "service:ws-1:WMSInfoImpl"),
                        new CacheKey(SERVICE, Kind.LOOKUP, "service:ws-1:ServiceInfoImpl"),
                        new CacheKey(SERVICE, Kind.LOOKUP, "service:ws-1:WMSInfo"),
                        new CacheKey(SERVICE, Kind.LOOKUP, "service:ws-1:ServiceInfo"),
                        new CacheKey(SERVICE, Kind.LOOKUP, "service-name:WMS"),
                        new CacheKey(SERVICE, Kind.LOOKUP, "service-name:ws-1:WMS"));
    }

    @Test
    void keysOfGlobalService() {
        WMSInfoImpl wms = wms(null);

        assertThat(ConfigInfoKeys.keysOf(wms))
                .containsExactlyInAnyOrder(
                        new CacheKey(SERVICE, Kind.ENTITY, "wms-1"),
                        new CacheKey(SERVICE, Kind.LOOKUP, "service:global:WMSInfoImpl"),
                        new CacheKey(SERVICE, Kind.LOOKUP, "service:global:ServiceInfoImpl"),
                        new CacheKey(SERVICE, Kind.LOOKUP, "service:global:WMSInfo"),
                        new CacheKey(SERVICE, Kind.LOOKUP, "service:global:ServiceInfo"),
                        new CacheKey(SERVICE, Kind.LOOKUP, "service-name:WMS"));
    }

    // a removed workspace takes its settings and services along, and the event does not tell their ids
    @Test
    void settingsAndServicesMatch() {
        Predicate<CacheKey> settingsAndServices = ConfigInfoKeys.settingsAndServices();

        assertThat(settingsAndServices)
                .accepts(
                        ConfigInfoKeys.settings("settings-1"),
                        ConfigInfoKeys.settingsByWorkspace("ws-1"),
                        ConfigInfoKeys.service("wms-1"),
                        ConfigInfoKeys.serviceByName("WMS"))
                .rejects(ConfigInfoKeys.global(), ConfigInfoKeys.logging(), CacheKey.entity(WORKSPACE, "ws-1"));
    }

    // the facade removes a service as it receives it, possibly wrapped in a proxy of another class
    @Test
    void keysOfProxiedService() {
        WMSInfoImpl wms = wms(workspace("ws-1"));
        ServiceInfo proxy = ModificationProxy.create(wms, ServiceInfo.class);

        assertThat(ConfigInfoKeys.keysOf(proxy)).isEqualTo(ConfigInfoKeys.keysOf(wms));
    }

    private static WMSInfoImpl wms(WorkspaceInfo workspace) {
        WMSInfoImpl wms = new WMSInfoImpl();
        wms.setId("wms-1");
        wms.setName("WMS");
        wms.setWorkspace(workspace);
        return wms;
    }

    private static WorkspaceInfo workspace(String id) {
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId(id);
        return workspace;
    }
}
