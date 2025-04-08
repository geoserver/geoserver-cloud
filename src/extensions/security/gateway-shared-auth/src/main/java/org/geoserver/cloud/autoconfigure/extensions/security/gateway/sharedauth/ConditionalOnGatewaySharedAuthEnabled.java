/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.security.gateway.sharedauth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.geoserver.cloud.autoconfigure.security.ConditionalOnGeoServerSecurityEnabled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Meta-annotation to conditionally register beans only when Gateway Shared Auth is enabled.
 *
 * <p>This conditional annotation is used to enable configuration classes or beans
 * only when the Gateway Shared Authentication feature is enabled. It checks the
 * following condition:
 *
 * <pre>
 * geoserver.extension.security.gateway-shared-auth.enabled = true
 * </pre>
 *
 * <p>Note that the Gateway service must also be configured with the same property
 * to enable the shared authentication mechanism to work end-to-end.
 *
 * @see GatewaySharedAuthConfigProperties
 * @see ConditionalOnGatewaySharedAuthDisabled
 * @since 1.9
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
@ConditionalOnGeoServerSecurityEnabled
@ConditionalOnProperty(
        name = GatewaySharedAuthConfigProperties.ENABLED_PROP,
        havingValue = "true",
        matchIfMissing = GatewaySharedAuthConfigProperties.DEFAULT_ENABLED)
public @interface ConditionalOnGatewaySharedAuthEnabled {}
