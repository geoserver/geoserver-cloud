/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.observability.logging.config;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "logging.mdc.include")
public class MDCConfigProperties {

    private boolean user = true;
    private boolean roles = true;
    private boolean ows = true;
}
