/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.config.catalog.backend.core;

import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupVisibilityPolicy;
import org.geoserver.catalog.impl.AdvertisedCatalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.LocalWorkspaceCatalog;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.cloud.autoconfigure.security.ConditionalOnGeoServerSecurityDisabled;
import org.geoserver.cloud.autoconfigure.security.ConditionalOnGeoServerSecurityEnabled;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerLoaderProxy;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.ResourceStoreFactory;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.impl.DefaultResourceAccessManager;
import org.geoserver.security.impl.GsCloudLayerGroupContainmentCache;
import org.geoserver.security.impl.LayerGroupContainmentCache;
import org.geoserver.security.impl.NoopLayerGroupContainmentCache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

/**
 * Base configuration to set up the core catalog and configuration geoserver components
 * <p>
 * A specialized catalog back-end, such as datadir and pgconfig, would provide a <code>@Configuration</code>
 * class that implements the marker interface {@link GeoServerBackendConfigurer}, that must provide at least
 * the following beans:
 * <ul>
 *  <li>GeoServerConfigurationLock configurationLock
 *  <li>UpdateSequence updateSequence
 *  <li>ExtendedCatalogFacade catalogFacade
 *  <li>GeoServerLoader geoServerLoaderImpl
 *  <li>GeoServerFacade geoserverFacade
 *  <li>ResourceStore resourceStoreImpl
 *  <li>GeoServerResourceLoader resourceLoader
 * </ul>
 */
// proxyBeanMethods = true required to avoid circular reference exceptions, especially related to
// GeoServerExtensions still being created
@Configuration(proxyBeanMethods = true)
@EnableConfigurationProperties(CatalogProperties.class)
@Slf4j(topic = "org.geoserver.cloud.config.catalog.backend.core")
public class CoreBackendConfiguration {

    /**
     * A {@link GeoServerLoaderProxy} that doesn't act as a BeanPostProcessor
     */
    @Lazy
    @Bean
    CloudGeoServerLoaderProxy geoServerLoader(
            @Qualifier("rawCatalog") CatalogImpl catalog, @Qualifier("geoServer") GeoServer geoserver) {
        return new CloudGeoServerLoaderProxy(catalog, geoserver);
    }

    /**
     * Base catalog
     */
    @ConditionalOnMissingBean(CatalogPlugin.class)
    @DependsOn({"resourceLoader", "catalogFacade"})
    @Bean
    CatalogPlugin rawCatalog(
            GeoServerResourceLoader resourceLoader,
            @Qualifier("catalogFacade") ExtendedCatalogFacade catalogFacade,
            CatalogProperties properties) {

        boolean isolated = properties.isIsolated();
        CatalogPlugin rawCatalog = new CatalogPlugin(catalogFacade, isolated);
        rawCatalog.setResourceLoader(resourceLoader);
        return rawCatalog;
    }

    /**
     * Default implementation of GeoServer global and service configuration manager.
     */
    @ConditionalOnMissingBean(GeoServerImpl.class)
    @Bean(name = "geoServer")
    GeoServerImpl geoServer(
            @Qualifier("catalog") Catalog catalog, @Qualifier("geoserverFacade") GeoServerFacade facade) {
        GeoServerImpl gs = new GeoServerImpl(facade);
        gs.setCatalog(catalog);
        return gs;
    }

    /**
     * Factory for {@link XStreamPersister} instances.
     */
    @Bean
    XStreamPersisterFactory xstreamPersisterFactory() {
        return new XStreamPersisterFactory();
    }

    /**
     * Utility class uses to process GeoServer extension points.
     */
    @Bean
    GeoServerExtensions extensions() {
        return new GeoServerExtensions();
    }

    /**
     * Utility class uses to process GeoServer configuration workflow through
     * external environment variables.
     * <p>
     * Usually provided by gs-main
     */
    @ConditionalOnMissingBean
    @DependsOn("extensions")
    @Bean
    GeoServerEnvironment environments() {
        return new GeoServerEnvironment();
    }

    /**
     * @return {@link SecureCatalogImpl} decorator if {@code properties.isSecure() == true}, {@code
     *     rawCatalog} otherwise.
     */
    @DependsOn({"extensions", "dataDirectory", "accessRulesDao"})
    @ConditionalOnGeoServerSecurityEnabled
    @Bean
    Catalog secureCatalog(@Qualifier("rawCatalog") Catalog rawCatalog, CatalogProperties properties) throws Exception {
        if (properties.isSecure()) {
            return new SecureCatalogImpl(rawCatalog);
        }
        return rawCatalog;
    }

