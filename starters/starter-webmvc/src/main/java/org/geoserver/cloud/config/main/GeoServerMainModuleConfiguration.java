/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.main;

import org.geoserver.cloud.config.catalog.GeoServerBackendConfigurer;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
 * <p>
 */
@Configuration(proxyBeanMethods = true)
@Import({UrlProxifyingConfiguration.class})
@ImportResource( //
    reader = FilteringXmlBeanDefinitionReader.class, //
    // exclude beans
    locations =
            "jar:gs-main-.*!/applicationContext.xml#name="
                    + GeoServerMainModuleConfiguration.EXCLUDE_BEANS_REGEX
)
public class GeoServerMainModuleConfiguration {

    static final String EXCLUDE_BEANS_REGEX =
            "^(?!rawCatalog|secureCatalog|localWorkspaceCatalog|catalog|advertisedCatalog|accessRulesDao|catalogFacade|dataDirectory|extensions|geoServer|geoserverFacade|geoServerLoader|geoServerSecurityManager|resourceLoader|resourceStoreImpl|secureCatalog|xstreamPersisterFactory).*$";
}
