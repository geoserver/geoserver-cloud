/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.datadirectory;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupVisibilityPolicy;
import org.geoserver.catalog.impl.AdvertisedCatalog;
import org.geoserver.catalog.impl.LocalWorkspaceCatalog;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.DefaultMemoryCatalogFacade;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.cloud.autoconfigure.bus.ConditionalOnGeoServerRemoteEventsEnabled;
import org.geoserver.cloud.autoconfigure.security.ConditionalOnGeoServerSecurityDisabled;
import org.geoserver.cloud.autoconfigure.security.ConditionalOnGeoServerSecurityEnabled;
import org.geoserver.cloud.config.catalog.CatalogProperties;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.GeoServerLoaderProxy;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.config.plugin.RepositoryGeoServerFacade;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.FileLockProvider;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.wcs.WCSXStreamLoader;
import org.geoserver.wfs.WFSXStreamLoader;
import org.geoserver.wms.WMSXStreamLoader;
import org.geoserver.wps.WPSXStreamLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/** */
@Configuration(proxyBeanMethods = true)
@Slf4j
public class DataDirectoryBackendConfigurer { // implements GeoServerBackendConfigurer {

    // private @Autowired ApplicationContext context;
    // private @Autowired GeoServerBackendProperties configProperties;

    public @PostConstruct void log() {
        log.info("Loading geoserver config backend with {}", getClass().getSimpleName());
    }

    @ConditionalOnGeoServerRemoteEventsEnabled
    public @Bean DataDirectoryRemoteEventProcessor dataDirectoryRemoteEventProcessor() {
        return new DataDirectoryRemoteEventProcessor();
    }

    public /* @Override */ @Bean DefaultMemoryCatalogFacade catalogFacade() {
        return new org.geoserver.catalog.plugin.DefaultMemoryCatalogFacade();
    }

    public /* @Override */ @Bean RepositoryGeoServerFacade geoserverFacade() {
        return new org.geoserver.config.plugin.RepositoryGeoServerFacadeImpl();
    }

    @DependsOn({"extensions", "wmsLoader", "wfsLoader", "wcsLoader", "wpsServiceLoader"})
    public /* @Override */ @Bean GeoServerLoader geoServerLoaderImpl(
            @Autowired GeoServerResourceLoader resourceLoader) {
        return new DataDirectoryGeoServerLoader(resourceLoader);
    }

    @Autowired
    public /* @Override */ @Bean GeoServerResourceLoader resourceLoader(
            @Value("${geoserver.backend.data-directory.location}") Path datadir,
            @Qualifier("resourceStoreImpl") ResourceStore resourceStore) {
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(resourceStore);
        log.debug("geoserver.backend.data-directory.location:" + datadir);
        File dataDirectory = datadir.toFile();
        resourceLoader.setBaseDirectory(dataDirectory);
        return resourceLoader;
    }

    public /* @Override */ @Bean ResourceStore resourceStoreImpl(
            @Value("${geoserver.backend.data-directory.location}") Path path) {
        Objects.requireNonNull(
                path, "geoserver.backend.data-directory.location config property resolves to null");
        File dataDirectory = path.toFile();
        NoServletContextDataDirectoryResourceStore store =
                new NoServletContextDataDirectoryResourceStore(dataDirectory);
        store.setLockProvider(new FileLockProvider(dataDirectory));
        return store;
    }

    /**
     * Provide {@code wmsLoader} if not loaded from {@code
     * gs-wms-<version>.jar!/applicationContext.xml#wmsLoader}
     */
    public @Bean WMSXStreamLoader wmsLoader(GeoServerResourceLoader resourceLoader) {
        return new WMSXStreamLoader(resourceLoader);
    }

    /**
     * Provide {@code wfsLoader} if not loaded from {@code
     * gs-wfs-<version>.jar!/applicationContext.xml#wfsLoader}
     */
    public @Bean WFSXStreamLoader wfsLoader(GeoServerResourceLoader resourceLoader) {
        return new WFSXStreamLoader(resourceLoader);
    }

    /**
     * Provide {@code wcsLoader} if not loaded from {@code
     * gs-wcs-<version>.jar!/applicationContext.xml#wcsLoader}
     */
    public @Bean WCSXStreamLoader wcsLoader(GeoServerResourceLoader resourceLoader) {
        return new WCSXStreamLoader(resourceLoader);
    }

