/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.logging.mdc.config;

import lombok.Data;

@Data
public class GeoServerMdcConfigProperties {

    private OWSMdcConfigProperties ows = new OWSMdcConfigProperties();

    /** Configuration properties to contribute GeoServer OWS request properties to the MDC */
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
