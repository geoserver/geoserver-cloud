/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FacadeReferenceLookupTest {

    private ExtendedCatalogFacade facade;
    private FacadeReferenceLookup lookup;
    private WorkspaceInfo topp;

    @BeforeEach
    void setUp() {
        facade = mock(ExtendedCatalogFacade.class);
        lookup = new FacadeReferenceLookup(facade);
        topp = new WorkspaceInfoImpl();
    }

    @Test
    void workspaceByIdThenName() {
        when(facade.getWorkspaceByName("topp")).thenReturn(topp);

        assertThat(lookup.resolve(ResolvingProxy.create("topp", WorkspaceInfo.class)))
                .isSameAs(topp);
        verify(facade).getWorkspace("topp");
    }

    @Test
    void namespaceByIdThenPrefix() {
        NamespaceInfo namespace = new NamespaceInfoImpl();
        when(facade.getNamespaceByPrefix("topp")).thenReturn(namespace);

        assertThat(lookup.resolve(ResolvingProxy.create("topp", NamespaceInfo.class)))
                .isSameAs(namespace);
        verify(facade).getNamespace("topp");
    }

    // a feature type holds its store as a data store reference
    @Test
    void storeOfTheProxyType() {
        DataStoreInfo store = new DataStoreInfoImpl(null);
        when(facade.getStore("ds-id", DataStoreInfo.class)).thenReturn(store);

        assertThat(lookup.resolve(ResolvingProxy.create("ds-id", DataStoreInfo.class)))
                .isSameAs(store);
    }

    @Test
    void storeByQualifiedName() {
        StoreInfo store = new DataStoreInfoImpl(null);
        when(facade.getWorkspaceByName("topp")).thenReturn(topp);
        when(facade.getStoreByName(topp, "states", StoreInfo.class)).thenReturn(store);

        assertThat(lookup.resolve(ResolvingProxy.create("topp:states", StoreInfo.class)))
                .isSameAs(store);
    }

    @Test
    void resourceByBareName() {
        ResourceInfo resource = new FeatureTypeInfoImpl(null);
        when(facade.getResourceByName(CatalogFacade.ANY_NAMESPACE, "states", ResourceInfo.class))
                .thenReturn(resource);

        assertThat(lookup.resolve(ResolvingProxy.create("states", ResourceInfo.class)))
                .isSameAs(resource);
    }

    @Test
    void layerByIdThenName() {
        LayerInfo layer = new LayerInfoImpl();
        when(facade.getLayerByName("topp:states")).thenReturn(layer);

        assertThat(lookup.resolve(ResolvingProxy.create("topp:states", LayerInfo.class)))
                .isSameAs(layer);
        verify(facade).getLayer("topp:states");
    }

    @Test
    void groupByQualifiedName() {
        LayerGroupInfo group = new LayerGroupInfoImpl();
        when(facade.getWorkspaceByName("topp")).thenReturn(topp);
        when(facade.getLayerGroupByName(topp, "tasmania")).thenReturn(group);

        assertThat(lookup.resolve(ResolvingProxy.create("topp:tasmania", LayerGroupInfo.class)))
                .isSameAs(group);
    }

    // the prefix of a proxy names the workspace of a style
    @Test
    void styleInProxyWorkspace() {
        StyleInfo style = new StyleInfoImpl(null);
        when(facade.getWorkspaceByName("topp")).thenReturn(topp);
        when(facade.getStyleByName(topp, "roads")).thenReturn(style);

        assertThat(lookup.resolve(ResolvingProxy.create("roads", "topp", StyleInfo.class)))
                .isSameAs(style);
    }

    @Test
    void styleByNameLast() {
        StyleInfo style = new StyleInfoImpl(null);
        when(facade.getStyleByName("roads")).thenReturn(style);

        assertThat(lookup.resolve(ResolvingProxy.create("roads", "topp", StyleInfo.class)))
                .isSameAs(style);
    }

    // one lookup finds a layer or a group
    @Test
    void publishedById() {
        LayerInfo layer = new LayerInfoImpl();
        when(facade.getPublished("l-id")).thenReturn(layer);

        assertThat(lookup.resolve(ResolvingProxy.create("l-id", PublishedInfo.class)))
                .isSameAs(layer);
        verify(facade, never()).getLayer(any());
    }

    // the placeholder of a style group for a missing layer
    @Test
    void emptyReferenceIsNull() {
        assertThat(lookup.resolve(ResolvingProxy.create("", PublishedInfo.class)))
                .isNull();
        verifyNoInteractions(facade);
    }

    @Test
    void notAProxyIsUnchanged() {
        LayerInfo layer = new LayerInfoImpl();

        assertThat(lookup.resolve(layer)).isSameAs(layer);
        verifyNoInteractions(facade);
    }

    @Test
    void notFoundIsNull() {
        assertThat(lookup.resolve(ResolvingProxy.create("nope", LayerInfo.class)))
                .isNull();
    }
}
