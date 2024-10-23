/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.authzn;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.geoserver.cloud.autoconfigure.security.ConditionalOnGeoServerSecurityEnabled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Conditional to enable gateway/webui shared authentication mechanism. It must also be enabled in
 * the gateway with the same config property {@code
 * geoserver.security.gateway-shared-auth.enabled=true}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnGeoServerSecurityEnabled
@ConditionalOnProperty(
        name = GatewaySharedAuthConfigProperties.ENABLED_PROP,
        havingValue = "true",
        matchIfMissing = false)
public @interface ConditionalOnGatewaySharedAuthEnabled {}
