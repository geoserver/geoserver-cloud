/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.datadirectory;

import lombok.extern.slf4j.Slf4j;

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
import org.geoserver.config.ServicePersister;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Lock;
import org.geoserver.platform.resource.Resources;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** */
@Slf4j
public class DataDirectoryGeoServerLoader extends DefaultGeoServerLoader {

    public DataDirectoryGeoServerLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader);
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
        final Lock lock = resourceLoader.getLockProvider().acquire("GLOBAL");
        try {

            Set<String> existing = preloadServiceNames(geoServer);
            super.loadGeoServer(geoServer, xp);
            replaceCatalogInfoPersisterWithFixedVersion(geoServer, xp);

            persistNewlyCreatedServices(geoServer, existing);

            initializeEmptyConfig(geoServer);
        } finally {
            lock.release();
        }
    }

    private Set<String> preloadServiceNames(GeoServer gs) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        final List<XStreamServiceLoader<ServiceInfo>> loaders =
                (List) GeoServerExtensions.extensions(XStreamServiceLoader.class);

        return loaders.stream()
                .map(loader -> preload(gs, loader))
                .filter(Objects::nonNull)
                .map(ServiceInfo::getName)
                .collect(Collectors.toSet());
    }

    public final ServiceInfo preload(GeoServer gs, XStreamServiceLoader<?> loader) {
        Resource root = resourceLoader.get("");
        Resource file = root.get(loader.getFilename());

        if (Resources.exists(file)) {
            try {
                return loader.load(gs);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private void persistNewlyCreatedServices(
            GeoServer geoServer, Set<String> existingServiceNames) {

        ServicePersister servicePersister =
                geoServer.getListeners().stream()
                        .filter(ServicePersister.class::isInstance)
                        .map(ServicePersister.class::cast)
                        .findFirst()
                        .orElseThrow();

        geoServer.getServices().stream()
                .filter(s -> !existingServiceNames.contains(s.getName()))
                .peek(s -> log.info("Persisting created service {}", s.getId()))
                .forEach(servicePersister::handlePostServiceChange);
    }

    protected void replaceCatalogInfoPersisterWithFixedVersion(
            final GeoServer geoServer, XStreamPersister xp) {

        ConfigurationListener configPersister =
                geoServer.getListeners().stream()
                        .filter(GeoServerConfigPersister.class::isInstance)
                        .findFirst()
                        .orElse(null);
        if (configPersister != null) {
            geoServer.removeListener(configPersister);
        }

        GeoServerConfigPersister fixedCatalogInfoPersister =
                new CatalogPluginGeoServerConfigPersister(
                        geoServer.getCatalog().getResourceLoader(), xp);

        geoServer.addListener(fixedCatalogInfoPersister);
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
            ServiceInfo serviceInfo = geoServer.getService(l.getServiceClass());
            if (serviceInfo == null) {
                serviceInfo = l.create(geoServer);
                geoServer.add(serviceInfo);
            } else {

            }
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
