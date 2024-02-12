/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import lombok.NonNull;

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

    @Override
    public <I extends CatalogInfo> I update(final I info, final Patch patch) {
        return asExtendedFacade().update(info, patch);
    }

    @Override
    public <T extends CatalogInfo> Stream<T> query(Query<T> query) {
        return asExtendedFacade().query(query);
    }

    protected ExtendedCatalogFacade asExtendedFacade() {
        return (ExtendedCatalogFacade) super.facade;
    }

    @Override
    public <T extends CatalogInfo> T add(@NonNull T info) {
        return asExtendedFacade().add(info);
    }

    @Override
    public void remove(@NonNull CatalogInfo info) {
        asExtendedFacade().remove(info);
    }
}
