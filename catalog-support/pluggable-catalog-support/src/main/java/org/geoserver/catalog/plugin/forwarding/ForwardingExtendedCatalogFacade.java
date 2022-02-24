/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Query;

import java.util.stream.Stream;

/** Adapts a regular {@link CatalogFacade} to a {@link ExtendedCatalogFacade} */
public class ForwardingExtendedCatalogFacade extends ForwardingCatalogFacade
        implements ExtendedCatalogFacade {

    public ForwardingExtendedCatalogFacade(ExtendedCatalogFacade facade) {
        super(facade);
    }

    public @Override <I extends CatalogInfo> I update(final I info, final Patch patch) {
        return facade().update(info, patch);
    }

    public @Override <T extends CatalogInfo> Stream<T> query(Query<T> query) {
        return facade().query(query);
    }

    protected ExtendedCatalogFacade facade() {
        return (ExtendedCatalogFacade) super.facade;
    }
}
