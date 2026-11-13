/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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

class ProxyUtilsTest {

    private CatalogPlugin catalog;
    private ProxyUtils proxyUtils;

    @BeforeEach
    void setUp() {
        catalog = mock(CatalogPlugin.class);
        // ResolvingProxy resolves against the raw catalog of a CatalogImpl
        when(catalog.getRawCatalog()).thenReturn(catalog);
        proxyUtils = new ProxyUtils(() -> catalog, Optional.empty());
    }

    @Test
    void publishedRefUsesGetPublished() {
        LayerGroupInfo group = newGroup("lg1");
        when(catalog.getPublished("lg1", PublishedInfo.class))
                .thenReturn(ModificationProxy.create(group, LayerGroupInfo.class));
        PublishedInfo ref = ResolvingProxy.create("lg1", PublishedInfo.class);

        PublishedInfo resolved = proxyUtils.resolve(ref);

        assertThat(resolved).isSameAs(group);
        verify(catalog, never()).getLayer(any());
        verify(catalog, never()).getLayerByName(anyString());
    }

    @Test
    void publishedRefFallsBackToName() {
        when(catalog.getPublished("ws:roads", PublishedInfo.class)).thenReturn(null);
        LayerInfo roads = newLayer("roads");
        when(catalog.getLayerByName("ws:roads")).thenReturn(roads);

        PublishedInfo ref = ResolvingProxy.create("ws:roads", PublishedInfo.class);

        assertThat(proxyUtils.resolve(ref)).isSameAs(roads);
    }

    @Test
    void groupRefsInTwoLookups() {
        LayerInfo l1 = newLayer("L1");
        LayerGroupInfo g1 = newGroup("G1");
        StyleInfo s1 = newStyle("S1");
        when(catalog.findAllById(any(), eq(PublishedInfo.class))).thenAnswer(call -> Stream.of(l1, g1));
        when(catalog.findAllById(any(), eq(StyleInfo.class))).thenAnswer(call -> Stream.of(s1));
        LayerGroupInfo group = newGroup("lg");
        group.getLayers().add(ResolvingProxy.create("L1", PublishedInfo.class));
        group.getLayers().add(ResolvingProxy.create("G1", PublishedInfo.class));
        group.getLayers().add(null);
        group.getStyles().addAll(Arrays.asList(ResolvingProxy.create("S1", StyleInfo.class), null));

        LayerGroupInfo resolved = proxyUtils.resolve(group);

        assertThat(resolved.getLayers()).containsExactly(l1, g1, null);
        assertThat(resolved.getStyles()).containsExactly(s1, null);
        verify(catalog).findAllById(List.of("L1", "G1"), PublishedInfo.class);
        verify(catalog).findAllById(List.of("S1"), StyleInfo.class);
        verify(catalog, never()).getLayer(any());
        verify(catalog, never()).getStyle(any());
    }

    @Test
    void missingMemberUsesOldPath() {
        when(catalog.findAllById(any(), any())).thenAnswer(call -> Stream.empty());
        LayerInfo l1 = newLayer("L1");
        when(catalog.getLayer("L1")).thenReturn(l1);
        LayerGroupInfo group = newGroup("lg");
        group.getLayers().add(ResolvingProxy.create("L1", PublishedInfo.class));

        assertThat(proxyUtils.resolve(group).getLayers()).containsExactly(l1);
    }

    @Test
    void missingStyleUsesOldPath() {
        when(catalog.findAllById(any(), any())).thenAnswer(call -> Stream.empty());
        StyleInfo s1 = newStyle("S1");
        when(catalog.getStyle("S1")).thenReturn(s1);
        LayerGroupInfo group = newGroup("lg");
        group.getStyles().add(ResolvingProxy.create("S1", StyleInfo.class));

        assertThat(proxyUtils.resolve(group).getStyles()).containsExactly(s1);
    }

    private StyleInfo newStyle(String id) {
        StyleInfoImpl style = new StyleInfoImpl(null);
        style.setId(id);
        style.setName(id);
        return style;
    }

    private LayerGroupInfo newGroup(String id) {
        LayerGroupInfoImpl group = new LayerGroupInfoImpl();
        group.setId(id);
        group.setName(id);
        group.setLayers(new ArrayList<>());
        group.setStyles(new ArrayList<>());
        return group;
    }

    private LayerInfo newLayer(String id) {
        LayerInfoImpl layer = new LayerInfoImpl();
        layer.setId(id);
        return layer;
    }
}
