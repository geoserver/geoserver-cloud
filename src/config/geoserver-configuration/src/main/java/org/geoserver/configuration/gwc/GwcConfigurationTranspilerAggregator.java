/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.gwc;

import org.geoserver.spring.config.annotations.ComponentScanStrategy;
import org.geoserver.spring.config.annotations.TranspileXmlConfig;

/**
 * @see GwcCoreContextConfiguration
 * @see GwcGeoServerContextConfiguration
 * @see GwcDiskQuotaContextConfiguration
 * @see GwcRestConfiguration
 * @see GwcDiskQuotaRestConfiguration
 * @see GwcKMLServiceConfiguration
 * @see GwcTMSServiceConfiguration
 * @see GwcWMSServiceConfiguration
 * @see GwcWMTSServiceConfiguration
 * @see GwcGeoServerWMTSIntegrationConfiguration
 * @see GwcGeoServerWebUIConfiguration
 * @see GwcWMSMinimalConfiguration
 * @see GwcWfsMinimalConfiguration
 * @since 3.0
 */
@TranspileXmlConfig(
        locations = "jar:gs-gwc-[0-9]+.*!/geowebcache-geoserver-context.xml",
        targetClass = "GwcGeoServerContextConfiguration",
        publicAccess = true,
        excludes = {
            "GeoSeverTileLayerCatalog",
            "gwcCatalogConfiguration",
            "wmsCapabilitiesXmlReader",
            "gwcTransactionListener",
            "gwcWMSExtendedCapabilitiesProvider",
            "gwcInitializer",
            "gwcGeoServervConfigPersister"
        })
@TranspileXmlConfig(
        locations = "jar:gs-gwc-[0-9]+.*!/geowebcache-core-context.xml",
        targetClass = "GwcCoreContextConfiguration",
        publicAccess = true,
        excludes = {
            // provided by GeoWebCacheCoreAutoConfiguration:
            "gwcXmlConfig",
            "gwcDefaultStorageFinder",
            "gwcXmlConfigResourceProvider",
            // transpiles wrongly, added to GeoWebCacheCoreAutoConfiguration:
            "gwcGridSetBroker",
            "gwcStorageBroker",
            "gwcRuntimeStats",
            // unused:
            "nioLock"
        })
@TranspileXmlConfig(
        locations = "jar:gs-gwc-[0-9]+.*!/geowebcache-diskquota-context.xml",
        targetClass = "GwcDiskQuotaContextConfiguration",
        publicAccess = true,
        excludes = {"DiskQuotaConfigLoader"})
@TranspileXmlConfig(
        locations = "jar:gs-gwc-rest-[0-9]+.*!/applicationContext.xml",
        componentScanStrategy = ComponentScanStrategy.GENERATE,
        excludes = "org.geowebcache.diskquota.rest.controller.*",
        targetClass = "GwcRestConfiguration",
        publicAccess = true)
@TranspileXmlConfig(
        locations = "jar:gs-gwc-rest-[0-9]+.*!/applicationContext.xml",
        componentScanStrategy = ComponentScanStrategy.GENERATE,
        includes = "org.geowebcache.diskquota.rest.controller.*",
        targetClass = "GwcDiskQuotaRestConfiguration",
        publicAccess = true)
@TranspileXmlConfig(
        locations = "jar:gs-gwc-[0-9]+.*!/geowebcache-kmlservice-context.xml",
        targetClass = "GwcKMLServiceConfiguration",
        publicAccess = true)
@TranspileXmlConfig(
        locations = "jar:gs-gwc-[0-9]+.*!/geowebcache-tmsservice-context.xml",
        targetClass = "GwcTMSServiceConfiguration",
        publicAccess = true)
@TranspileXmlConfig(
        locations = "jar:gs-gwc-[0-9]+.*!/geowebcache-wmsservice-context.xml",
        targetClass = "GwcWMSServiceConfiguration",
        publicAccess = true)
@TranspileXmlConfig(
        locations = "jar:gs-gwc-[0-9]+.*!/geowebcache-wmtsservice-context.xml",
        targetClass = "GwcWMTSServiceConfiguration",
        publicAccess = true)
@TranspileXmlConfig(
        locations = "jar:gs-gwc-[0-9]+.*!/geowebcache-geoserver-wmts-integration.xml",
        targetClass = "GwcGeoServerWMTSIntegrationConfiguration",
        publicAccess = true)
@TranspileXmlConfig(
        locations = "jar:gs-web-gwc-.*!/applicationContext.xml",
        targetClass = "GwcGeoServerWebUIConfiguration",
        publicAccess = true,
        excludes = {
            /*
             * Disable disk-quota by brute force for now. We need to resolve how and where to store the
             * configuration and database.
             *
             * wmtsServiceDescriptor: replaced by CloudGWCServiceDescriptionProvider
             */
            "diskQuotaMenuPage"
        })
@TranspileXmlConfig(
        locations = "jar:gs-wfs-core-.*!/applicationContext.xml",
        targetClass = "GwcWfsMinimalConfiguration",
        publicAccess = true,
        includes = {
            // These are wfs core specific filters that required a careful evaluation of what to include to support a
            // minimal WMS without the WFS service running (i.e. without including all {@code gs-wfs} beans in the
            // application context.
            "wfsSqlViewKvpParser",
            "gml2OutputFormat"
        })
public class GwcConfigurationTranspilerAggregator {}
