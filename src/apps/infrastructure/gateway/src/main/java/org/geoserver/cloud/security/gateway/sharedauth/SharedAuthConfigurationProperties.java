/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.security.gateway.sharedauth;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * For automatic documentation purposes only, as used by the {@literal
 * spring-boot-configuration-processor}
 */
@ConfigurationProperties(prefix = "geoserver.security.gateway-shared-auth")
@Data
class SharedAuthConfigurationProperties {

    /**
     * Enable or disable the Gateway/WebUI Shared Authentication mechanism, where the Gateway works
     * as mediator to share the authentication from the GeoServer WebUI with the rest of the
     * services.
     */
    private boolean enabled = true;

    static final String X_GSCLOUD_ROLES = "x-gsc-roles";
    static final String X_GSCLOUD_USERNAME = "x-gsc-username";
}
