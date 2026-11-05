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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.plugin.CatalogPlugin;
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
