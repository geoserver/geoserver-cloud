/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.logging.mdc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "logging.mdc.include")
public class MDCConfigProperties {

    private SpringEnvironmentMdcConfigProperties application = new SpringEnvironmentMdcConfigProperties();
    private HttpRequestMdcConfigProperties http = new HttpRequestMdcConfigProperties();
    private AuthenticationMdcConfigProperties user = new AuthenticationMdcConfigProperties();
    private GeoServerMdcConfigProperties geoserver = new GeoServerMdcConfigProperties();
}
