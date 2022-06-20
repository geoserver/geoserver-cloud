/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.main;

import org.geoserver.cloud.config.catalog.backend.core.GeoServerBackendConfigurer;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * Loads bean definitions from {@code jar:gs-main-.*!/applicationContext.xml}, excluding the ones
 * that shall be provided by the enabled {@link GeoServerBackendConfigurer}, as defined in {@code
 * gs-cloud-catalog-backend-starter}.
 *
 * <p>For instance:
 *
 * <ul>
 *   <li>{@link GeoServerBackendConfigurer#accessRulesDao}
 *   <li>{@link GeoServerBackendConfigurer#catalogFacade}
 *   <li>{@link GeoServerBackendConfigurer#dataDirectory}
 *   <li>{@link GeoServerBackendConfigurer#extensions}
 *   <li>{@link GeoServerBackendConfigurer#geoServer}
 *   <li>{@link GeoServerBackendConfigurer#geoserverFacade}
 *   <li>{@link GeoServerBackendConfigurer#geoServerLoader}
 *   <li>{@link GeoServerBackendConfigurer#geoServerSecurityManager}
 *   <li>{@link GeoServerBackendConfigurer#rawCatalog}
 *   <li>{@link GeoServerBackendConfigurer#resourceLoader}
 *   <li>{@link GeoServerBackendConfigurer#resourceStoreImpl}
 *   <li>{@link GeoServerBackendConfigurer#secureCatalog}
 *   <li>{@link GeoServerBackendConfigurer#xstreamPersisterFactory}
 * </ul>
 *
 * <p>Other excluded beans that are not used in geoserver-cloud:
 *
 * <p>We let spring-boot's {@code ForwardedHeaderFilter} take care of reflecting the
 * client-originated protocol and address in the HttpServletRequest through the {@code
 * server.forward-headers-strategy=framework} config property, so the following are not used:
 *
 * <ul>
 *   <li>{@code <bean id="proxyfierHeaderCollector"
 *       class="org.geoserver.ows.HTTPHeadersCollector"/>}
 *   <li>{@code <bean id="proxyfierHeaderTransfer" class=
 *       "org.geoserver.threadlocals.PublicThreadLocalTransfer">}
 *   <li>{@code <bean id="proxyfier" class="org.geoserver.ows.ProxifyingURLMangler">}
 * </ul>
 *
 * <p>
 *
 * <ul>
 *   <li>{@code logsPage}: {@code org.geoserver.web.admin.LogPage} is of no use with the
 *       microservices approach, as it reads from {@literal <datadir>/logs/geoserver.log}
 * </ul>
 */
@Configuration(proxyBeanMethods = true)
@ImportResource( //
        reader = FilteringXmlBeanDefinitionReader.class, //
        // exclude beans
        locations =
                "jar:gs-main-.*!/applicationContext.xml#name="
                        + GeoServerMainModuleConfiguration.EXCLUDE_BEANS_REGEX)
public class GeoServerMainModuleConfiguration {

    private static final String UNUSED_BEAN_NAMES =
            "proxyfierHeaderCollector"
                    + "|proxyfierHeaderTransfer"
                    + "|proxyfier"
                    + "|fileLockProvider"
                    + "|memoryLockProvider"
                    + "|nullLockProvider"
                    + "|lockProviderInitializer";

    private static final String OVERRIDDEN_BEAN_NAMES =
            "rawCatalog"
                    + "|secureCatalog"
                    + "|localWorkspaceCatalog"
                    + "|catalog"
                    + "|advertisedCatalog"
                    + "|accessRulesDao"
                    + "|catalogFacade"
                    + "|dataDirectory"
                    + "|extensions"
                    + "|geoServer"
                    + "|geoserverFacade"
                    + "|geoServerLoader"
                    + "|geoServerSecurityManager"
                    + "|resourceLoader"
                    + "|resourceStoreImpl"
                    + "|xstreamPersisterFactory"
                    + "|loggingInitializer";

    static final String EXCLUDE_BEANS_REGEX =
            "^(?!" + OVERRIDDEN_BEAN_NAMES + "|" + UNUSED_BEAN_NAMES + ").*$";
}
