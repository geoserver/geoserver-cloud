/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.pgconfig;

import static org.geoserver.catalog.StyleInfo.DEFAULT_GENERIC;
import static org.geoserver.catalog.StyleInfo.DEFAULT_LINE;
import static org.geoserver.catalog.StyleInfo.DEFAULT_POINT;
import static org.geoserver.catalog.StyleInfo.DEFAULT_POLYGON;
import static org.geoserver.catalog.StyleInfo.DEFAULT_RASTER;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.GeoServerLoaderProxy;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource.Lock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

/**
 * @since 1.4
 */
@Slf4j
public class PgsqlGeoServerLoader extends GeoServerLoader {

    private @NonNull GeoServerConfigurationLock configLock;

    /**
     * @param resourceLoader
     * @param knownServiceTypes know {@link ServiceInfo} types used to initialize a default service
     *     config when starting off an empty config
     */
    public PgsqlGeoServerLoader(
            @NonNull GeoServerResourceLoader resourceLoader,
            @NonNull GeoServerConfigurationLock configLock) {
        super(resourceLoader);
        this.configLock = configLock;
    }

    /** There's no {@link GeoServerLoaderProxy} in gs-cloud */
    public @PostConstruct void load() {
        Catalog rawCatalog = (Catalog) GeoServerExtensions.bean("rawCatalog");
        GeoServer geoserver = (GeoServer) GeoServerExtensions.bean("geoServer");
        postProcessBeforeInitialization(rawCatalog, "rawCatalog");
        postProcessBeforeInitialization(geoserver, "geoServer");
    }

    @Override
    protected void loadCatalog(Catalog catalog, XStreamPersister xp) throws Exception {
        log.info("Loading catalog with pgsql loader...");
    }

    /**
     * Overrides to run inside a lock on "styles" to avoid multiple instances starting up off an
     * empty database trying to create the same default styles, which results in either a startup
     * error or multiple styles named the same.
     */
    @Override
    protected void initializeDefaultStyles(Catalog catalog) throws IOException {
        if (anyStyleMissing(
                catalog,
                DEFAULT_POINT,
                DEFAULT_LINE,
                DEFAULT_POLYGON,
                DEFAULT_RASTER,
                DEFAULT_GENERIC)) {
            final Lock lock = resourceLoader.getLockProvider().acquire("DEFAULT_STYLES");
            try {
                super.initializeDefaultStyles(catalog);
            } finally {
                lock.release();
            }
        }
    }

    private boolean anyStyleMissing(Catalog catalog, String... defaultStyleNames) {
        for (String name : defaultStyleNames) {
            StyleInfo style = catalog.getStyleByName(name);
            if (null == style) return true;
        }
        return false;
    }

    @Override
    protected void loadGeoServer(GeoServer geoServer, XStreamPersister xp) throws Exception {
        log.info("loading geoserver config with pgsql loader");

        // to ensure we have a service configuration for every service we know about
        var missingServices = findMissingServices(geoServer);

        configLock.lock(LockType.READ);
        try {
            GeoServerInfo global = geoServer.getGlobal();
            LoggingInfo logging = geoServer.getLogging();
            boolean someConfigMissing =
                    global == null || logging == null || !missingServices.isEmpty();
            if (someConfigMissing) {
                try {
                    log.info("Found missing config objects, acquiring config lock...");

                    configLock.tryUpgradeLock();
                    log.info(
                            "Config lock acquired. Creating initial GeoServer configuration objects...");

                    doCreateMissing(geoServer, missingServices);

                    log.info("Done creating initial GeoServer configuration objects.");
                } catch (RuntimeException failedUpgrade) {
                    log.info(
                            "Unable to acquire config lock, checking if another instance initialized the config");
                    verifyInitialized(geoServer, missingServices);
                }
            }
        } finally {
            configLock.unlock();
        }
        log.info("GeoServer config loaded.");
    }

    private void verifyInitialized(
            GeoServer geoServer,
            List<XStreamServiceLoader<? extends ServiceInfo>> missingServices) {

        if (geoServer.getGlobal() == null) {
            throw new IllegalStateException("GeoServerInfo not found");
        }
        if (geoServer.getLogging() == null) {
            throw new IllegalStateException("LoggingInfo not found");
        }

        String missing =
                missingServices.stream()
                        .filter(loader -> null == geoServer.getService(loader.getServiceClass()))
                        .map(XStreamServiceLoader::getServiceClass)
                        .map(Class::getName)
                        .collect(Collectors.joining(", "));
        if (!missing.isEmpty()) {
            throw new IllegalStateException("ServiceInfo not found for %s".formatted(missing));
        }
    }

    /** Must run inside a config lock to create missing config objects */
    private void doCreateMissing(
            GeoServer geoServer,
            List<XStreamServiceLoader<? extends ServiceInfo>> missingServices) {

        if (geoServer.getGlobal() == null) {
            log.info("initializing geoserver global config");
            geoServer.setGlobal(geoServer.getFactory().createGlobal());
        }

        if (geoServer.getLogging() == null) {
            log.info("initializing geoserver logging config");
            geoServer.setLogging(geoServer.getFactory().createLogging());
        }
        for (var loader : missingServices) {
            var serviceClass = loader.getServiceClass();
            ServiceInfo service = geoServer.getService(serviceClass);
            if (service == null) {
                log.info("creating default service config for {}", serviceClass.getSimpleName());
                try {
                    service = loader.create(geoServer);
                    geoServer.add(service);
                } catch (Exception e) {
                    log.warn("Error creating default {}", serviceClass, e);
                }
            }
        }
    }

    private List<XStreamServiceLoader<? extends ServiceInfo>> findMissingServices(
            GeoServer geoServer) {

        var loaders = GeoServerExtensions.extensions(XStreamServiceLoader.class);
        var missing = new ArrayList<XStreamServiceLoader<? extends ServiceInfo>>();
        for (XStreamServiceLoader<?> loader : loaders) {
            ServiceInfo service = geoServer.getService(loader.getServiceClass());
            if (service == null) {
                missing.add(loader);
            }
        }
        return missing;
    }
}
