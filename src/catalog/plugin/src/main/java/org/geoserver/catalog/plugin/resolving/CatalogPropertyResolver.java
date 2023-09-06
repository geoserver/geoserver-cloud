/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * {@link ResolvingCatalogFacadeDecorator#setObjectResolver resolving function} to set the {@link
 * Catalog} property on {@link CatalogInfo} objects that require it before returning them from the
 * facade.
 *
 * @see StoreInfo#getCatalog()
 * @see ResourceInfo#getCatalog()
 * @see StyleInfoImpl#getCatalog()
 */
public class CatalogPropertyResolver<T extends Info> implements UnaryOperator<T> {

    private Catalog catalog;

    public CatalogPropertyResolver(Catalog catalog) {
        Objects.requireNonNull(catalog);
        this.catalog = catalog;
    }

    public static <I extends Info> CatalogPropertyResolver<I> of(Catalog catalog) {
        return new CatalogPropertyResolver<>(catalog);
    }

    public @Override T apply(T i) {
        if (i instanceof StoreInfo) setCatalog((StoreInfo) i);
        else if (i instanceof ResourceInfo) setCatalog((ResourceInfo) i);
        else if (i instanceof StyleInfo) setCatalog((StyleInfo) i);
        else if (i instanceof PublishedInfo) setCatalog((PublishedInfo) i);
        return i;
    }

    private void setCatalog(PublishedInfo i) {
        if (i instanceof LayerInfo) setCatalog((LayerInfo) i);
        else if (i instanceof LayerGroupInfo) setCatalog((LayerGroupInfo) i);
    }

    private void setCatalog(LayerInfo i) {
        if (null == i) return;
        ResourceInfo resource = i.getResource();
        if (null != resource) {
            if (null == resource.getCatalog()) {
                setCatalog(resource);
            }
            StoreInfo store = resource.getStore();
            if (null != store && null == store.getCatalog()) setCatalog(store);
        }
        setCatalog(i.getDefaultStyle());
        if (i.getStyles() != null) i.getStyles().forEach(this::setCatalog);
    }

    private void setCatalog(LayerGroupInfo i) {
        if (null == i) return;
        if (i.getLayerGroupStyles() != null) i.getLayerGroupStyles().forEach(this::setCatalog);

        if (i.getLayers() != null) i.getLayers().forEach(this::setCatalog);

        setCatalog(i.getRootLayer());
        setCatalog(i.getRootLayerStyle());
    }

    private void setCatalog(LayerGroupStyle i) {
        if (null == i) return;

        // TODO: check if this would result in a stack overflow
        // if(null!=i.getLayers())i.getLayers().forEach(this::setCatalog);
        // if(null != i.getStyles())i.getStyles().forEach(this::setCatalog);
    }

    private void setCatalog(StoreInfo i) {
        if (null == i) return;
        i = ModificationProxy.unwrap(i);
        if (i instanceof StoreInfoImpl) ((StoreInfoImpl) i).setCatalog(catalog);
    }

    private void setCatalog(ResourceInfo i) {
        if (null == i) return;
        i = ModificationProxy.unwrap(i);
        i.setCatalog(catalog);
    }

    private void setCatalog(StyleInfo i) {
        if (null == i) return;
        i = ModificationProxy.unwrap(i);
        if (i instanceof StyleInfoImpl) ((StyleInfoImpl) i).setCatalog(catalog);
    }
}
