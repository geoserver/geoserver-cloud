/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.datadirectory;

import static org.geoserver.catalog.StyleInfo.DEFAULT_GENERIC;
import static org.geoserver.catalog.StyleInfo.DEFAULT_LINE;
import static org.geoserver.catalog.StyleInfo.DEFAULT_POINT;
import static org.geoserver.catalog.StyleInfo.DEFAULT_POLYGON;
import static org.geoserver.catalog.StyleInfo.DEFAULT_RASTER;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import javax.annotation.PostConstruct;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeCallback;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.CatalogPluginStyleResourcePersister;
import org.geoserver.catalog.plugin.locking.LockingCatalog;
import org.geoserver.catalog.plugin.locking.LockingGeoServer;
import org.geoserver.catalog.plugin.locking.LockingSupport;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerConfigPersister;
import org.geoserver.config.GeoServerLoaderProxy;
import org.geoserver.config.GeoServerResourcePersister;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.vfny.geoserver.util.DataStoreUtils;

/**
 * Extends the parallel {@link org.geoserver.catalog.datadir.DataDirectoryGeoServerLoader} to
 * account for extra care needed on a cloud environment with potentially multiple instances starting
 * up at the same time.
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
 * @see DataDirectoryGeoServerLoader
 */
@Slf4j
public class ParallelDataDirectoryGeoServerLoader extends org.geoserver.catalog.datadir.DataDirectoryGeoServerLoader {

    private @NonNull UpdateSequence updateSequence;
    private @NonNull Catalog rawCatalog;
    private @NonNull LockingGeoServer lockingGeoserver;
    private final DataDirectoryLoaderSupport support;

    /**
     * @param resourceLoader
     * @param securityManager
     */
    public ParallelDataDirectoryGeoServerLoader( //
            @NonNull UpdateSequence updateSequence, //
            @NonNull GeoServerResourceLoader resourceLoader, //
            @NonNull LockingGeoServer geoserver, //
            @NonNull Catalog rawCatalog, //
            @NonNull GeoServerSecurityManager securityManager) {

        super(resourceLoader, securityManager);
        this.support = new DataDirectoryLoaderSupport(resourceLoader);
        this.rawCatalog = rawCatalog;
        this.lockingGeoserver = geoserver;
        this.updateSequence = updateSequence;
    }

    @Override
    protected CatalogImpl newTemporaryCatalog() {
        return new CatalogPlugin();
    }

    /**
     * There's no {@link GeoServerLoaderProxy} in gs-cloud
     *
     * @throws FactoryException
     * @throws NoSuchAuthorityCodeException
     */
    public @PostConstruct void load() throws FactoryException {
        triggerCollaboratorBeansLoading();

        final long initialSequence = updateSequence.currValue();
        postProcessBeforeInitialization(rawCatalog, "rawCatalog");
        postProcessBeforeInitialization(lockingGeoserver, "geoServer");
        final long finalSequence = updateSequence.currValue();
        if (initialSequence != finalSequence) {
            log.warn(
                    "updateSequence changed during startup. Initial value: %,d. Post load value: %,d",
                    initialSequence, finalSequence);
        }
    }

    /**
     * @throws FactoryException
     * @throws NoSuchAuthorityCodeException
     */
    private void triggerCollaboratorBeansLoading() throws FactoryException {
        // force initializing the referencing subsystem or can get
        // "java.lang.IllegalArgumentException: NumberSystem
        // tech.units.indriya.function.DefaultNumberSystem not found" when hit in parallel
        org.geotools.referencing.CRS.decode("EPSG:4326");

        // misconfigured layers may end up calling FeatureTypeInfo.getFeatureType(), which in turn
        // will trigger GeoServerExtensions and deadlock on the main thread's while spring is
        // building up beans
        DataStoreUtils.getAvailableDataStoreFactories().forEach(f -> {
            try {
                DataStoreUtils.aquireFactory(f.getDisplayName());
            } catch (Exception ignore) {
                //
            }
        });

        GeoServerExtensions.extensions(FeatureTypeCallback.class);
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
            final Set<String> existing = support.preloadServiceNames(geoServer);
            super.loadGeoServer(geoServer, xp);
            support.replaceCatalogInfoPersisterWithFixedVersion(geoServer, xp);

            GeoServerConfigurationLock configLock = lockingGeoserver.getConfigurationLock();
            LockingSupport lockingSupport = LockingSupport.locking(configLock);

            lockingSupport.callInWriteLock(
                    Exception.class,
                    () -> {
                        support.persistNewlyCreatedServices(geoServer, existing);
                        support.initializeEmptyConfig(geoServer);
                        return null;
                    },
                    "loadGeoServer()");
        } finally {
            lockingGeoserver.enableLocking();
        }
    }

    /**
     * Override to run default styles initialization within a cluster-wide {@link
     * GeoServerConfigurationLock}
     */
    @Override
    protected void initializeDefaultStyles(Catalog catalog) throws IOException {
        if (styleMissing(catalog, DEFAULT_POINT, DEFAULT_LINE, DEFAULT_POLYGON, DEFAULT_RASTER, DEFAULT_GENERIC)) {

            log.info("Initializing default styles");
            GeoServerConfigurationLock configLock = lockingGeoserver.getConfigurationLock();
            LockingSupport lockingSupport = LockingSupport.locking(configLock);

            lockingSupport.callInWriteLock(
                    IOException.class,
                    () -> {
                        super.initializeDefaultStyles(catalog);
                        return null;
                    },
                    "initializeDefaultStyles()");
        } else {
            log.debug("Default styles already present");
        }
    }

    private boolean styleMissing(Catalog catalog, String... styleNames) {
        return Arrays.stream(styleNames).anyMatch(name -> null == catalog.getStyleByName(name));
    }
}
