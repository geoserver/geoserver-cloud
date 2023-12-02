/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import lombok.NonNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;

import java.util.Collection;
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

    @Override
    public T apply(T i) {
        return resolve(i);
    }

    private <I> I resolve(I i) {
        i = null == i ? null : ModificationProxy.unwrap(i);
        if (i instanceof StoreInfo) setCatalog((StoreInfo) i);
        else if (i instanceof ResourceInfo) setCatalog((ResourceInfo) i);
        else if (i instanceof StyleInfo) setCatalog((StyleInfo) i);
        else if (i instanceof PublishedInfo) setCatalog((PublishedInfo) i);
        else if (i instanceof LayerGroupStyle) setCatalog((LayerGroupStyle) i);
        return i;
    }

    private void resolve(Collection<?> list) {
        if (null != list) list.forEach(this::resolve);
    }

    private void setCatalog(@NonNull PublishedInfo i) {
        if (i instanceof LayerInfo) setCatalog((LayerInfo) i);
        else if (i instanceof LayerGroupInfo) setCatalog((LayerGroupInfo) i);
    }

    private void setCatalog(@NonNull LayerInfo i) {
        resolve(i.getResource());
        resolve(i.getDefaultStyle());
        resolve(i.getStyles());
    }

    private void setCatalog(@NonNull LayerGroupInfo i) {
        resolve(i.getRootLayer());
        resolve(i.getRootLayerStyle());
        resolve(i.getLayers());
        resolve(i.getStyles());
        resolve(i.getLayerGroupStyles());
    }

    private void setCatalog(@NonNull LayerGroupStyle i) {

        // TODO: check if this would result in a stack overflow
        // if(null!=i.getLayers())i.getLayers().forEach(this::setCatalog);
        // if(null != i.getStyles())i.getStyles().forEach(this::setCatalog);
    }

    private void setCatalog(@NonNull StoreInfo i) {
        if (i instanceof StoreInfoImpl) ((StoreInfoImpl) i).setCatalog(catalog);
    }

    private void setCatalog(@NonNull ResourceInfo i) {
        i.setCatalog(catalog);
        resolve(i.getStore());
        if (i instanceof WMSLayerInfo) {
            resolve(((WMSLayerInfo) i).getAllAvailableRemoteStyles());
        }
    }

    private void setCatalog(@NonNull StyleInfo i) {
        if (i instanceof StyleInfoImpl) ((StyleInfoImpl) i).setCatalog(catalog);
    }
}
