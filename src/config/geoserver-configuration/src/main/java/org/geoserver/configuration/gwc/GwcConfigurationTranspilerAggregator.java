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
 * @see GwcImageCodecsConfiguration
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
        excludes = {
            // Replaced in DiskQuotaAutoConfiguration: the transpiler picks the wrong constructor.
            "DiskQuotaConfigLoader",
            // Replaced in DiskQuotaAutoConfiguration (fallback) and PgconfigDiskQuotaAutoConfiguration
            // (pgconfig-aware subclass). Excluding lets backends override via standard bean wiring
            // instead of relying on bean-definition overriding to win against the XML import.
            "DiskQuotaStoreProvider"
        })
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
        publicAccess = true,
        // contributed by GwcImageCodecsConfiguration, which GeoWebCache needs whether or not this service runs
        excludes = {
            GwcConfigurationTranspilerAggregator.IMAGE_ENCODERS,
            GwcConfigurationTranspilerAggregator.IMAGE_DECODERS,
            GwcConfigurationTranspilerAggregator.IMAGE_ENCODER_CONTAINER,
            GwcConfigurationTranspilerAggregator.IMAGE_DECODER_CONTAINER
        })
@TranspileXmlConfig(
        locations = "jar:gs-gwc-[0-9]+.*!/geowebcache-wmsservice-context.xml",
        targetClass = "GwcImageCodecsConfiguration",
        publicAccess = true,
        /*
         * The image codecs upstream declares along with the GWC WMS service. The coalescedRequestSplitter the
         * gwcFacade is built with decodes the cached member tiles and encodes the assembled multi-layer tile, hence
         * these beans come with the GeoServer integration instead. The per-format codecs come along with the two
         * containers, which collect them from the application context and fail to initialize if none is found.
         */
        includes = {
            GwcConfigurationTranspilerAggregator.IMAGE_ENCODERS,
            GwcConfigurationTranspilerAggregator.IMAGE_DECODERS,
            GwcConfigurationTranspilerAggregator.IMAGE_ENCODER_CONTAINER,
            GwcConfigurationTranspilerAggregator.IMAGE_DECODER_CONTAINER
        })
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
             * diskQuotaMenuPage: excluded here and contributed separately by
             * GwcDiskQuotaWebUIConfiguration, gated on gwc.disk-quota.enabled by
             * DiskQuotaAutoConfiguration's DisquotaWebUIAutoConfiguration.
             *
             * wmtsServiceDescriptor: replaced by CloudGWCServiceDescriptionProvider
             */
            "diskQuotaMenuPage"
        })
@TranspileXmlConfig(
        locations = "jar:gs-web-gwc-.*!/applicationContext.xml",
        targetClass = "GwcDiskQuotaWebUIConfiguration",
        publicAccess = true,
        // Server -> DiskQuota admin page; imported only when gwc.disk-quota.enabled=true.
        includes = {"diskQuotaMenuPage"})
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
public class GwcConfigurationTranspilerAggregator {

    static final String IMAGE_ENCODERS = ".*Encoder";

    static final String IMAGE_DECODERS = ".*Decoder";

    static final String IMAGE_ENCODER_CONTAINER = "encoderContainer";

    static final String IMAGE_DECODER_CONTAINER = "decoderContainer";
}
