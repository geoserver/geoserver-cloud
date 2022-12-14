/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.datadirectory;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.impl.CatalogModifyEventImpl;
import org.geoserver.catalog.impl.DefaultCatalogFacade;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.config.GeoServerResourcePersister;

/**
 * A {@link GeoServerResourcePersister} that unwraps the {@link CatalogModifyEvent#getSource()}'s
 * from a {@link ModificationProxy} before proceeding with {@link
 * GeoServerResourcePersister#handleModifyEvent super.handleModifyEvent()}, since it works only if
 * the source is the real {@link Info}, as thrown by the legacy {@link
 * DefaultCatalogFacade#beforeSaved}, despite it having the following comment: {@code "// TODO:
 * protect this original object, perhaps with another proxy"}; while {@link CatalogPlugin} fixes it
 * both by using the modification proxy as the source and by taking full responsibility of event
 * dispatching instead of mixing it up between catalog and facade.
 */
class CatalogPluginGeoServerResourcePersister extends GeoServerResourcePersister {

    public CatalogPluginGeoServerResourcePersister(Catalog catalog) {
        super(catalog);
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) {
        CatalogModifyEventImpl e = CatalogPluginGeoServerResourcePersister.withRealSource(event);
        super.handleModifyEvent(e);
    }

    static CatalogModifyEventImpl withRealSource(CatalogModifyEvent event) {
        CatalogInfo source = event.getSource();
        CatalogInfo real = ModificationProxy.unwrap(source);

        CatalogModifyEventImpl e = new CatalogModifyEventImpl();
        e.setSource(real);
        e.setPropertyNames(event.getPropertyNames());
        e.setOldValues(event.getOldValues());
        e.setNewValues(event.getNewValues());
        return e;
    }
}
