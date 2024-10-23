/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.geoserver.catalog.CatalogFacade.ANY_WORKSPACE;
import static org.geoserver.catalog.CatalogFacade.NO_WORKSPACE;
import static org.geoserver.cloud.event.info.ConfigInfoType.FEATURETYPE;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.SneakyThrows;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.event.catalog.CatalogInfoAdded;
import org.geoserver.cloud.event.catalog.CatalogInfoModified;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.cloud.event.catalog.DefaultDataStoreSet;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.verification.VerificationMode;

class CachingCatalogFacadeTest {

    @Mock
    ExtendedCatalogFacade subject;

    @Mock
    CachingCatalogFacadeContainmentSupport supportMock;

    CachingCatalogFacade facade;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        facade = new CachingCatalogFacade(subject, supportMock);
    }

    static VerificationMode once() {
        return VerificationModeFactory.times(1);
    }

    @SuppressWarnings("unchecked")
    private <T> ArgumentCaptor<Callable<T>> loaderCaptor() {
        return ArgumentCaptor.forClass(Callable.class);
    }

    <I extends CatalogInfo> I stub(Class<I> type) {
        String id = type.getSimpleName() + "-id";
        String name = type.getSimpleName() + "-name";
        return stub(type, id, name);
    }

    private <I extends CatalogInfo> I stub(Class<I> type, String id, String name) {
        I info = mock(type);
        when(info.getId()).thenReturn(id);
        if (info instanceof WorkspaceInfo i) {
            when(i.getName()).thenReturn(name);
            when(subject.getWorkspace(id)).thenReturn(i);
        } else if (info instanceof NamespaceInfo i) {
            when(i.getPrefix()).thenReturn(name);
            when(subject.getNamespace(id)).thenReturn(i);
        } else if (info instanceof StoreInfo i) {
            when(i.getName()).thenReturn(name);
            when(subject.getStore(id, StoreInfo.class)).thenReturn(i);
        } else if (info instanceof ResourceInfo i) {
            when(i.getName()).thenReturn(name);
            when(subject.getResource(id, ResourceInfo.class)).thenReturn(i);
        } else if (info instanceof PublishedInfo i) {
            when(i.getName()).thenReturn(name);
            if (info instanceof LayerInfo l) when(subject.getLayer(id)).thenReturn(l);
            else when(subject.getLayerGroup(id)).thenReturn((LayerGroupInfo) i);
        } else if (info instanceof StyleInfo i) {
            when(i.getName()).thenReturn(name);
            when(subject.getStyle(id)).thenReturn(i);
        }
        return info;
    }

    @Test
    void testAdd() {
        assertThrows(NullPointerException.class, () -> facade.add((CatalogInfo) null));
        testAdd(WorkspaceInfo.class);
        testAdd(NamespaceInfo.class);
        testAdd(DataStoreInfo.class);
        testAdd(CoverageStoreInfo.class);
        testAdd(WMSStoreInfo.class);
        testAdd(WMTSStoreInfo.class);
        testAdd(FeatureTypeInfo.class);
        testAdd(CoverageInfo.class);
        testAdd(WMSLayerInfo.class);
        testAdd(WMTSLayerInfo.class);
        testAdd(LayerInfo.class);
        testAdd(LayerGroupInfo.class);
        testAdd(StyleInfo.class);
    }

    @SneakyThrows
    <I extends CatalogInfo> void testAdd(Class<I> type) {
        I info = stub(type);
        facade.add(info);
        ArgumentCaptor<Callable<I>> loaderCaptor = loaderCaptor();
        verify(supportMock, once()).evictAndGet(same(info), loaderCaptor.capture());

        loaderCaptor.getValue().call();
        verify(subject, once()).add(info);
    }

    @Test
    void testUpdate() {
        assertThrows(NullPointerException.class, () -> facade.update(null, mock(Patch.class)));
        assertThrows(NullPointerException.class, () -> facade.update(mock(WorkspaceInfo.class), null));
        testUpdate(WorkspaceInfo.class);
        testUpdate(NamespaceInfo.class);
        testUpdate(DataStoreInfo.class);
        testUpdate(CoverageStoreInfo.class);
        testUpdate(WMSStoreInfo.class);
        testUpdate(WMTSStoreInfo.class);
        testUpdate(FeatureTypeInfo.class);
        testUpdate(CoverageInfo.class);
        testUpdate(WMSLayerInfo.class);
        testUpdate(WMTSLayerInfo.class);
        testUpdate(LayerInfo.class);
        testUpdate(LayerGroupInfo.class);
        testUpdate(StyleInfo.class);
    }

    @SneakyThrows
    <I extends CatalogInfo> void testUpdate(Class<I> type) {
        I info = stub(type);
        String id = Objects.requireNonNull(info.getId());

        String newPrefixedName = "newName";
        String propName = NamespaceInfo.class.equals(type) ? "prefix" : "name";
        Patch patch = new Patch(List.of(new Patch.Property(propName, newPrefixedName)));

        facade = new CachingCatalogFacade(subject, supportMock) {

            @Override
            Optional<String> newPrefixedName(CatalogInfo info, Patch patch) {
                return Optional.of(newPrefixedName);
            }
        };

        facade.update(info, patch);

        ConfigInfoType configInfoType = ConfigInfoType.valueOf(type);
        verify(supportMock, once()).evict(id, newPrefixedName, configInfoType);

        ArgumentCaptor<Callable<I>> loaderCaptor = loaderCaptor();
        verify(supportMock, once()).evictAndGet(same(info), loaderCaptor.capture());

        loaderCaptor.getValue().call();
        verify(subject, once()).update(info, patch);
    }

    @Test
    void testRemove() {
        assertThrows(NullPointerException.class, () -> facade.remove((CatalogInfo) null));
        testRemove(WorkspaceInfo.class);
        testRemove(NamespaceInfo.class);
        testRemove(DataStoreInfo.class);
        testRemove(CoverageStoreInfo.class);
        testRemove(WMSStoreInfo.class);
        testRemove(WMTSStoreInfo.class);
        testRemove(FeatureTypeInfo.class);
        testRemove(CoverageInfo.class);
        testRemove(WMSLayerInfo.class);
        testRemove(WMTSLayerInfo.class);
        testRemove(LayerInfo.class);
        testRemove(LayerGroupInfo.class);
        testRemove(StyleInfo.class);
    }

    @SneakyThrows
    <I extends CatalogInfo> void testRemove(Class<I> type) {
        I info = mock(type);
        facade.remove(info);
        verify(supportMock, once()).evict(same(info));
        verify(subject, once()).remove(info);
    }

    @Test
    void testGetStore() {
        assertThrows(NullPointerException.class, () -> facade.getStore(null, StoreInfo.class));
        assertThrows(NullPointerException.class, () -> facade.getStore("id", null));
        testGetStore(StoreInfo.class);
        testGetStore(DataStoreInfo.class);
        testGetStore(CoverageStoreInfo.class);
        testGetStore(WMSStoreInfo.class);
        testGetStore(WMTSStoreInfo.class);
    }

    @SneakyThrows
    <S extends StoreInfo> void testGetStore(Class<S> type) {
        facade = new CachingCatalogFacade(subject);
        S info = stub(type);
        String id = info.getId();
        when(subject.getStore(id, StoreInfo.class)).thenReturn(info);
        when(subject.getStore(id, type)).thenReturn(info);

        assertSameTimesN(info, () -> facade.getStore(id, StoreInfo.class), 3);
        assertSameTimesN(info, () -> facade.getStore(id, type), 3);
        verify(subject, once()).getStore(id, StoreInfo.class);
        verify(subject, once()).getStore(id, type);
    }

    @Test
    @SneakyThrows
    void testGetDefaultDataStore() {
        assertThrows(NullPointerException.class, () -> facade.getDefaultDataStore(null));

        WorkspaceInfo ws = mock(WorkspaceInfo.class);
        facade.getDefaultDataStore(ws);

        ArgumentCaptor<Callable<DataStoreInfo>> loaderCaptor = loaderCaptor();
        verify(supportMock, once()).getDefaultDataStore(same(ws), loaderCaptor.capture());

        loaderCaptor.getValue().call();
        verify(subject, once()).getDefaultDataStore(ws);
    }

    @Test
    void testSetDefaultDataStore() {
        WorkspaceInfo ws = null;

        assertThrows(NullPointerException.class, () -> facade.setDefaultDataStore(null, null));

        ws = mock(WorkspaceInfo.class);
        facade.setDefaultDataStore(ws, null);

        verify(supportMock, once()).evictDefaultDataStore(ws);
        verify(subject, once()).setDefaultDataStore(ws, null);
    }

    @Test
    void testGetResource() {
        assertThrows(NullPointerException.class, () -> facade.getResource(null, ResourceInfo.class));
        assertThrows(NullPointerException.class, () -> facade.getResource("id", null));
        testGetResource(FeatureTypeInfo.class);
        testGetResource(CoverageInfo.class);
        testGetResource(WMSLayerInfo.class);
        testGetResource(WMTSLayerInfo.class);
    }

    <R extends ResourceInfo> void testGetResource(Class<R> type) {
        facade = new CachingCatalogFacade(subject);
        R info = stub(type);
        String id = info.getId();
        when(subject.getResource(id, ResourceInfo.class)).thenReturn(info);
        when(subject.getResource(id, type)).thenReturn(info);

        assertSameTimesN(info, () -> facade.getResource(id, ResourceInfo.class), 3);
        assertSameTimesN(info, () -> facade.getResource(id, type), 3);
        verify(subject, once()).getResource(id, ResourceInfo.class);
        verify(subject, once()).getResource(id, type);
    }

    @Test
    void testGetResourceByName() {
        assertThrows(NullPointerException.class, () -> facade.getResourceByName(null, "name", ResourceInfo.class));
        assertThrows(
                NullPointerException.class,
                () -> facade.getResourceByName(mock(NamespaceInfo.class), null, ResourceInfo.class));
        assertThrows(
                NullPointerException.class, () -> facade.getResourceByName(mock(NamespaceInfo.class), "name", null));

        facade = new CachingCatalogFacade(subject);
        FeatureTypeInfo info = stub(FeatureTypeInfo.class);
        NamespaceInfo ns = stub(NamespaceInfo.class);

        String name = info.getName();
        when(subject.getResourceByName(ns, name, ResourceInfo.class)).thenReturn(info);
        when(subject.getResourceByName(ns, name, FeatureTypeInfo.class)).thenReturn(info);

        assertSameTimesN(info, () -> facade.getResourceByName(ns, name, ResourceInfo.class), 3);
        assertSameTimesN(info, () -> facade.getResourceByName(ns, name, FeatureTypeInfo.class), 3);
        verify(subject, once()).getResourceByName(ns, name, ResourceInfo.class);
        verify(subject, once()).getResourceByName(ns, name, FeatureTypeInfo.class);
    }

    @Test
    void testGetResourceByName_ignore_no_namespace() {
        facade = new CachingCatalogFacade(subject);
        FeatureTypeInfo info = stub(FeatureTypeInfo.class);
        NamespaceInfo ns = CatalogFacade.ANY_NAMESPACE;
        String name = info.getName();

        when(subject.getResourceByName(ns, name, ResourceInfo.class)).thenReturn(info);

        assertSameTimesN(info, () -> facade.getResourceByName(ns, name, ResourceInfo.class), 3);
        verify(subject, times(3)).getResourceByName(ns, name, ResourceInfo.class);
    }

    @Test
    void testGetLayer() {
        facade = new CachingCatalogFacade(subject);
        assertThrows(NullPointerException.class, () -> facade.getLayer(null));
        LayerInfo info = stub(LayerInfo.class);
        String id = info.getId();
        when(subject.getLayer(id)).thenReturn(info);

        assertSameTimesN(info, () -> facade.getLayer(id), 3);
        verify(subject, once()).getLayer(id);
    }

    @Test
    void testGetLayerByName() {
        facade = new CachingCatalogFacade(subject);
        assertThrows(NullPointerException.class, () -> facade.getLayerByName(null));

        LayerInfo info = stub(LayerInfo.class);
        when(subject.getLayerByName(info.getName())).thenReturn(info);

        assertSameTimesN(info, () -> facade.getLayerByName(info.getName()), 3);
        verify(subject, once()).getLayerByName(info.getName());
    }

    @Test
    void testGetLayersByResource() {
        facade = new CachingCatalogFacade(subject);
        assertThrows(NullPointerException.class, () -> facade.getLayers((ResourceInfo) null));

        var resource = stub(FeatureTypeInfo.class);
        var layers = List.of(stub(LayerInfo.class));

        when(subject.getLayers(resource)).thenReturn(layers);

        assertThat(facade.getLayers(resource)).isEqualTo(layers);
        assertThat(facade.getLayers(resource)).isEqualTo(layers);

        verify(subject, once()).getLayers(resource);
    }

    @Test
    void testGetLayersByResourceEmptyResultNotCached() {
        facade = new CachingCatalogFacade(subject);
        var resource = stub(FeatureTypeInfo.class);

        when(subject.getLayers(resource)).thenReturn(List.of());
        facade.getLayers(resource);
        facade.getLayers(resource);

        verify(subject, times(2)).getLayers(resource);
    }

    @Test
    void testGetLayerGroup() {
        facade = new CachingCatalogFacade(subject);
        assertThrows(NullPointerException.class, () -> facade.getLayerGroup(null));
        LayerGroupInfo info = stub(LayerGroupInfo.class);
        String id = info.getId();
        when(subject.getLayerGroup(id)).thenReturn(info);

        assertSameTimesN(info, () -> facade.getLayerGroup(id), 3);
        verify(subject, once()).getLayerGroup(id);
    }

    @Test
    void testGetLayerGroupByName() {
        facade = new CachingCatalogFacade(subject);
        assertThrows(NullPointerException.class, () -> facade.getLayerGroupByName(null));

        LayerGroupInfo info = stub(LayerGroupInfo.class);
        when(subject.getLayerGroupByName(info.getName())).thenReturn(info);

        assertSameTimesN(info, () -> facade.getLayerGroupByName(info.getName()), 3);
        verify(subject, once()).getLayerGroupByName(info.getName());
    }

    @Test
    void testGetLayerGroupByNameAndWorkspace() {
        facade = new CachingCatalogFacade(subject);
        var ws = stub(WorkspaceInfo.class);
        assertThrows(NullPointerException.class, () -> facade.getLayerGroupByName(null, "name"));
        assertThrows(NullPointerException.class, () -> facade.getLayerGroupByName(ws, null));

        LayerGroupInfo info = stub(LayerGroupInfo.class);
        when(subject.getLayerGroupByName(ws, info.getName())).thenReturn(info);

        assertSameTimesN(info, () -> facade.getLayerGroupByName(ws, info.getName()), 3);
        verify(subject, once()).getLayerGroupByName(ws, info.getName());
    }

    @Test
    void testGetLayerGroupByNameAndWorkspaceDoesNotCacheNoWorkspace() {
        LayerGroupInfo info = stub(LayerGroupInfo.class);
        String name = info.getName();
        when(subject.getLayerGroupByName(same(ANY_WORKSPACE), eq(name))).thenReturn(info);
        when(subject.getLayerGroupByName(same(NO_WORKSPACE), eq(name))).thenReturn(info);

        assertSameTimesN(info, () -> facade.getLayerGroupByName(ANY_WORKSPACE, name), 3);
        assertSameTimesN(info, () -> facade.getLayerGroupByName(NO_WORKSPACE, name), 3);
        verify(subject, times(3)).getLayerGroupByName(same(ANY_WORKSPACE), eq(name));
        verify(subject, times(3)).getLayerGroupByName(same(ANY_WORKSPACE), eq(name));

        verifyNoInteractions(supportMock);
    }

    @Test
    void testGetDefaultNamespace() {
        facade = new CachingCatalogFacade(subject);
        var info = stub(NamespaceInfo.class);
        when(subject.getDefaultNamespace()).thenReturn(info);

        assertSameTimesN(info, facade::getDefaultNamespace, 3);
        verify(subject, once()).getDefaultNamespace();
    }

    @Test
    void testSetDefaultNamespace() {
        facade.setDefaultNamespace(null);
        verify(supportMock, once()).evictDefaultNamespace();
        verify(subject, once()).setDefaultNamespace(null);
    }

    @Test
    void testGetNamespace() {
        assertThrows(NullPointerException.class, () -> facade.getNamespace(null));
        facade = new CachingCatalogFacade(subject);
        NamespaceInfo info = stub(NamespaceInfo.class);
        String id = info.getId();
        when(subject.getNamespace(id)).thenReturn(info);

        assertSameTimesN(info, () -> facade.getNamespace(id), 3);
        verify(subject, once()).getNamespace(id);
    }

    @Test
    void testGetNamespaceByPrefix() {
        facade = new CachingCatalogFacade(subject);
        assertThrows(NullPointerException.class, () -> facade.getNamespaceByPrefix(null));

        NamespaceInfo info = stub(NamespaceInfo.class);
        when(subject.getNamespaceByPrefix(info.getPrefix())).thenReturn(info);

        assertSameTimesN(info, () -> facade.getNamespaceByPrefix(info.getPrefix()), 3);
        verify(subject, once()).getNamespaceByPrefix(info.getPrefix());
    }

    @Test
    void testGetDefaultWorkspace() {
        facade = new CachingCatalogFacade(subject);
        var info = stub(WorkspaceInfo.class);
        when(subject.getDefaultWorkspace()).thenReturn(info);

        assertSameTimesN(info, facade::getDefaultWorkspace, 3);
        verify(subject, once()).getDefaultWorkspace();
    }

    @Test
    void testSetDefaultWorkspace() {
        facade.setDefaultWorkspace(null);
        verify(supportMock, once()).evictDefaultWorkspace();
        verify(subject, once()).setDefaultWorkspace(null);
    }

    @Test
    void testGetWorkspace() {
        facade = new CachingCatalogFacade(subject);
        assertThrows(NullPointerException.class, () -> facade.getWorkspace(null));
        var info = stub(WorkspaceInfo.class);
        String id = info.getId();
        when(subject.getWorkspace(id)).thenReturn(info);

        assertSameTimesN(info, () -> facade.getWorkspace(id), 3);
        verify(subject, once()).getWorkspace(id);
    }

    @Test
    void testGetWorkspaceByName() {
        facade = new CachingCatalogFacade(subject);
        assertThrows(NullPointerException.class, () -> facade.getWorkspaceByName(null));

        var info = stub(WorkspaceInfo.class);
        when(subject.getWorkspaceByName(info.getName())).thenReturn(info);

        assertSameTimesN(info, () -> facade.getWorkspaceByName(info.getName()), 3);
        verify(subject, once()).getWorkspaceByName(info.getName());
    }

    @Test
    void testGetStyle() {
        facade = new CachingCatalogFacade(subject);
        assertThrows(NullPointerException.class, () -> facade.getStyle(null));
        StyleInfo info = stub(StyleInfo.class);
        String id = info.getId();
        when(subject.getStyle(id)).thenReturn(info);

        assertSameTimesN(info, () -> facade.getStyle(id), 3);
        verify(subject, once()).getStyle(id);
    }

    @Test
    void testGetStyleByName() {
        facade = new CachingCatalogFacade(subject);
        assertThrows(NullPointerException.class, () -> facade.getStyleByName(null));

        var info = stub(StyleInfo.class);
        when(subject.getStyleByName(info.getName())).thenReturn(info);

        assertSameTimesN(info, () -> facade.getStyleByName(info.getName()), 3);
        verify(subject, once()).getStyleByName(info.getName());
    }

    @Test
    void testGetStyleByWorkspaceAndName() {
        facade = new CachingCatalogFacade(subject);
        var ws = stub(WorkspaceInfo.class);
        assertThrows(NullPointerException.class, () -> facade.getStyleByName(null, "name"));
        assertThrows(NullPointerException.class, () -> facade.getStyleByName(ws, null));

        var info = stub(StyleInfo.class);
        when(subject.getStyleByName(ws, info.getName())).thenReturn(info);

        assertSameTimesN(info, () -> facade.getStyleByName(ws, info.getName()), 3);
        verify(subject, once()).getStyleByName(ws, info.getName());
    }

    @Test
    void testGetStyleByWorkspaceAndNameDoesNotCacheNoWorkspace() {
        var info = stub(StyleInfo.class);
        String name = info.getName();
        when(subject.getStyleByName(same(ANY_WORKSPACE), eq(name))).thenReturn(info);
        when(subject.getStyleByName(same(NO_WORKSPACE), eq(name))).thenReturn(info);

        assertSameTimesN(info, () -> facade.getStyleByName(ANY_WORKSPACE, name), 3);
        assertSameTimesN(info, () -> facade.getStyleByName(NO_WORKSPACE, name), 3);
        verify(subject, times(3)).getStyleByName(same(ANY_WORKSPACE), eq(name));
        verify(subject, times(3)).getStyleByName(same(ANY_WORKSPACE), eq(name));

        verifyNoInteractions(supportMock);
    }

    @Test
    void testOnDefaultWorkspaceSet() {
        facade.onDefaultWorkspaceSet();
        verify(supportMock, once()).evictDefaultWorkspace();
    }

    @Test
    void testOnDefaultNamespaceSet() {
        facade.onDefaultNamespaceSet();
        verify(supportMock, once()).evictDefaultNamespace();
    }

    @Test
    void testOnDefaultDataStoreSet() {
        var event = DefaultDataStoreSet.createLocal(1_000L, stub(WorkspaceInfo.class), stub(DataStoreInfo.class));

        facade.onDefaultDataStoreSet(event);
        verify(supportMock, once()).evictDefaultDataStore(eq(event.getWorkspaceId()), any());
    }

    @Test
    void testOnCatalogInfoAdded() {
        var event = event(CatalogInfoAdded.class, "id", FEATURETYPE);
        when(event.getObjectName()).thenReturn("new");

        facade.onCatalogInfoAdded(event);

        verify(supportMock, once()).evict(event.getObjectId(), event.getObjectName(), event.getObjectType());

        when(event.isRemote()).thenReturn(false);
        clearInvocations(supportMock);
        facade.onCatalogInfoAdded(event);
        verifyNoMoreInteractions(supportMock);
    }

    @Test
    void testOnCatalogInfoModified() {
        var event = event(CatalogInfoModified.class, "id", FEATURETYPE);
        when(event.getOldName()).thenReturn("old");
        when(event.getObjectName()).thenReturn("new");

        facade.onCatalogInfoModified(event);

        verify(supportMock, once()).evict(event.getObjectId(), event.getObjectName(), event.getObjectType());
        verify(supportMock, once()).evict(event.getObjectId(), event.getOldName(), event.getObjectType());

        when(event.isRemote()).thenReturn(false);
        clearInvocations(supportMock);
        facade.onCatalogInfoModified(event);
        verifyNoMoreInteractions(supportMock);
    }

    @Test
    void testOnCatalogInfoRemoveEvent() {
        var event = event(CatalogInfoRemoved.class, "id", FEATURETYPE);
        when(event.getObjectName()).thenReturn("new");

        facade.onCatalogInfoRemovedEvent(event);

        verify(supportMock, once()).evict(event.getObjectId(), event.getObjectName(), event.getObjectType());

        when(event.isRemote()).thenReturn(false);
        clearInvocations(supportMock);
        facade.onCatalogInfoRemovedEvent(event);
        verifyNoMoreInteractions(supportMock);
    }

    private <E extends InfoEvent> E event(Class<E> type, String id, ConfigInfoType objectType) {
        E event = mock(type);
        when(event.isRemote()).thenReturn(true);
        when(event.getObjectId()).thenReturn(id);
        when(event.getObjectType()).thenReturn(objectType);
        return event;
    }

    private <T extends Info> void assertSameTimesN(T info, Supplier<T> query, int times) {
        assertSameTimesN(info, id -> query.get(), times);
    }

    private <T extends Info> void assertSameTimesN(T info, Function<String, T> query, int times) {
        for (int i = 0; i < times; i++) {
            String id = Objects.requireNonNull(info.getId());
            T result = query.apply(id);
            assertSame(info, result);
        }
    }
}
