/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.remote.datadir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.cloud.event.UpdateSequenceEvent;
import org.geoserver.cloud.event.catalog.CatalogInfoAdded;
import org.geoserver.cloud.event.catalog.CatalogInfoModified;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.cloud.event.catalog.DefaultDataStoreSet;
import org.geoserver.cloud.event.catalog.DefaultNamespaceSet;
import org.geoserver.cloud.event.catalog.DefaultWorkspaceSet;
import org.geoserver.cloud.event.config.ConfigInfoModified;
import org.geoserver.cloud.event.config.GeoServerInfoModified;
import org.geoserver.cloud.event.config.ServiceAdded;
import org.geoserver.cloud.event.config.ServiceRemoved;
import org.geoserver.cloud.event.config.SettingsAdded;
import org.geoserver.cloud.event.config.SettingsRemoved;
import org.geoserver.cloud.event.info.InfoModified;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.config.plugin.RepositoryGeoServerFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RemoteEventDataDirectoryProcessorTest {

    ExtendedCatalogFacade mockFacade;
    RepositoryGeoServerFacade mockGeoServerFacade;
    RemoteEventDataDirectoryProcessor processor;

    GeoServerInfo global;

    @BeforeEach
    void setUp() {
        mockFacade = mock(ExtendedCatalogFacade.class);
        var catalog = mock(CatalogPlugin.class);
        when(catalog.getFacade()).thenReturn(mockFacade);
        mockGeoServerFacade = mock(RepositoryGeoServerFacade.class);
        global = new GeoServerInfoImpl();
        when(mockGeoServerFacade.getGlobal()).thenReturn(global);
        processor = new RemoteEventDataDirectoryProcessor(mockGeoServerFacade, catalog);
    }

    @Test
    void testRemoteEventDataDirectoryProcessor() {
        RepositoryGeoServerFacade configFacade = mock(RepositoryGeoServerFacade.class);
        CatalogPlugin rawCatalog = mock(CatalogPlugin.class);

        assertThrows(NullPointerException.class, () -> new RemoteEventDataDirectoryProcessor(null, rawCatalog));

        assertThrows(NullPointerException.class, () -> new RemoteEventDataDirectoryProcessor(configFacade, null));

        var catalogFacade = mock(ExtendedCatalogFacade.class);
        when(rawCatalog.getFacade()).thenReturn(catalogFacade);
        var p = new RemoteEventDataDirectoryProcessor(configFacade, rawCatalog);
        assertSame(catalogFacade, p.catalogFacade());
    }

    @Test
    void testOnRemoteUpdateSequenceEvent() {
        global.setUpdateSequence(1);
        UpdateSequenceEvent event = UpdateSequenceEvent.createLocal(10);

        event.setRemote(false);
        processor.onRemoteUpdateSequenceEvent(event);
        verify(mockGeoServerFacade, never()).getGlobal();

        event.setRemote(true);
        processor.onRemoteUpdateSequenceEvent(event);
        assertThat(global.getUpdateSequence()).isEqualTo(10);

        clearInvocations(mockGeoServerFacade);
        event = UpdateSequenceEvent.createLocal(global.getUpdateSequence() - 1);
        event.setRemote(true);
        processor.onRemoteUpdateSequenceEvent(event);
        verify(mockGeoServerFacade, times(1)).getGlobal();
        assertThat(global.getUpdateSequence()).isEqualTo(10);
    }

    @Test
    void testOnRemoteRemoveEvent_CatalogInfo() {
        WorkspaceInfo info = new WorkspaceInfoImpl();
        ((WorkspaceInfoImpl) info).setId("ws1");
        info.setName("workspace1");

        CatalogInfoRemoved event = CatalogInfoRemoved.createLocal(101, info);

        processor.onRemoteRemoveEvent(event);
        verifyNoMoreInteractions(mockFacade);

        Optional<WorkspaceInfo> found = Optional.of(info);
        when(mockFacade.get("ws1", WorkspaceInfo.class)).thenReturn(found);

        clearInvocations(mockFacade);
        event.setRemote(true);
        processor.onRemoteRemoveEvent(event);
        // cast cause it'll call the generic remove method
        verify(mockFacade).remove((CatalogInfo) info);

        clearInvocations(mockFacade);
        when(mockFacade.get("ws1", WorkspaceInfo.class)).thenReturn(Optional.empty());
        processor.onRemoteRemoveEvent(event);
        verify(mockFacade, times(1)).get("ws1", WorkspaceInfo.class);
        verifyNoMoreInteractions(mockFacade);
    }

    @Test
    void testOnRemoteRemoveEvent_ConfigInfo() {
        ServiceInfoImpl service = new ServiceInfoImpl();
        service.setId("wms1");
        service.setName("WMS");
        SettingsInfoImpl settings = new SettingsInfoImpl();
        settings.setId("settings1");
        settings.setWorkspace(new WorkspaceInfoImpl());
        ((WorkspaceInfoImpl) settings.getWorkspace()).setId("ws-id");
        settings.getWorkspace().setName("ws");

        when(mockGeoServerFacade.getService(service.getId(), ServiceInfo.class)).thenReturn(service);
        when(mockGeoServerFacade.getSettings(settings.getId())).thenReturn(settings);

        ServiceRemoved serviceEvent = ServiceRemoved.createLocal(101, service);
        SettingsRemoved settingsEvent = SettingsRemoved.createLocal(102, settings);

        processor.onRemoteRemoveEvent(serviceEvent);
        verifyNoMoreInteractions(mockGeoServerFacade);
        clearInvocations(mockGeoServerFacade);

        processor.onRemoteRemoveEvent(settingsEvent);
        verifyNoMoreInteractions(mockGeoServerFacade);
        clearInvocations(mockGeoServerFacade);

        serviceEvent.setRemote(true);
        settingsEvent.setRemote(true);

        clearInvocations(mockGeoServerFacade);
        processor.onRemoteRemoveEvent(serviceEvent);
        verify(mockGeoServerFacade, times(1)).getService(service.getId(), ServiceInfo.class);
        verify(mockGeoServerFacade, times(1)).remove(service);
        verifyNoMoreInteractions(mockGeoServerFacade);

        clearInvocations(mockGeoServerFacade);
        processor.onRemoteRemoveEvent(settingsEvent);
        verify(mockGeoServerFacade, times(1)).getSettings(settings.getId());
        verify(mockGeoServerFacade, times(1)).remove(settings);
        verifyNoMoreInteractions(mockGeoServerFacade);
    }

    @Test
    void testOnRemoteAddEvent_CatalogInfo() {
        NamespaceInfoImpl info = new NamespaceInfoImpl();
        info.setId("ns1");
        info.setPrefix("ns");
        info.setURI("ns");
        CatalogInfoAdded event = CatalogInfoAdded.createLocal(10, info);

        event.setRemote(false);
        processor.onRemoteAddEvent(event);
        verifyNoMoreInteractions(mockFacade);

        event.setRemote(true);
        processor.onRemoteAddEvent(event);
        verify(mockFacade, times(1)).add((CatalogInfo) info);
    }

    @Test
    void testOnRemoteAddEvent_ServiceInfo() {
        ServiceInfoImpl service = new ServiceInfoImpl();
        service.setId("s1");
        service.setName("S1");
        ServiceAdded event = ServiceAdded.createLocal(0, service);
        event.setRemote(true);
        processor.onRemoteAddEvent(event);
        verify(mockGeoServerFacade, times(1)).add(service);
    }

    @Test
    void testOnRemoteAddEvent_SettingsInfo() {
        SettingsInfoImpl settings = new SettingsInfoImpl();
        settings.setId("settings1");
        settings.setWorkspace(new WorkspaceInfoImpl());
        ((WorkspaceInfoImpl) settings.getWorkspace()).setId("ws-id");
        settings.getWorkspace().setName("ws");

        SettingsAdded event = SettingsAdded.createLocal(0, settings);
        event.setRemote(true);
        processor.onRemoteAddEvent(event);
        verify(mockGeoServerFacade, times(1)).add(settings);
    }

    @Test
    void testOnRemoteModifyEvent_CatalogInfo() {
        WorkspaceInfo info = new WorkspaceInfoImpl();
        ((WorkspaceInfoImpl) info).setId("id");
        info.setName("oldName");
        info = ModificationProxy.create(info, WorkspaceInfo.class);
        info.setName("newName");

        InfoModified event = CatalogInfoModified.createLocal(1, info);
        event.setRemote(false);
        processor.onRemoteModifyEvent(event);
        verifyNoMoreInteractions(mockFacade);

        when(mockFacade.get(info.getId(), WorkspaceInfo.class)).thenReturn(Optional.of(info));
        event.setRemote(true);
        processor.onRemoteModifyEvent(event);
        Patch patch = simplePatch("name", "oldName", "newName");
        verify(mockFacade, times(1)).update(info, patch);
    }

    private Patch simplePatch(String property, Object oldValue, Object newValue) {
        return PropertyDiff.valueOf(List.of(property), List.of(oldValue), List.of(newValue))
                .toPatch();
    }

    @Test
    void testOnRemoteModifyEvent_IgnoresSetDefaultEvents() {
        processor.onRemoteModifyEvent(mock(DefaultWorkspaceSet.class));
        processor.onRemoteModifyEvent(mock(DefaultNamespaceSet.class));
        processor.onRemoteModifyEvent(mock(DefaultDataStoreSet.class));
        verifyNoMoreInteractions(mockFacade);
        verifyNoMoreInteractions(mockGeoServerFacade);
    }

    @Test
    void testOnRemoteModifyEvent_GeoServerInfo() {
        when(mockGeoServerFacade.getGlobal()).thenReturn(ModificationProxy.create(global, GeoServerInfo.class));

        global.setFeatureTypeCacheSize(1);
        var proxied = mockGeoServerFacade.getGlobal();
        proxied.setFeatureTypeCacheSize(1000);
        Patch patch = PropertyDiff.valueOf(ModificationProxy.handler(proxied)).toPatch();

        ConfigInfoModified event = ConfigInfoModified.createLocal(1, global, patch);
        event.setRemote(true);

        processor.onRemoteModifyEvent(event);
        assertThat(global.getFeatureTypeCacheSize()).isEqualTo(1_000);
    }

    @Test
    void testOnRemoteModifyEvent_SettingsInfo() {
        SettingsInfoImpl settings = new SettingsInfoImpl();
        settings.setId("set1");
        settings.setWorkspace(new WorkspaceInfoImpl());
        ((WorkspaceInfoImpl) settings.getWorkspace()).setId("ws1");
        settings.getWorkspace().setName("testws");

        when(mockGeoServerFacade.getSettings(settings.getId()))
                .thenReturn(ModificationProxy.create(settings, SettingsInfo.class));

        settings.setCharset("ISO-8859-1");
        var proxied = mockGeoServerFacade.getSettings(settings.getId());
        proxied.setCharset("UTF-8");
        Patch patch = PropertyDiff.valueOf(ModificationProxy.handler(proxied)).toPatch();

        ConfigInfoModified event = GeoServerInfoModified.createLocal(1, settings, patch);
        event.setRemote(true);

        processor.onRemoteModifyEvent(event);
        assertThat(settings.getCharset()).isEqualTo("UTF-8");
    }

    @Test
    void testOnRemoteModifyEvent_ServiceInfo() {
        ServiceInfoImpl service = new ServiceInfoImpl();
        service.setId("serv1");
        service.setName("S1");

        when(mockGeoServerFacade.getService(service.getId(), ServiceInfo.class))
                .thenReturn(ModificationProxy.create(service, ServiceInfo.class));

        service.setTitle("old title");
        var proxied = mockGeoServerFacade.getService(service.getId(), ServiceInfo.class);
        proxied.setTitle("new title");
        Patch patch = PropertyDiff.valueOf(ModificationProxy.handler(proxied)).toPatch();

        InfoModified event = GeoServerInfoModified.createLocal(1, service, patch);
        event.setRemote(true);

        processor.onRemoteModifyEvent(event);
        assertThat(service.getTitle()).isEqualTo("new title");
    }

    @Test
    void testOnRemoteDefaultWorkspaceEvent() {
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId("ws1");
        workspace.setName("workspace1");

        DefaultWorkspaceSet event = DefaultWorkspaceSet.createLocal(100, workspace);

        // Test local event is ignored
        event.setRemote(false);
        processor.onRemoteDefaultWorkspaceEvent(event);
        verify(mockFacade, never()).setDefaultWorkspace(any());

        // Test remote event with workspace - creates ResolvingProxy with the workspace ID
        event.setRemote(true);
        processor.onRemoteDefaultWorkspaceEvent(event);
        verify(mockFacade, times(1)).setDefaultWorkspace(any(WorkspaceInfo.class));

        // Test remote event with null workspace (unsetting default)
        clearInvocations(mockFacade);
        DefaultWorkspaceSet nullEvent = DefaultWorkspaceSet.createLocal(101, (WorkspaceInfo) null);
        nullEvent.setRemote(true);
        processor.onRemoteDefaultWorkspaceEvent(nullEvent);
        verify(mockFacade, times(1)).setDefaultWorkspace(isNull());
    }

    @Test
    void testOnRemoteDefaultNamespaceEvent() {
        NamespaceInfoImpl namespace = new NamespaceInfoImpl();
        namespace.setId("ns1");
        namespace.setPrefix("ns");
        namespace.setURI("http://example.com/ns");

        DefaultNamespaceSet event = DefaultNamespaceSet.createLocal(100, namespace);

        // Test local event is ignored
        event.setRemote(false);
        processor.onRemoteDefaultNamespaceEvent(event);
        verify(mockFacade, never()).setDefaultNamespace(any());

        // Test remote event with namespace - creates ResolvingProxy with the namespace ID
        event.setRemote(true);
        processor.onRemoteDefaultNamespaceEvent(event);
        verify(mockFacade, times(1)).setDefaultNamespace(any());

        // Test remote event with null namespace (unsetting default)
        clearInvocations(mockFacade);
        DefaultNamespaceSet nullEvent = DefaultNamespaceSet.createLocal(101, (NamespaceInfo) null);
        nullEvent.setRemote(true);
        processor.onRemoteDefaultNamespaceEvent(nullEvent);
        verify(mockFacade, times(1)).setDefaultNamespace(isNull());
    }

    @Test
    void testOnRemoteDefaultDataStoreEvent() {
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId("ws1");
        workspace.setName("workspace1");

        DataStoreInfoImpl dataStore = new DataStoreInfoImpl(null);
        dataStore.setId("ds1");
        dataStore.setName("datastore1");
        dataStore.setWorkspace(workspace);

        DefaultDataStoreSet event = DefaultDataStoreSet.createLocal(100, workspace, dataStore);

        // Test local event is ignored
        event.setRemote(false);
        processor.onRemoteDefaultDataStoreEvent(event);
        verify(mockFacade, never()).setDefaultDataStore(any(), any());

        // Test remote event with data store - creates ResolvingProxy for both workspace and store
        event.setRemote(true);
        processor.onRemoteDefaultDataStoreEvent(event);
        verify(mockFacade, times(1)).setDefaultDataStore(any(WorkspaceInfo.class), any(DataStoreInfo.class));

        // Test remote event with null data store (unsetting default)
        clearInvocations(mockFacade);
        DefaultDataStoreSet nullEvent = DefaultDataStoreSet.createLocal(101, workspace, null);
        nullEvent.setRemote(true);
        processor.onRemoteDefaultDataStoreEvent(nullEvent);
        verify(mockFacade, times(1)).setDefaultDataStore(any(WorkspaceInfo.class), isNull());
    }
}
