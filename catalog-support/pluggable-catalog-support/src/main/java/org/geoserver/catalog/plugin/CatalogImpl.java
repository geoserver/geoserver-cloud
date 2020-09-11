/*
 * (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import java.util.Objects;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.LockingCatalogFacade;
import org.geoserver.platform.GeoServerExtensions;

/**
 * Extends {@link org.geoserver.catalog.impl.CatalogImpl} to allow decorating the {@link
 * CatalogFacade} with an {@link IsolatedCatalogFacade}, and use {@link
 * org.geoserver.catalog.plugin.DefaultCatalogFacade} as the default facade implementation.
 */
@SuppressWarnings("serial")
public class CatalogImpl extends org.geoserver.catalog.impl.CatalogImpl {

    private @Getter @NonNull CatalogFacade rawCatalogFacade;
    private final boolean isolated;

    public static org.geoserver.catalog.impl.CatalogImpl nonIsolated(
            CatalogFacade catalogFacadeImpl) {
        return new CatalogImpl(catalogFacadeImpl, false);
    }

    public static org.geoserver.catalog.impl.CatalogImpl isoLated(CatalogFacade catalogFacadeImpl) {
        return new CatalogImpl(catalogFacadeImpl, true);
    }

    public CatalogImpl() {
        this(new org.geoserver.catalog.plugin.DefaultCatalogFacade());
    }

    public CatalogImpl(CatalogFacade rawCatalogFacade) {
        this(rawCatalogFacade, true);
    }

    private CatalogImpl(CatalogFacade rawCatalogFacade, boolean isolated) {
        Objects.requireNonNull(rawCatalogFacade);
        this.isolated = isolated;
        setFacade(rawCatalogFacade);
        // just to stress out the parent's default constructor is called nonetheless and we need to
        // completely replace its facade
        Objects.requireNonNull(super.resourcePool);
    }

    public @Override void setFacade(CatalogFacade facade) {
        Objects.requireNonNull(facade);
        this.rawCatalogFacade = facade;
        final GeoServerConfigurationLock configurationLock =
                GeoServerExtensions.bean(GeoServerConfigurationLock.class);
        if (configurationLock != null) {
            facade = LockingCatalogFacade.create(facade, configurationLock);
        }
        // wrap the default catalog facade with the facade capable of handling isolated workspaces
        // behavior
        this.facade = isolated ? new IsolatedCatalogFacade(facade) : facade;
        facade.setCatalog(this);
    }
}
