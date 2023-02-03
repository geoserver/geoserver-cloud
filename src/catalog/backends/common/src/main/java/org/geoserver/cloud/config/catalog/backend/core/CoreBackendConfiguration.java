/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.core;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupVisibilityPolicy;
import org.geoserver.catalog.impl.AdvertisedCatalog;
import org.geoserver.catalog.impl.LocalWorkspaceCatalog;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.cloud.autoconfigure.security.ConditionalOnGeoServerSecurityDisabled;
import org.geoserver.cloud.autoconfigure.security.ConditionalOnGeoServerSecurityEnabled;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.impl.DefaultResourceAccessManager;
import org.geoserver.security.impl.LayerGroupContainmentCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

// proxyBeanMethods = true required to avoid circular reference exceptions, especially related to
// GeoServerExtensions still being created
@Configuration(proxyBeanMethods = true)
@EnableConfigurationProperties(CatalogProperties.class)
@Slf4j
public class CoreBackendConfiguration {

    public @Bean XStreamPersisterFactory xstreamPersisterFactory() {
        return new XStreamPersisterFactory();
    }

    //    @Autowired
    //    @DependsOn("geoServerLoaderImpl")
    //    public @Bean GeoServerLoaderProxy geoServerLoader(GeoServerResourceLoader resourceLoader)
    // {
    //        return new GeoServerLoaderProxy(resourceLoader);
    //    }

    public @Bean GeoServerExtensions extensions() {
        return new GeoServerExtensions();
    }

    /** Usually provided by gs-main */
    @ConditionalOnMissingBean
    @DependsOn("extensions")
    public @Bean GeoServerEnvironment environments() {
        return new GeoServerEnvironment();
    }

    @ConditionalOnMissingBean(CatalogPlugin.class)
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

    // Added in 2.22.x
    //  <bean id="defaultResourceAccessManager"
    // class="org.geoserver.security.impl.DefaultResourceAccessManager">
    //    <constructor-arg ref="accessRulesDao"/>
    //    <constructor-arg ref="rawCatalog"/>
    //    <property name="groupsCache" ref="layerGroupContainmentCache"/>
    //  </bean>
    @ConditionalOnMissingBean
    @DependsOn("layerGroupContainmentCache")
    public @Bean DefaultResourceAccessManager defaultResourceAccessManager( //
            DataAccessRuleDAO dao, //
            @Qualifier("rawCatalog") Catalog rawCatalog) {
        return new DefaultResourceAccessManager(dao, rawCatalog);
    }

    // Added in 2.22.x
    //  <bean id="layerGroupContainmentCache"
    // class="org.geoserver.security.impl.LayerGroupContainmentCache">
    //    <constructor-arg ref="rawCatalog"/>
    //  </bean>
    @ConditionalOnMissingBean
    public @Bean LayerGroupContainmentCache layerGroupContainmentCache(
            @Qualifier("rawCatalog") Catalog rawCatalog) {
        return new LayerGroupContainmentCache(rawCatalog) {

            @Override
            public void onApplicationEvent(ApplicationEvent applicationEvent) {
                try {
                    super.onApplicationEvent(applicationEvent);
                } catch (RuntimeException e) {
                    String p = applicationEvent.getClass().getPackage().getName();
                    if (p.startsWith("org.springframework.test.context.event")) {
                        // ignore, spring-test sends events when the catalog is unavailable (e.g.
                        // jdbcconfig db closed)
                        log.debug("Error handling " + applicationEvent, e);
                    } else {
                        throw e;
                    }
                }
            }
        };
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

    @ConditionalOnMissingBean(GeoServerImpl.class)
    public @Bean(name = "geoServer") GeoServerImpl geoServer(
            @Qualifier("catalog") Catalog catalog,
            @Qualifier("geoserverFacade") GeoServerFacade facade)
            throws Exception {
        GeoServerImpl gs = new GeoServerImpl(facade);
        gs.setCatalog(catalog);
        return gs;
    }

    @Autowired
    public @Bean GeoServerDataDirectory dataDirectory(GeoServerResourceLoader resourceLoader) {
        return new GeoServerDataDirectory(resourceLoader);
    }
}
