/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.datadirectory;

import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.CatalogPluginStyleResourcePersister;
import org.geoserver.catalog.plugin.locking.LockingGeoServer;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerConfigPersister;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerResourcePersister;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.GeoServerSecurityManager;

/**
 * Extends GeoServer's standard
 * {@link org.geoserver.config.datadir.DataDirectoryGeoServerLoader} with
 * minimal customizations for cloud environment compatibility.
 *
 * <p>
 * This class used to implement a custom parallel loading mechanism, but since
 * commit de932a54e8, most customizations are no longer needed as the optimized
 * parallel data directory loader has become the default in upstream GeoServer.
 *
 * <p>
 * Current customizations include:
 *
 * <ul>
 * <li>Uses {@link CatalogPlugin} as the temporary catalog implementation
 * <li>Disables locking during initial configuration loading
 * <li>Uses custom {@link CatalogPluginGeoServerConfigPersister} and
 * {@link CatalogPluginStyleResourcePersister} instead of the standard ones
 * <li>Ensures catalog and GeoServer listeners are properly configured for the
 * plugin catalog
 * </ul>
 *
 * @since 1.0
 * @see DataDirectoryGeoServerLoader
 */
public class CloudDataDirectoryGeoServerLoader extends org.geoserver.config.datadir.DataDirectoryGeoServerLoader {

    private @NonNull LockingGeoServer lockingGeoserver;

    public CloudDataDirectoryGeoServerLoader( //
            @NonNull GeoServerDataDirectory dataDirectory, //
            @NonNull LockingGeoServer geoserver, //
            @NonNull GeoServerSecurityManager securityManager) {

        super(dataDirectory, securityManager, geoserver.getConfigurationLock());
        this.lockingGeoserver = geoserver;
    }

    @Override
    protected CatalogImpl newTemporaryCatalog() {
        return new CatalogPlugin();
    }

    @Override
    public void loadCatalog(Catalog catalog, XStreamPersister xp) throws Exception {
        super.loadCatalog(catalog, xp);

        catalog.removeListeners(GeoServerConfigPersister.class);
        catalog.removeListeners(GeoServerResourcePersister.class);

        catalog.addListener(new CatalogPluginGeoServerConfigPersister(catalog.getResourceLoader(), xp));
        catalog.addListener(new CatalogPluginStyleResourcePersister(catalog));
    }

    @Override
    public void loadGeoServer(GeoServer geoServer, XStreamPersister xp) throws Exception {
        // disable locking just on the GeoServer mutating operations while loading the
        // config
        lockingGeoserver.disableLocking();
        try {
            super.loadGeoServer(geoServer, xp);
            replaceCatalogInfoPersisterWithFixedVersion(geoServer, xp);
        } finally {
            lockingGeoserver.enableLocking();
        }
    }

    private void replaceCatalogInfoPersisterWithFixedVersion(final GeoServer geoServer, XStreamPersister xp) {

        ConfigurationListener configPersister = geoServer.getListeners().stream()
                .filter(GeoServerConfigPersister.class::isInstance)
                .findFirst()
                .orElse(null);
        if (configPersister != null) {
            geoServer.removeListener(configPersister);
        }

        GeoServerConfigPersister fixedCatalogInfoPersister =
                new CatalogPluginGeoServerConfigPersister(geoServer.getCatalog().getResourceLoader(), xp);

        geoServer.addListener(fixedCatalogInfoPersister);
    }
}
