/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.datadirectory;

import lombok.NonNull;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.CatalogPluginStyleResourcePersister;
import org.geoserver.catalog.plugin.locking.LockingCatalog;
import org.geoserver.catalog.plugin.locking.LockingGeoServer;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerConfigPersister;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerResourcePersister;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.security.GeoServerSecurityManager;

/**
 * Extends the parallel
 * {@link org.geoserver.catalog.datadir.DataDirectoryGeoServerLoader} to account
 * for extra care needed on a cloud environment with potentially multiple
 * instances starting up at the same time.
 *
 * <p>
 * For instance:
 *
 * <ul>
 * <li>Uses {@link LockingCatalog} and {@link LockingGeoServer}
 * <li>Uses the gs-cloud specific {@link UpdateSequence} abstraction to check if
 * the config changed during load. In the future such event might imply a
 * reload.
 * <li>Runs {@link #initializeDefaultStyles} within a cluster-wide
 * {@link GeoServerConfigurationLock}
 * <li>Persists the {@link ServiceInfo}s created that were missing, within a
 * {@link GeoServerConfigurationLock} (otherwise, for example, starting from an
 * empty data directory (or if any ServiceInfo config is missing), there's a
 * race condition where each service instance will end up with service infos
 * with different ids for the same service).
 * <li>Starting from an empty data directory, sets {@link GeoServer#setGlobal
 * global} and {@link GeoServer#setLogging logging}, within a
 * {@link GeoServerConfigurationLock}
 * </ul>
 *
 * @since 1.0
 * @see DataDirectoryGeoServerLoader
 */
public class ParallelDataDirectoryGeoServerLoader extends org.geoserver.config.datadir.DataDirectoryGeoServerLoader {

    private @NonNull Catalog rawCatalog;
    private @NonNull LockingGeoServer lockingGeoserver;
    private final DataDirectoryLoaderSupport support;

    public ParallelDataDirectoryGeoServerLoader( //
            @NonNull GeoServerDataDirectory dataDirectory, //
            @NonNull LockingGeoServer geoserver, //
            @NonNull Catalog rawCatalog, //
            @NonNull GeoServerSecurityManager securityManager) {

        super(dataDirectory, securityManager, geoserver.getConfigurationLock());
        this.support = new DataDirectoryLoaderSupport(dataDirectory.getResourceLoader());
        this.rawCatalog = rawCatalog;
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
        // disable locking just on the GeoServer mutating operations while loading the config
        lockingGeoserver.disableLocking();
        try {
            super.loadGeoServer(geoServer, xp);
            support.replaceCatalogInfoPersisterWithFixedVersion(geoServer, xp);
        } finally {
            lockingGeoserver.enableLocking();
        }
    }
}
