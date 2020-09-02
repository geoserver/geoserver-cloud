package org.geoserver.cloud.config.catalog;

import javax.annotation.Nullable;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.LayerGroupVisibilityPolicy;
import org.geoserver.catalog.impl.AdvertisedCatalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.LocalWorkspaceCatalog;
import org.geoserver.cloud.config.datadirectory.DataDirectoryBackendConfigurer;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.GeoServerLoaderProxy;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.ResourceStoreFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

/**
 * Unified provider interface for the complete GeoServer backend storage.
 *
 * <p>If in vanilla GeoServer the default storage is the "data directory", in the "cloud native
 * GeoServer", you need to explicitly enable and configure the backend by means of one of this
 * interface implementations, using traditional spring's externalized configuration properties.
 *
 * <p>An <code>@Configuration</code> annotated class that implements this interface is all that's a
 * catalog/configuration backend "plugin" needs to provide to set up GeoServer's catalog and
 * configuration storage for a particular backend.
 *
 * <p>Such configuration class should be included as a spring-boot autoconfiguration by adding it to
 * the {@code org.springframework.boot.autoconfigure.EnableAutoConfiguration} list of
 * auto-configuration classes in {@code META-INF/spring.factories}, and should implement a means to
 * enable/disable itself based on the required criteria, for example, using {@link
 * ConditionalOnProperty @ConditionalOnProperty}, {@link ConditionalOnClass @ConditionalOnClass},
 * etc.
 *
 * @see DataDirectoryBackendConfigurer
 */
public interface GeoServerBackendConfigurer {

    ApplicationContext getContext();

    @DependsOn({"resourceLoader", "catalogFacade"})
    default @Bean CatalogImpl rawCatalog() {
        GeoServerResourceLoader resourceLoader = resourceLoader();
        CatalogImpl catalog = new org.geoserver.catalog.plugin.CatalogImpl(catalogFacade());
        catalog.setResourceLoader(resourceLoader);
        return catalog;
    }

    default @Bean CatalogFacade catalogFacade() {
        return new org.geoserver.catalog.plugin.DefaultCatalogFacade();
    }

    @DependsOn({"catalog", "geoserverFacade"})
    default @Bean(name = "geoServer") GeoServer geoServer() throws Exception {
        Catalog catalog = catalog();
        GeoServerFacade geoserverFacade = geoserverFacade();
        GeoServerImpl gs = new GeoServerImpl();
        if (geoserverFacade != null) gs.setFacade(geoserverFacade);
        gs.setCatalog(catalog);
        return gs;
    }

    @DependsOn("secureCatalog")
    @ConditionalOnMissingBean(AdvertisedCatalog.class)
    default @Bean AdvertisedCatalog advertisedCatalog() throws Exception {
        Catalog secureCatalog = getContext().getBean("secureCatalog", Catalog.class);
        AdvertisedCatalog advertisedCatalog = new AdvertisedCatalog(secureCatalog);
        advertisedCatalog.setLayerGroupVisibilityPolicy(LayerGroupVisibilityPolicy.HIDE_NEVER);
        return advertisedCatalog;
    }

    @DependsOn("advertisedCatalog")
    @Bean(name = {"localWorkspaceCatalog", "catalog"})
    default LocalWorkspaceCatalog catalog() throws Exception {
        return new LocalWorkspaceCatalog(advertisedCatalog());
    }

    default @Bean @Nullable GeoServerFacade geoserverFacade() throws Exception {
        return null; // new DefaultGeoServerFacade(geoServer());
    }

    @DependsOn("geoServerLoaderImpl")
    default @Bean GeoServerLoaderProxy geoServerLoader() {
        return new GeoServerLoaderProxy(resourceLoader());
    }

    default @Bean GeoServerLoader geoServerLoaderImpl() {
        GeoServerResourceLoader resourceLoader = resourceLoader();
        DefaultGeoServerLoader defaultGeoServerLoader = new DefaultGeoServerLoader(resourceLoader);
        return defaultGeoServerLoader;
    }

    /**
     * {@link ResourceStore} named {@code resourceStoreImpl}, as looked up in the application
     * context by {@link ResourceStoreFactory}. With this, we don't need a bean called
     * "dataDirectoryResourceStore" at all.
     */
    @Bean
    ResourceStore resourceStoreImpl();

    @Bean
    GeoServerResourceLoader resourceLoader();

    // <bean id="dataDirectory" class="org.geoserver.config.GeoServerDataDirectory">
    // <constructor-arg ref="resourceLoader"/>
    // </bean>
    default @Bean GeoServerDataDirectory dataDirectory() {
        return new GeoServerDataDirectory(resourceLoader());
    }

    default @Bean GeoServerExtensions extensions() {
        return new GeoServerExtensions();
    }

    @ConditionalOnMissingBean(name = "xstreamPersisterFactory")
    default @Bean XStreamPersisterFactory xstreamPersisterFactory() {
        return new XStreamPersisterFactory();
    }
}
