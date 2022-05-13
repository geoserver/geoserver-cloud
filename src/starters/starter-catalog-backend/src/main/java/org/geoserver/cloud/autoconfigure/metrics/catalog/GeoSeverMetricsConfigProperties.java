/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */

package org.geoserver.cloud.autoconfigure.metrics.catalog;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @since 1.0
 */
@Data
@ConfigurationProperties(prefix = "geoserver.metrics")
public class GeoSeverMetricsConfigProperties {

    private boolean enabled = true;

    private String instanceId;
}
