/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.logging.mdc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for controlling which GeoServer-specific information is included in the MDC.
 * <p>
 * These properties determine what GeoServer-related information is added to the MDC (Mapped Diagnostic Context)
 * during OGC Web Service (OWS) request processing. Including this information in the MDC makes it available
 * to all logging statements, providing valuable context for debugging and monitoring OGC service requests.
 * <p>
 * The properties are configured using the prefix {@code logging.mdc.include.geoserver} in the application
 * properties or YAML files.
 * <p>
 * Example configuration in YAML:
 * <pre>
 * logging:
 *   mdc:
 *     include:
 *       geoserver:
 *         ows:
 *           service-name: true
 *           service-version: true
 *           service-format: true
 *           operation-name: true
 * </pre>
 *
 * @see org.geoserver.cloud.logging.mdc.ows.OWSMdcDispatcherCallback
 */
@Data
@ConfigurationProperties(prefix = "logging.mdc.include.geoserver")
public class GeoServerMdcConfigProperties {

    private OWSMdcConfigProperties ows = new OWSMdcConfigProperties();

    /**
     * Configuration properties for OGC Web Service (OWS) request information in the MDC.
     * <p>
     * These properties control which OWS-specific information is added to the MDC during
     * GeoServer request processing. This information allows for identifying and tracking
     * specific OGC service operations in logs.
     */
    @Data
    public static class OWSMdcConfigProperties {
        /**
         * Whether to append the gs.ows.service.name MDC property from the OWS dispatched request
         */
        private boolean serviceName = true;

        /**
         * Whether to append the gs.ows.service.version MDC property from the OWS dispatched request
         */
        private boolean serviceVersion = true;

        /**
         * Whether to append the gs.ows.service.format MDC property from the OWS dispatched request
         */
        private boolean serviceFormat = true;

        /**
         * Whether to append the gs.ows.service.operation MDC property from the OWS dispatched
         * request
         */
        private boolean operationName = true;
    }
}
