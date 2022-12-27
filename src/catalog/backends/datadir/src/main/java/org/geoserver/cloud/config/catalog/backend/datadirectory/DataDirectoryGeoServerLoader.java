/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.datadirectory;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.catalog.locking.LockingCatalog;
import org.geoserver.cloud.catalog.locking.LockingGeoServer;
import org.geoserver.cloud.catalog.locking.LockingSupport;
import org.geoserver.cloud.config.catalog.backend.core.CoreBackendConfiguration;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerConfigPersister;
import org.geoserver.config.GeoServerLoaderProxy;
import org.geoserver.config.GeoServerResourcePersister;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.config.UpdateSequence;

import java.io.IOException;
import java.util.Set;

import javax.annotation.PostConstruct;

/**
 * {@link Catalog} and {@link GeoServer config} loader for the data-directory backend, loads the
 * configuration from disk as soon as the spring bean wiring is {@link #load() ready}; {@link
 * GeoServerLoaderProxy} is excluded from {@link CoreBackendConfiguration}.
 *
 * <p>Extends the {@link DefaultGeoServerLoader} to account for extra care needed on a cloud
 * environment with potentially multiple instances starting up at the same time.
 *
 * <p>For instance:
 *
 * <ul>
 *   <li>Uses {@link LockingCatalog} and {@link LockingGeoServer}
 *   <li>Uses the gs-cloud specific {@link UpdateSequence} abstraction to check if the config
 *       changed during load. In the future such event might imply a reload.
 *   <li>Runs {@link #initializeDefaultStyles} within a cluster-wide {@link
 *       GeoServerConfigurationLock}
 *   <li>Persists the {@link ServiceInfo}s created that were missing, within a {@link
 *       GeoServerConfigurationLock} (otherwise, for example, starting from an empty data directory
 *       (or if any ServiceInfo config is missing), there's a race condition where each service
 *       instance will end up with service infos with different ids for the same service).
 *   <li>Starting from an empty data directory, sets {@link GeoServer#setGlobal global} and {@link
 *       GeoServer#setLogging logging}, within a {@link GeoServerConfigurationLock}
 * </ul>
 *
 * @since 1.0
 * @see ParallelDataDirectoryGeoServerLoader
 */
@Slf4j
public class DataDirectoryGeoServerLoader extends DefaultGeoServerLoader {

    private final UpdateSequence updateSequence;
    private final Catalog rawCatalog;
    private final LockingGeoServer geoserver;
    private final DataDirectoryLoaderSupport support;

    public DataDirectoryGeoServerLoader( //
            @NonNull UpdateSequence updateSequence,
            @NonNull GeoServerResourceLoader resourceLoader,
            @NonNull LockingGeoServer geoserver,
            @NonNull Catalog rawCatalog) {

        super(resourceLoader);
        this.support = new DataDirectoryLoaderSupport(resourceLoader);
        this.updateSequence = updateSequence;
        this.geoserver = geoserver;
        this.rawCatalog = rawCatalog;
    }

    /** There's no {@link GeoServerLoaderProxy} in gs-cloud */
    public @PostConstruct void load() {
        final long initialSequence = updateSequence.currValue();
        postProcessBeforeInitialization(rawCatalog, "rawCatalog");
        postProcessBeforeInitialization(geoserver, "geoServer");
        final long finalSequence = updateSequence.currValue();
        if (initialSequence != finalSequence) {
            log.warn(
                    "updateSequence changed during startup. Initial value: %,d. Post load value: %,d",
                    initialSequence, finalSequence);
        }
    }

    /**
     * Override to run default styles initialization within a cluster-wide {@link
     * GeoServerConfigurationLock}
     */
    protected @Override void initializeDefaultStyles(Catalog catalog) throws IOException {

        GeoServerConfigurationLock configLock = geoserver.getConfigurationLock();
        LockingSupport lockingSupport = LockingSupport.locking(configLock);

        lockingSupport.callInWriteLock(
                IOException.class,
                () -> {
                    super.initializeDefaultStyles(catalog);
                    return null;
                },
                "initializeDefaultStyles()");
    }

    /**
     * Issues with {@link DefaultGeoServerLoader}:
     *
     * <ul>
     *   <li>Starting from an empty data directory, creates the ServiceInfos, but doesn't persist
     *       them
     *   <li>Starting from an empty data directory, does not set {@link GeoServer#setGlobal global}
     *       and {@link GeoServer#setLogging logging}
     *   <li>Starting from an empty data directory (or if any ServiceInfo config is missing),
     *       there's a race condition where each service instance will end up with service infos
     *       with different ids for the same service.
     * </ul>
     */
    @Override
    protected void loadGeoServer(final GeoServer geoServer, XStreamPersister xp) throws Exception {
        // disable locking just on the GeoServer mutating operations while loading the config
        geoserver.disableLocking();
        try {
            GeoServerConfigurationLock configLock = geoserver.getConfigurationLock();
            LockingSupport lockingSupport = LockingSupport.locking(configLock);

            lockingSupport.callInWriteLock(
                    Exception.class,
                    () -> {
                        Set<String> existing = support.preloadServiceNames(geoServer);
                        super.loadGeoServer(geoServer, xp);
                        support.replaceCatalogInfoPersisterWithFixedVersion(geoServer, xp);

                        support.persistNewlyCreatedServices(geoServer, existing);

                        support.initializeEmptyConfig(geoServer);
                        return null;
                    },
                    "loadGeoServer()");
        } finally {
            geoserver.enableLocking();
        }
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
}
