/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.datadirectory;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerConfigPersister;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.ServicePersister;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;

/**
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
class DataDirectoryLoaderSupport {

    private final @NonNull GeoServerResourceLoader resourceLoader;

    public Set<String> preloadServiceNames(GeoServer gs) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        final List<XStreamServiceLoader<ServiceInfo>> loaders =
                (List) GeoServerExtensions.extensions(XStreamServiceLoader.class);

        return loaders.stream()
                .map(loader -> preload(gs, loader))
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .map(ServiceInfo::getName)
                .collect(Collectors.toSet());
    }

    public Optional<ServiceInfo> preload(GeoServer gs, XStreamServiceLoader<?> loader) {
        Resource root = resourceLoader.get("");
        Resource file = root.get(loader.getFilename());

        if (Resources.exists(file)) {
            try {
                return Optional.of(loader.load(gs));
            } catch (Exception e) {
                log.warn("Error loading service {}", file);
            }
        }
        return Optional.empty();
    }

    public void persistNewlyCreatedServices(GeoServer geoServer, Set<String> existingServiceNames) {

        ServicePersister servicePersister = geoServer.getListeners().stream()
                .filter(ServicePersister.class::isInstance)
                .map(ServicePersister.class::cast)
                .findFirst()
                .orElseThrow();

        geoServer.getServices().stream()
                .filter(s -> !existingServiceNames.contains(s.getName()))
                .forEach(s -> {
                    log.info("Persisting created service {}", s.getId());
                    servicePersister.handlePostServiceChange(s);
                });
    }

    public void replaceCatalogInfoPersisterWithFixedVersion(final GeoServer geoServer, XStreamPersister xp) {

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

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void initializeEmptyConfig(final GeoServer geoServer) {
        // REVISIT: this needs to be pushed upstream?s
        if (geoServer.getGlobal() == null) {
            geoServer.setGlobal(geoServer.getFactory().createGlobal());
        }
        if (geoServer.getLogging() == null) {
            geoServer.setLogging(geoServer.getFactory().createLogging());
        }
        // ensure we have a service configuration for every service we know about
        final List<XStreamServiceLoader> loaders = GeoServerExtensions.extensions(XStreamServiceLoader.class);
        for (XStreamServiceLoader l : loaders) {
            ServiceInfo serviceInfo = geoServer.getService(l.getServiceClass());
            if (serviceInfo == null) {
                serviceInfo = l.create(geoServer);
                geoServer.add(serviceInfo);
            }
        }
    }
}
