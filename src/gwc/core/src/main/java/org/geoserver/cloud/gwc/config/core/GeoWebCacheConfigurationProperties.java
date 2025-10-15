/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.config.core;

import java.nio.file.Path;
import lombok.Data;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for GeoWebcache
 *
 * <p>Available properties:
 *
 * <pre>{@code
 * gwc:
 *   enabled: true
 *   cache-directory:
 *   web-ui: false
 *   rest-config: false
 *   blobstores:
 *     azure: true
 *     s3: true
 *     gcs: true
 *   services:
 *     wms: false
 *     tms: false
 *     wmts: false
 *     kml: false
 *     gmaps: false
 *     mgmaps: false
 *   disk-quota:
 *     enabled: false
 *     data-source:
 *       # either jndiName or in-line data source properties
 *       # jndiName:
 *       url:
 *       username:
 *       password:
 *       driverClassname:
 *       minimumIdle:
 *       maximumPoolSize:
 * }</pre>
 *
 * {@code gwc.disk-quota.data-source} is a {@link DataSourceProperties}
 *
 * @since 1.0
 */
@ConfigurationProperties(prefix = "gwc")
public @Data class GeoWebCacheConfigurationProperties {

    public static final String GWC_ENABLED_PROPERTY = "gwc.enabled";
    public static final String CACHE_DIRECTORY = "gwc.cache-directory";
    public static final String CONFIG_DIRECTORY = "gwc.config-directory";
    public static final String WEBUI_ENABLED = "gwc.web-ui";
    public static final String WMS_INTEGRATION_ENABLED = "gwc.wms-integration";
    public static final String RESTCONFIG_ENABLED = "gwc.rest-config";
    public static final String SERVICE_WMTS_ENABLED = "gwc.services.wmts";
    public static final String SERVICE_TMS_ENABLED = "gwc.services.tms";
    public static final String SERVICE_WMS_ENABLED = "gwc.services.wms";
    public static final String SERVICE_KML_ENABLED = "gwc.services.kml";
    public static final String SERVICE_GMAPS_ENABLED = "gwc.services.gmaps";
    public static final String SERVICE_MGMAPS_ENABLED = "gwc.services.mgmaps";
    public static final String DISKQUOTA_ENABLED = "gwc.disk-quota.enabled";
    public static final String DISKQUOTA_DATASOURCE = "gwc.disk-quota.data-source";
    public static final String BLOBSTORE_S3_ENABLED = "gwc.blobstores.s3";
    public static final String BLOBSTORE_AZURE_ENABLED = "gwc.blobstores.azure";
    public static final String BLOBSTORE_GCS_ENABLED = "gwc.blobstores.gcs";

    /**
     * Enables the core GeoWebCache functionality and integration with GeoServer tile layers. All
     * other config properties depend on this one to be enabled.
     */
    private boolean enabled = true;

    /**
     * Location of the default cache directory. This is the directory where tile images will be
     * stored, unless a separate "Blob Store" is configured for a given Tile Layer.
     */
    private Path cacheDirectory;

    /**
     * Alternate parent directory for the global geowebcache.xml configuration file. If specified,
     * must be an absolute path to a writable directory. An attempt to create it will be made if it
     * doesn't exist. Defaults to null, meaning the GeoServer resource store's gwc/ directory will
     * be used.
     */
    private Path configDirectory = null;

    /** Enables or disables the GWC user interface */
    private boolean webUi = false;

    /**
     * Enables or disables the extension to integrate GWC with GeoServer's WMS. This is a
     * component-level configuration property that, if enabled, the option to activate the
     * integration needs to be configured in GeoServer's WEB-UI, and if disabled, the Web-UI don't
     * even show the option
     */
    private boolean wmsIntegration = false;

    /** Enables or disables the GWC REST API to configure layers, blob stores, etc. */
    private boolean restConfig = false;

    private ServicesConfig services = new ServicesConfig();

    private DiskQuotaConfig diskQuota = new DiskQuotaConfig();

    private BlobstoresConfig blobstores = new BlobstoresConfig();

    /**
     * Configure which tile services to load at startup time. These are not dynamic enablements for
     * individual services, but application container level one. Disabled services won't even be
     * loaded to the runtime context.
     */
    public static @Data class ServicesConfig {
        /**
         * Enables or disables the WMTS service. This is not a dynamic runtime setting, but an
         * application container level one. Disabled services won't even be loaded to the runtime
         * context.
         */
        private boolean wmts = false;

        /**
         * Enables or disables the TMS service. This is not a dynamic runtime setting, but an
         * application container level one. Disabled services won't even be loaded to the runtime
         * context.
         */
        private boolean tms = false;

        /**
         * Enables or disables the WMS service. This is not a dynamic runtime setting, but an
         * application container level one. Disabled services won't even be loaded to the runtime
         * context.
         */
        private boolean wms = false;

        /**
         * Enables or disables the KML service. This is not a dynamic runtime setting, but an
         * application container level one. Disabled services won't even be loaded to the runtime
         * context.
         */
        private boolean kml = false;

        /**
         * Enables or disables the GoogleMaps service. This is not a dynamic runtime setting, but an
         * application container level one. Disabled services won't even be loaded to the runtime
         * context.
         */
        private boolean gmaps = false;

        /**
         * Enables or disables the MGMaps service. This is not a dynamic runtime setting, but an
         * application container level one. Disabled services won't even be loaded to the runtime
         * context.
         */
        private boolean mgmaps = false;
    }

    public static @Data class BlobstoresConfig {
        /**
         * Enables or disables support for Amazon-S3 BLOB Store. This is not a dynamic runtime
         * setting, but an application container level one. Disabled BLOB stores won't even be
         * loaded to the runtime context.
         */
        private boolean s3 = false;

        /**
         * Enables or disables support for Microsoft Azure BLOB Store. This is not a dynamic runtime
         * setting, but an application container level one. Disabled BLOB stores won't even be
         * loaded to the runtime context.
         */
        private boolean azure = false;

        /**
         * Enables or disables support for Google Cloud Storage BLOB Store. This is not a dynamic runtime
         * setting, but an application container level one. Disabled BLOB stores won't even be
         * loaded to the runtime context.
         */
        private boolean gcs = false;
    }

    private static @Data class DiskQuotaConfig {
        private boolean enabled = false;
        private DataSourceProperties dataSource;
    }
}
