/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import java.util.Objects;
import java.util.function.Function;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;

/**
 * {@link ResolvingCatalogFacade#setObjectResolver resolving function} to set the {@link Catalog}
 * property on {@link CatalogInfo} objects that require it before returning them from the facade.
 *
 * @see StoreInfo#getCatalog()
 * @see ResourceInfo#getCatalog()
 * @see StyleInfoImpl#getCatalog()
 */
public class CatalogPropertyResolver implements Function<CatalogInfo, CatalogInfo> {

    private Catalog catalog;

    public CatalogPropertyResolver(Catalog catalog) {
        Objects.requireNonNull(catalog);
        this.catalog = catalog;
    }

    public static CatalogPropertyResolver of(Catalog catalog) {
        return new CatalogPropertyResolver(catalog);
    }

    public @Override CatalogInfo apply(CatalogInfo i) {
        if (i instanceof StoreInfo) setCatalog((StoreInfo) i);
        else if (i instanceof ResourceInfo) setCatalog((ResourceInfo) i);
        else if (i instanceof StyleInfo) setCatalog((StyleInfo) i);
        return i;
    }

    private void setCatalog(StoreInfo i) {
        if (i instanceof StoreInfoImpl) ((StoreInfoImpl) i).setCatalog(catalog);
    }

    private void setCatalog(ResourceInfo i) {
        i.setCatalog(catalog);
    }

    private void setCatalog(StyleInfo i) {
        if (i instanceof StyleInfoImpl) ((StyleInfoImpl) i).setCatalog(catalog);
    }
}
