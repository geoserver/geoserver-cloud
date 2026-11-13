/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResolvingProxyResolverTest {

    private CatalogPlugin catalog;
    private ResolvingProxyResolver<LayerGroupInfo> resolver;

    private LayerInfo l1;
    private LayerGroupInfo g1;
    private StyleInfo s1;
    private LayerGroupInfo group;

    @BeforeEach
    void setUp() {
        catalog = mock(CatalogPlugin.class);
        // ResolvingProxy resolves against the raw catalog of a CatalogImpl
        when(catalog.getRawCatalog()).thenReturn(catalog);

        l1 = newLayer("L1");
        g1 = newGroup("G1");
        s1 = newStyle("S1");
        lenient().when(catalog.findAllById(any(), eq(PublishedInfo.class))).thenAnswer(call -> Stream.of(l1, g1));
        lenient().when(catalog.findAllById(any(), eq(StyleInfo.class))).thenAnswer(call -> Stream.of(s1));

        group = newGroup("lg");
        group.getLayers().add(ResolvingProxy.create("L1", PublishedInfo.class));
        group.getLayers().add(ResolvingProxy.create("G1", PublishedInfo.class));
        group.getLayers().add(null);
        group.getStyles().addAll(Arrays.asList(ResolvingProxy.create("S1", StyleInfo.class), null, emptyStyleRef()));

        resolver = ResolvingProxyResolver.of(catalog);
    }

    @Test
    void groupRefsInTwoLookups() {
        LayerGroupInfo resolved = resolver.apply(group);

        assertThat(resolved.getLayers()).containsExactly(l1, g1, null);
        assertThat(resolved.getStyles()).containsExactly(s1, null, null);
        verify(catalog).findAllById(List.of("L1", "G1"), PublishedInfo.class);
        verify(catalog).findAllById(List.of("S1"), StyleInfo.class);
        verify(catalog, never()).getLayer(any());
        verify(catalog, never()).getStyle(any());
    }

    @Test
    void membersAreNeverProxied() {
        LayerInfo proxied = ModificationProxy.create(l1, LayerInfo.class);
        when(catalog.findAllById(any(), eq(PublishedInfo.class))).thenAnswer(call -> Stream.of(proxied, g1));

        LayerGroupInfo resolved = resolver.apply(group);

        assertThat(ProxyUtils.isModificationProxy(resolved.getLayers().get(0))).isFalse();
    }

    @Test
    void missingMemberUsesOldPath() {
        when(catalog.findAllById(any(), eq(PublishedInfo.class))).thenAnswer(call -> Stream.of(g1));
        when(catalog.getLayer("L1")).thenReturn(l1);

        assertThat(resolver.apply(group).getLayers().get(0)).isSameAs(l1);
    }

    @Test
    void missingStyleUsesOldPath() {
        when(catalog.findAllById(any(), eq(StyleInfo.class))).thenAnswer(call -> Stream.empty());
        when(catalog.getStyle("S1")).thenReturn(s1);

        assertThat(resolver.apply(group).getStyles().get(0)).isSameAs(s1);
    }

    @Test
    void danglingMemberCallsNotFound() {
        when(catalog.findAllById(any(), eq(PublishedInfo.class))).thenAnswer(call -> Stream.of(g1));
        List<String> notFound = new ArrayList<>();
        resolver = ResolvingProxyResolver.of(catalog, (proxied, proxy) -> notFound.add(proxy.getRef()));
        PublishedInfo dangling = group.getLayers().get(0);

        LayerGroupInfo resolved = resolver.apply(group);

        assertThat(notFound).containsExactly("L1");
        assertThat(resolved.getLayers().get(0)).isSameAs(dangling);
        assertThat(resolved.getLayers().get(1)).isSameAs(g1);
    }

    // the facade returns the resource and the store with proxies, resolved in turn
    @Test
    void resolvesThroughFacadeNotCatalog() {
        ExtendedCatalogFacade facade = mock(ExtendedCatalogFacade.class);
        WorkspaceInfo workspace = newWorkspace("ws");
        DataStoreInfo store = newStore("ds", ResolvingProxy.create("ws", WorkspaceInfo.class));
        ResourceInfo resource = newResource("ft", ResolvingProxy.create("ds", DataStoreInfo.class));
        when(facade.getWorkspace("ws")).thenReturn(workspace);
        when(facade.getStore("ds", DataStoreInfo.class)).thenReturn(store);
        when(facade.getResource("ft", ResourceInfo.class)).thenReturn(resource);
        LayerInfo layer = newLayer("layer");
        layer.setResource(ResolvingProxy.create("ft", ResourceInfo.class));

        LayerInfo resolved = ResolvingProxyResolver.<LayerInfo>of(facade).apply(layer);

        assertThat(resolved.getResource().getStore().getWorkspace()).isSameAs(workspace);
        verifyNoInteractions(catalog);
    }

    @Test
    void facadeLoadsGroupRefsInBulk() {
        ExtendedCatalogFacade facade = mock(ExtendedCatalogFacade.class);
        when(facade.findAllById(any(), eq(PublishedInfo.class))).thenAnswer(call -> Stream.of(l1, g1));
        when(facade.findAllById(any(), eq(StyleInfo.class))).thenAnswer(call -> Stream.of(s1));

        LayerGroupInfo resolved =
                ResolvingProxyResolver.<LayerGroupInfo>of(facade).apply(group);

        assertThat(resolved.getLayers()).containsExactly(l1, g1, null);
        assertThat(resolved.getStyles()).containsExactly(s1, null, null);
        verify(facade).findAllById(List.of("L1", "G1"), PublishedInfo.class);
        verify(facade).findAllById(List.of("S1"), StyleInfo.class);
        verify(facade, never()).getPublished(any());
    }

    // a group found for a member brings its own members with one more call
    @Test
    void facadeResolvesNestedMembers() {
        LayerInfo l2 = newLayer("L2");
        g1.getLayers().add(ResolvingProxy.create("L2", PublishedInfo.class));
        ExtendedCatalogFacade facade = mock(ExtendedCatalogFacade.class);
        when(facade.findAllById(List.of("L1", "G1"), PublishedInfo.class)).thenAnswer(call -> Stream.of(l1, g1));
        when(facade.findAllById(List.of("L2"), PublishedInfo.class)).thenAnswer(call -> Stream.of(l2));
        when(facade.findAllById(any(), eq(StyleInfo.class))).thenAnswer(call -> Stream.of(s1));

        LayerGroupInfo resolved =
                ResolvingProxyResolver.<LayerGroupInfo>of(facade).apply(group);

        LayerGroupInfo nested = (LayerGroupInfo) resolved.getLayers().get(1);
        assertThat(nested.getLayers()).containsExactly(l2);
    }

    @Test
    void facadeFindsMissingMember() {
        ExtendedCatalogFacade facade = mock(ExtendedCatalogFacade.class);
        when(facade.findAllById(any(), eq(PublishedInfo.class))).thenAnswer(call -> Stream.of(g1));
        when(facade.findAllById(any(), eq(StyleInfo.class))).thenAnswer(call -> Stream.of(s1));
        when(facade.getPublished("L1")).thenReturn(l1);

        LayerGroupInfo resolved =
                ResolvingProxyResolver.<LayerGroupInfo>of(facade).apply(group);

        assertThat(resolved.getLayers().get(0)).isSameAs(l1);
    }

    // a row may bring a layer with its style, and the style with its workspace as a proxy
    @Test
    void facadeResolvesInsideObjects() {
        ExtendedCatalogFacade facade = mock(ExtendedCatalogFacade.class);
        WorkspaceInfo workspace = newWorkspace("ws");
        when(facade.getWorkspace("ws")).thenReturn(workspace);
        StyleInfo style = newStyle("style");
        style.setWorkspace(ResolvingProxy.create("ws", WorkspaceInfo.class));
        LayerInfo layer = newLayer("layer");
        layer.setDefaultStyle(style);

        LayerInfo resolved = ResolvingProxyResolver.<LayerInfo>of(facade).apply(layer);

        assertThat(resolved.getDefaultStyle().getWorkspace()).isSameAs(workspace);
    }

    // the facade decodes a new object on each lookup, and the three layers share their store, workspace and style
    @Test
    void sharedReferencesLookedUpOnce() {
        ExtendedCatalogFacade facade = decodingFacade();
        LayerGroupInfo shared = newGroup("shared");
        for (String id : List.of("L1", "L2", "L3")) {
            shared.getLayers().add(ResolvingProxy.create(id, PublishedInfo.class));
            shared.getStyles().add(ResolvingProxy.create("st", StyleInfo.class));
        }

        LayerGroupInfo resolved =
                ResolvingProxyResolver.<LayerGroupInfo>of(facade).apply(shared);

        LayerInfo first = (LayerInfo) resolved.getLayers().get(0);
        LayerInfo last = (LayerInfo) resolved.getLayers().get(2);
        assertThat(last.getResource().getStore()).isSameAs(first.getResource().getStore());
        assertThat(first.getResource().getStore().getWorkspace().getName()).isEqualTo("ws");
        verify(facade).getStore("ds", DataStoreInfo.class);
        verify(facade).getWorkspace("ws");
        verify(facade).getStyle("st");
        verify(facade, never()).findAllById(any(), eq(StyleInfo.class));
    }

    // other threads may read a resolved layer: its styles cannot be modified here, and a rewrite would throw
    @Test
    void resolvedObjectNotRewritten() {
        ExtendedCatalogFacade facade = mock(ExtendedCatalogFacade.class);
        ResourceInfo resource = spy(newResource("ft", newStore("ds", newWorkspace("ws"))));
        LayerInfo layer = spy(resolvedLayer(resource));
        Set<StyleInfo> styles = layer.getStyles();

        ResolvingProxyResolver.<LayerInfo>of(facade).apply(layer);

        verify(layer, never()).setResource(any());
        verify(layer, never()).setDefaultStyle(any());
        verify(resource, never()).setStore(any());
        assertThat(layer.getStyles()).isSameAs(styles);
        verifyNoInteractions(facade);
    }

    /** A facade decoding a new object on each lookup, as the pgconfig facade does. */
    private ExtendedCatalogFacade decodingFacade() {
        ExtendedCatalogFacade facade = mock(ExtendedCatalogFacade.class);
        when(facade.findAllById(any(), eq(PublishedInfo.class)))
                .thenAnswer(call -> Stream.of(rawLayer("L1"), rawLayer("L2"), rawLayer("L3")));
        when(facade.getResource(any(), eq(ResourceInfo.class)))
                .thenAnswer(call -> newResource(call.getArgument(0), ResolvingProxy.create("ds", DataStoreInfo.class)));
        when(facade.getStore("ds", DataStoreInfo.class))
                .thenAnswer(call -> newStore("ds", ResolvingProxy.create("ws", WorkspaceInfo.class)));
        when(facade.getWorkspace("ws")).thenAnswer(call -> newWorkspace("ws"));
        when(facade.getStyle("st")).thenAnswer(call -> newStyle("st"));
        return facade;
    }

    /** A layer as the facade returns it: its resource and its default style are proxies. */
    private LayerInfo rawLayer(String id) {
        LayerInfo layer = newLayer(id);
        layer.setResource(ResolvingProxy.create("ft-" + id, ResourceInfo.class));
        layer.setDefaultStyle(ResolvingProxy.create("st", StyleInfo.class));
        return layer;
    }

    /** A layer with its references resolved. Its styles cannot be modified. */
    private LayerInfo resolvedLayer(ResourceInfo resource) {
        LayerInfoImpl layer = new LayerInfoImpl();
        layer.setId("layer");
        layer.setResource(resource);
        layer.setDefaultStyle(newStyle("st"));
        layer.setStyles(Set.of(newStyle("alt")));
        return layer;
    }

    private WorkspaceInfo newWorkspace(String id) {
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId(id);
        workspace.setName(id);
        return workspace;
    }

    private DataStoreInfo newStore(String id, WorkspaceInfo workspace) {
        DataStoreInfoImpl store = new DataStoreInfoImpl(null, id);
        store.setName(id);
        store.setWorkspace(workspace);
        return store;
    }

    private ResourceInfo newResource(String id, StoreInfo store) {
        FeatureTypeInfoImpl resource = new FeatureTypeInfoImpl(null, id);
        resource.setName(id);
        resource.setStore(store);
        return resource;
    }

    private StyleInfo emptyStyleRef() {
        return ResolvingProxy.create("", StyleInfo.class);
    }

    private LayerGroupInfo newGroup(String id) {
        LayerGroupInfoImpl lg = new LayerGroupInfoImpl();
        lg.setId(id);
        lg.setName(id);
        lg.setLayers(new ArrayList<>());
        lg.setStyles(new ArrayList<>());
        return lg;
    }

    private LayerInfo newLayer(String id) {
        LayerInfoImpl layer = new LayerInfoImpl();
        layer.setId(id);
        return layer;
    }

    private StyleInfo newStyle(String id) {
        StyleInfoImpl style = new StyleInfoImpl(null);
        style.setId(id);
        style.setName(id);
        return style;
    }
}