    /**
     * Provide {@code wcsLoader} if not loaded from {@code
     * gs-wps-<version>.jar!/applicationContext.xml#wpsServiceLoader}
     */
    public @Bean WPSXStreamLoader wpsServiceLoader(GeoServerResourceLoader resourceLoader) {
        return new WPSXStreamLoader(resourceLoader);
    }
    /////////////////////////////////

    // REVISIT: @ConfigurationProperties not working
    // @ConfigurationProperties(prefix = "geoserver.catalog")
    public @Bean CatalogProperties geoServerCatalogProperties() {
        return new CatalogProperties();
    }

    @DependsOn("geoServerLoaderImpl")
    public @Bean GeoServerLoaderProxy geoServerLoader(GeoServerResourceLoader resourceLoader) {
        return new GeoServerLoaderProxy(resourceLoader);
    }

    public @Bean GeoServerExtensions extensions() {
        return new GeoServerExtensions();
    }

    @ConditionalOnMissingBean(name = "xstreamPersisterFactory")
    public @Bean XStreamPersisterFactory xstreamPersisterFactory() {
        return new XStreamPersisterFactory();
    }
    ////////////////////////////

    @DependsOn({"resourceLoader", "catalogFacade"})
    public @Bean CatalogPlugin rawCatalog(
            GeoServerResourceLoader resourceLoader,
            @Qualifier("catalogFacade") ExtendedCatalogFacade catalogFacade,
            CatalogProperties properties) {

        boolean isolated = properties.isIsolated();
        CatalogPlugin rawCatalog = new CatalogPlugin(catalogFacade, isolated);
        rawCatalog.setResourceLoader(resourceLoader);
        return rawCatalog;
    }

    /**
     * @return {@link SecureCatalogImpl} decorator if {@code properties.isSecure() == true}, {@code
     *     rawCatalog} otherwise.
     */
    @DependsOn({"extensions", "dataDirectory", "accessRulesDao"})
    @ConditionalOnGeoServerSecurityEnabled
    public @Bean Catalog secureCatalog(
            @Qualifier("rawCatalog") Catalog rawCatalog, CatalogProperties properties)
            throws Exception {
        if (properties.isSecure()) return new SecureCatalogImpl(rawCatalog);
        return rawCatalog;
    }

    @ConditionalOnGeoServerSecurityDisabled
    @Bean(name = {"catalog", "secureCatalog"})
    public Catalog secureCatalogDisabled(@Qualifier("rawCatalog") Catalog rawCatalog) {
        return rawCatalog;
    }

    /**
     * @return {@link AdvertisedCatalog} decorator if {@code properties.isAdvertised() == true},
     *     {@code secureCatalog} otherwise.
     */
    public @Bean Catalog advertisedCatalog(
            @Qualifier("secureCatalog") Catalog secureCatalog, CatalogProperties properties)
            throws Exception {
        if (properties.isAdvertised()) {
            AdvertisedCatalog advertisedCatalog = new AdvertisedCatalog(secureCatalog);
            advertisedCatalog.setLayerGroupVisibilityPolicy(LayerGroupVisibilityPolicy.HIDE_NEVER);
            return advertisedCatalog;
        }
        return secureCatalog;
    }

    /**
     * @return {@link LocalWorkspaceCatalog} decorator if {@code properties.isLocalWorkspace() ==
     *     true}, {@code advertisedCatalog} otherwise
     */
    @Bean(name = {"catalog", "localWorkspaceCatalog"})
    public Catalog localWorkspaceCatalog(
            @Qualifier("advertisedCatalog") Catalog advertisedCatalog, CatalogProperties properties)
            throws Exception {
        return properties.isLocalWorkspace()
                ? new LocalWorkspaceCatalog(advertisedCatalog)
                : advertisedCatalog;
    }
    //////////////////////////

    public @Bean(name = "geoServer") GeoServerImpl geoServer(@Qualifier("catalog") Catalog catalog)
            throws Exception {
        GeoServerFacade facade = geoserverFacade();
        GeoServerImpl gs = new GeoServerImpl(facade);
        gs.setCatalog(catalog);
        return gs;
    }

    // <bean id="dataDirectory" class="org.geoserver.config.GeoServerDataDirectory">
    // <constructor-arg ref="resourceLoader"/>
    // </bean>
    public @Bean GeoServerDataDirectory dataDirectory(GeoServerResourceLoader resourceLoader) {
        return new GeoServerDataDirectory(resourceLoader);
    }
}
