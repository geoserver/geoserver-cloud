/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.requestcache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import java.util.stream.Stream;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.LoggingInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RequestCachingGeoServerFacadeTest {

    private final GeoServerFacade subject = mock(GeoServerFacade.class);
    private final RequestCachingGeoServerFacade facade = new RequestCachingGeoServerFacade(subject);

    private final WorkspaceInfo topp = workspace("topp");
    private final WorkspaceInfo sf = workspace("sf");

    @AfterEach
    void closeScope() {
        RequestCacheScope.close();
    }

    // the getters of a proxy write to it: two threads of the request must not read one proxy at once
    @Test
    void eachReadGetsItsOwnProxy() {
        RequestCacheScope.open(100);
        GeoServerInfo global = new GeoServerInfoImpl();
        when(subject.getGlobal()).thenAnswer(invocation -> ModificationProxy.create(global, GeoServerInfo.class));

        GeoServerInfo first = facade.getGlobal();
        GeoServerInfo second = facade.getGlobal();

        assertThat(first).isNotSameAs(second);
        assertThat(ModificationProxy.handler(first)).isNotNull();
        assertThat(ModificationProxy.handler(second)).isNotNull();
        assertThat(ModificationProxy.unwrap(first)).isSameAs(global);
        assertThat(ModificationProxy.unwrap(second)).isSameAs(global);
        verify(subject, times(1)).getGlobal();
    }

    @Test
    void setGlobalEvicts() {
        RequestCacheScope.open(100);
        GeoServerInfo global = new GeoServerInfoImpl();
        when(subject.getGlobal()).thenReturn(global);
        facade.getGlobal();
        facade.getGlobal();

        facade.setGlobal(global);
        facade.getGlobal();

        verify(subject, times(2)).getGlobal();
    }

    // the subject answers null: the second lookup finds the cached miss
    @ParameterizedTest
    @MethodSource("lookups")
    void lookupCachedPerRequest(Consumer<GeoServerFacade> lookup) {
        RequestCacheScope.open(100);

        lookup.accept(facade);
        lookup.accept(facade);

        lookup.accept(verify(subject, times(1)));
    }

    static Stream<Named<Consumer<GeoServerFacade>>> lookups() {
        WorkspaceInfo topp = workspace("topp");
        return Stream.of(
                lookup("getGlobal", GeoServerFacade::getGlobal),
                lookup("getLogging", GeoServerFacade::getLogging),
                lookup("getSettings", target -> target.getSettings(topp)),
                lookup("getService", target -> target.getService(WMSInfo.class)),
                lookup("getService in workspace", target -> target.getService(topp, WMSInfo.class)),
                lookup("getService by id", target -> target.getService("wms-id", ServiceInfo.class)),
                lookup("getServiceByName", target -> target.getServiceByName("WMS", WMSInfo.class)),
                lookup("getServiceByName in workspace", target -> target.getServiceByName("WMS", topp, WMSInfo.class)));
    }

    // the subject answers null: the reads before the write find the cached miss
    @ParameterizedTest(name = "{0}")
    @MethodSource("writes")
    void writeEvictsLookup(Consumer<GeoServerFacade> read, Consumer<GeoServerFacade> write) {
        RequestCacheScope.open(100);
        read.accept(facade);
        read.accept(facade);

        write.accept(facade);
        read.accept(facade);

        read.accept(verify(subject, times(2)));
    }

    static Stream<Arguments> writes() {
        WorkspaceInfo topp = workspace("topp");
        WMSInfo wms = wms("wms-id");
        return Stream.of(
                write(
                        "save(GeoServerInfo)",
                        GeoServerFacade::getGlobal,
                        target -> target.save(new GeoServerInfoImpl())),
                write("save(LoggingInfo)", GeoServerFacade::getLogging, target -> target.save(new LoggingInfoImpl())),
                write(
                        "remove(SettingsInfo)",
                        target -> target.getSettings(topp),
                        target -> target.remove(settings(topp))),
                write("add(ServiceInfo)", target -> target.getService(WMSInfo.class), target -> target.add(wms)),
                write(
                        "remove(ServiceInfo)",
                        target -> target.getServiceByName("WMS", WMSInfo.class),
                        target -> target.remove(wms)),
                write(
                        "save(ServiceInfo), global service",
                        target -> target.getService((WorkspaceInfo) null, WMSInfo.class),
                        target -> target.save(wms)));
    }

    // the facade below looks a service up among the global services for a null workspace
    @Test
    void globalServiceKeyShared() {
        RequestCacheScope.open(100);

        facade.getService(WMSInfo.class);
        facade.getService((WorkspaceInfo) null, WMSInfo.class);

        verify(subject, times(1)).getService(WMSInfo.class);
        verify(subject, never()).getService((WorkspaceInfo) null, WMSInfo.class);
    }

    @Test
    void noScopePassesThrough() {
        facade.getGlobal();
        facade.getGlobal();

        verify(subject, times(2)).getGlobal();
    }

    @Test
    void setLoggingEvicts() {
        RequestCacheScope.open(100);
        LoggingInfo logging = new LoggingInfoImpl();
        when(subject.getLogging()).thenReturn(logging);
        facade.getLogging();
        facade.getLogging();

        facade.setLogging(logging);
        facade.getLogging();

        verify(subject, times(2)).getLogging();
    }

    @Test
    void saveServiceEvictsLookups() {
        RequestCacheScope.open(100);
        WMSInfo wms = wms("wms-id");
        when(subject.getService(WMSInfo.class)).thenReturn(wms);
        when(subject.getServiceByName("WMS", WMSInfo.class)).thenReturn(wms);
        facade.getService(WMSInfo.class);
        facade.getService(WMSInfo.class);
        facade.getServiceByName("WMS", WMSInfo.class);
        facade.getServiceByName("WMS", WMSInfo.class);

        facade.save(wms);
        facade.getService(WMSInfo.class);
        facade.getServiceByName("WMS", WMSInfo.class);

        verify(subject, times(2)).getService(WMSInfo.class);
        verify(subject, times(2)).getServiceByName("WMS", WMSInfo.class);
    }

    // the id of a WMS service finds nothing as a WFS service, as in the facade below
    @Test
    void serviceByIdChecksClass() {
        RequestCacheScope.open(100);
        WMSInfo wms = wms("wms-id");
        when(subject.getService("wms-id", ServiceInfo.class)).thenReturn(wms);
        facade.getService("wms-id", ServiceInfo.class);

        WMSInfo asWms = facade.getService("wms-id", WMSInfo.class);
        WFSInfo asWfs = facade.getService("wms-id", WFSInfo.class);

        assertThat(ModificationProxy.unwrap(asWms)).isSameAs(wms);
        assertThat(asWfs).isNull();
        verify(subject).getService("wms-id", ServiceInfo.class);
        verifyNoMoreInteractions(subject);
    }

    // a lookup by a class of service finding nothing does not tell whether the id exists
    @Test
    void typedServiceMissGoesBelow() {
        RequestCacheScope.open(100);

        facade.getService("wms-id", WMSInfo.class);
        facade.getService("wms-id", WMSInfo.class);

        verify(subject, times(2)).getService("wms-id", WMSInfo.class);
    }

    @Test
    void addSettingsEvictsMiss() {
        RequestCacheScope.open(100);
        SettingsInfo settings = settings(topp);
        facade.getSettings(topp);
        facade.getSettings(topp);

        facade.add(settings);
        facade.getSettings(topp);

        verify(subject, times(2)).getSettings(topp);
    }

    // the save may move the settings to another workspace
    @Test
    void saveSettingsEvictsBoth() {
        RequestCacheScope.open(100);
        SettingsInfo settings = ModificationProxy.create(settings(topp), SettingsInfo.class);
        when(subject.getSettings(topp)).thenReturn(settings);
        facade.getSettings(topp);
        facade.getSettings(topp);
        facade.getSettings(sf);
        facade.getSettings(sf);

        settings.setWorkspace(sf);
        facade.save(settings);
        facade.getSettings(topp);
        facade.getSettings(sf);

        verify(subject, times(2)).getSettings(topp);
        verify(subject, times(2)).getSettings(sf);
    }

    private static Named<Consumer<GeoServerFacade>> lookup(String name, Consumer<GeoServerFacade> call) {
        return Named.of(name, call);
    }

    private static Arguments write(String name, Consumer<GeoServerFacade> read, Consumer<GeoServerFacade> write) {
        return Arguments.of(Named.of(name, read), write);
    }

    private static WorkspaceInfo workspace(String name) {
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId(name + "-id");
        workspace.setName(name);
        return workspace;
    }

    private static SettingsInfo settings(WorkspaceInfo workspace) {
        SettingsInfoImpl settings = new SettingsInfoImpl();
        settings.setId(workspace.getName() + "-settings-id");
        settings.setWorkspace(workspace);
        return settings;
    }

    private static WMSInfo wms(String id) {
        WMSInfoImpl wms = new WMSInfoImpl();
        wms.setId(id);
        wms.setName("WMS");
        return wms;
    }
}