    /**
     * Default implementation of {@link ResourceAccessManager}, loads simple access rules from a properties file or a
     * Properties object
     * <p>
     *      * Added to {@literal gs-main.jar} in 2.22.x as
     *
     * <pre>
     * {@code
     *  <bean id="defaultResourceAccessManager" class="org.geoserver.security.impl.DefaultResourceAccessManager">
     *      <constructor-arg ref="accessRulesDao"/>
     *      <constructor-arg ref="rawCatalog"/>
     *      <property name="groupsCache" ref="layerGroupContainmentCache"/>
     *  </bean>
     * }
     */
    @ConditionalOnMissingBean
    @DependsOn("layerGroupContainmentCache")
    @Bean
    DefaultResourceAccessManager defaultResourceAccessManager( //
            DataAccessRuleDAO dao, //
            @Qualifier("rawCatalog") Catalog rawCatalog,
            LayerGroupContainmentCache layerGroupContainmentCache) {

        DefaultResourceAccessManager accessManager = new DefaultResourceAccessManager(dao, rawCatalog);
        accessManager.setGroupsCache(layerGroupContainmentCache);
        return accessManager;
    }

    /**
     * A cache for layer group containment, it speeds up looking up layer groups
     * containing a particular layer (recursively).
     * <p>
     * Actual {@link LayerGroupContainmentCache}, matches if the config property {@code
     * geoserver.security.layergroup-containmentcache=true}
     *
     * @see #noOpLayerGroupContainmentCache(Catalog)
     */
    @Bean(name = "layerGroupContainmentCache")
    @ConditionalOnGeoServerSecurityEnabled
    @ConditionalOnProperty(
            name = "geoserver.security.layergroup-containmentcache",
            havingValue = "true",
            matchIfMissing = false)
    LayerGroupContainmentCache enabledLayerGroupContainmentCache(@Qualifier("rawCatalog") Catalog rawCatalog) {

        log.info("using {}", GsCloudLayerGroupContainmentCache.class.getSimpleName());
        return new GsCloudLayerGroupContainmentCache(rawCatalog);
    }

    /**
     * The default {@link LayerGroupContainmentCache} is a no-op, matches if the config
     * property {@code
     * geoserver.security.layergroup-containmentcache=false} or is not specified
     *
     * @see #enabledLayerGroupContainmentCache(Catalog)
     */
    @Bean(name = "layerGroupContainmentCache")
    @ConditionalOnGeoServerSecurityEnabled
    @ConditionalOnProperty(
            name = "geoserver.security.layergroup-containmentcache",
            havingValue = "false",
            matchIfMissing = true)
    LayerGroupContainmentCache noOpLayerGroupContainmentCache() {

        log.info("using {}", NoopLayerGroupContainmentCache.class.getSimpleName());
        return new NoopLayerGroupContainmentCache();
    }

    @ConditionalOnGeoServerSecurityDisabled
    @Bean(name = {"catalog", "secureCatalog"})
    @Primary
    Catalog secureCatalogDisabled(@Qualifier("rawCatalog") Catalog rawCatalog) {
        return rawCatalog;
    }

    /**
     * Catalog decorator handling cases when a {@link LocalWorkspace} is set, becomes the primary {@code catalog} bean (i.e. outer-most decorator)
     *
     * @return {@link LocalWorkspaceCatalog} decorator if {@code properties.isLocalWorkspace() ==
     *     true}, {@code advertisedCatalog} otherwise
     */
    @Bean(name = {"catalog", "localWorkspaceCatalog"})
    @Primary
    Catalog localWorkspaceCatalog(
            @Qualifier("advertisedCatalog") Catalog advertisedCatalog, CatalogProperties properties) {
        return properties.isLocalWorkspace() ? new LocalWorkspaceCatalog(advertisedCatalog) : advertisedCatalog;
    }

    /**
     * Filters out the non advertised layers and resources.
     * @return {@link AdvertisedCatalog} decorator if {@code properties.isAdvertised() == true},
     *     {@code secureCatalog} otherwise.
     */
    @Bean
    Catalog advertisedCatalog(@Qualifier("secureCatalog") Catalog secureCatalog, CatalogProperties properties) {
        if (properties.isAdvertised()) {
            AdvertisedCatalog advertisedCatalog = new AdvertisedCatalog(secureCatalog);
            advertisedCatalog.setLayerGroupVisibilityPolicy(LayerGroupVisibilityPolicy.HIDE_NEVER);
            return advertisedCatalog;
        }
        return secureCatalog;
    }

    /**
     * File or Resource access to GeoServer data directory. In addition to paths Catalog objects such as workspace or
     * FeatureTypeInfo can be used to locate resources.
     */
    @Bean
    GeoServerDataDirectory dataDirectory(GeoServerResourceLoader resourceLoader) {
        return new GeoServerDataDirectory(resourceLoader);
    }

    /**
     * Factory for ResourceStore creation. Looks for a resourceStoreImpl bean before falling back to the
     * dataDirectoryResourceStore bean. Used to override ResourceStore implementation if desired.
     */
    @Bean
    ResourceStoreFactory resourceStore() {
        return new ResourceStoreFactory();
    }
}
