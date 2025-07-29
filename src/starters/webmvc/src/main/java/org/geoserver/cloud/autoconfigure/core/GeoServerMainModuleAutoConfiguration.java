/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.core;

import org.geoserver.cloud.autoconfigure.catalog.backend.core.GeoServerBackendAutoConfiguration;
import org.geoserver.cloud.config.catalog.backend.core.GeoServerBackendConfigurer;
import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/** Autoconfiguration for GeoServer's main module on servlet/webmvc applications
 * <p>
 * Loads bean definitions from {@code jar:gs-main-.*!/applicationContext.xml}, excluding the ones
 * that shall be provided by the enabled {@link GeoServerBackendConfigurer}, as defined in {@code
 * gs-cloud-catalog-backend-starter}.
 * */
@AutoConfiguration(after = GeoServerBackendAutoConfiguration.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@TranspileXmlConfig(
        locations = "jar:gs-main-.*!/applicationContext.xml",
        targetPackage = "org.geoserver.config.gen.main",
        targetClass = "GeoServerMainModuleConfiguration",
        publicAccess = true,
        excludes = {
            // Beans overridden by GeoServerBackendConfigurer (from gs-cloud-catalog-backend-starter)
            "catalog", // overridden by cloud-specific catalog implementation (alias for localWorkspaceCatalog)
            "rawCatalog", // provided by GeoServerBackendConfigurer#rawCatalog
            "secureCatalog", // provided by GeoServerBackendConfigurer#secureCatalog
            "localWorkspaceCatalog", // overridden by cloud-specific catalog implementation
            "advertisedCatalog", // overridden by cloud-specific catalog implementation
            "accessRulesDao", // provided by GeoServerBackendConfigurer#accessRulesDao
            "catalogFacade", // provided by GeoServerBackendConfigurer#catalogFacade
            "dataDirectory", // provided by GeoServerBackendConfigurer#dataDirectory
            "extensions", // provided by CoreBackendConfiguration#extensions
            "geoServer", // provided by GeoServerBackendConfigurer#geoServer
            "geoserverFacade", // provided by GeoServerBackendConfigurer#geoserverFacade
            "geoServerLoader", // provided by GeoServerBackendConfigurer#geoServerLoader
            "geoServerSecurityManager", // provided by GeoServerBackendConfigurer#geoServerSecurityManager
            "resourceLoader", // provided by GeoServerBackendConfigurer#resourceLoader
            "dataDirectoryResourceStore", // provided by datadir catalog backend when enabled
            "resourceStoreImpl", // provided by GeoServerBackendConfigurer#resourceStoreImpl
            "xstreamPersisterFactory", // provided by GeoServerBackendConfigurer#xstreamPersisterFactory
            "loggingInitializer", // overridden for cloud-specific logging configuration
            "configurationLock", // overridden for distributed configuration management
            "layerGroupContainmentCache", // overridden for distributed caching

            // Beans unused in geoserver-cloud - proxying handled by Spring Boot's ForwardedHeaderFilter
            // via server.forward-headers-strategy=framework config property
            "proxyfierHeaderCollector", // unused - Spring Boot handles forwarded headers
            "proxyfierHeaderTransfer", // unused - Spring Boot handles forwarded headers
            "proxyfier", // unused - Spring Boot handles forwarded headers

            // Beans unused in geoserver-cloud - lock providers handled differently
            "fileLockProvider", // unused - cloud uses distributed locking mechanisms
            "memoryLockProvider", // unused - cloud uses distributed locking mechanisms

            // GeoServer Cloud uses org.geoserver.platform.config.UpdateSequence instead
            "updateSequenceListener", // package-private class org.geoserver.config.UpdateSequenceListener
        })
@Import(org.geoserver.config.gen.main.GeoServerMainModuleConfiguration.class)
public class GeoServerMainModuleAutoConfiguration {}
