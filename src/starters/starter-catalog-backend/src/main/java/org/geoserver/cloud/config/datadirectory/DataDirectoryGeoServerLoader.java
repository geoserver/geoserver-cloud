/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.datadirectory;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.impl.CatalogModifyEventImpl;
import org.geoserver.catalog.impl.DefaultCatalogFacade;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerConfigPersister;
import org.geoserver.config.GeoServerResourcePersister;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource.Lock;

import java.util.List;

/** */
public class DataDirectoryGeoServerLoader extends DefaultGeoServerLoader {

    public DataDirectoryGeoServerLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader);
    }

    @Override
    protected void loadGeoServer(final GeoServer geoServer, XStreamPersister xp) throws Exception {
        super.loadGeoServer(geoServer, xp);

        GeoServerConfigPersister fixedConfigPersister =
                new CatalogPluginGeoServerConfigPersister(
                        geoServer.getCatalog().getResourceLoader(), xp);

        ConfigurationListener configPersister =
                geoServer.getListeners().stream()
                        .filter(GeoServerConfigPersister.class::isInstance)
                        .findFirst()
                        .orElse(null);
        if (configPersister != null) {
            geoServer.removeListener(configPersister);
        }
        geoServer.addListener(fixedConfigPersister);

        initializeEmptyConfig(geoServer);
    }

    @Override
    protected void loadCatalog(Catalog catalog, XStreamPersister xp) throws Exception {
        super.loadCatalog(catalog, xp);

        catalog.removeListeners(GeoServerConfigPersister.class);
        catalog.removeListeners(GeoServerResourcePersister.class);

        catalog.addListener(
                new CatalogPluginGeoServerConfigPersister(catalog.getResourceLoader(), xp));
        catalog.addListener(new CatalogPluginGeoServerResourcePersister(catalog));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void initializeEmptyConfig(final GeoServer geoServer) {
        // TODO: this needs to be pushed upstream
        final Lock lock = resourceLoader.getLockProvider().acquire("GLOBAL");
        try {
            if (geoServer.getGlobal() == null) {
                geoServer.setGlobal(geoServer.getFactory().createGlobal());
            }
            if (geoServer.getLogging() == null) {
                geoServer.setLogging(geoServer.getFactory().createLogging());
            }
            // ensure we have a service configuration for every service we know about
            final List<XStreamServiceLoader> loaders =
                    GeoServerExtensions.extensions(XStreamServiceLoader.class);
            for (XStreamServiceLoader l : loaders) {
                ServiceInfo s = geoServer.getService(l.getServiceClass());
                if (s == null) {
                    geoServer.add(l.create(geoServer));
                }
            }
        } finally {
            lock.release();
        }
    }

    /**
     * A {@link GeoServerConfigPersister} that unwraps the {@link CatalogModifyEvent#getSource()}'s
     * from a {@link ModificationProxy} before proceeding with {@link
     * GeoServerConfigPersister#handleModifyEvent super.handleModifyEvent()}, since it works only if
     * the source is the real {@link Info}, as thrown by the legacy {@link
     * DefaultCatalogFacade#beforeSaved}, despite it having the following comment: {@code "// TODO:
     * protect this original object, perhaps with another proxy"}; while {@link CatalogPlugin} fixes
     * it both by using the modification proxy as the source and by taking full responsibility of
     * event dispatching instead of mixing it up between catalog and facade.
     */
    private static class CatalogPluginGeoServerConfigPersister extends GeoServerConfigPersister {
        public CatalogPluginGeoServerConfigPersister(
                GeoServerResourceLoader rl, XStreamPersister xp) {
            super(rl, xp);
        }

        @Override
        public void handleModifyEvent(CatalogModifyEvent event) {
            CatalogModifyEventImpl e = withRealSource(event);
            super.handleModifyEvent(e);
        }
    }

    /**
     * A {@link GeoServerResourcePersister} that unwraps the {@link
     * CatalogModifyEvent#getSource()}'s from a {@link ModificationProxy} before proceeding with
     * {@link GeoServerResourcePersister#handleModifyEvent super.handleModifyEvent()}, since it
     * works only if the source is the real {@link Info}, as thrown by the legacy {@link
     * DefaultCatalogFacade#beforeSaved}, despite it having the following comment: {@code "// TODO:
     * protect this original object, perhaps with another proxy"}; while {@link CatalogPlugin} fixes
     * it both by using the modification proxy as the source and by taking full responsibility of
     * event dispatching instead of mixing it up between catalog and facade.
     */
    private static class CatalogPluginGeoServerResourcePersister
            extends GeoServerResourcePersister {

        public CatalogPluginGeoServerResourcePersister(Catalog catalog) {
            super(catalog);
        }

        @Override
        public void handleModifyEvent(CatalogModifyEvent event) {
            CatalogModifyEventImpl e = withRealSource(event);
            super.handleModifyEvent(e);
        }
    }

    private static CatalogModifyEventImpl withRealSource(CatalogModifyEvent event) {
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
