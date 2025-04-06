/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.security.gateway.sharedauth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Meta-annotation to conditionally register beans only when Gateway Shared Auth is disabled.
 *
 * <p>This conditional annotation is used to enable configuration classes or beans
 * only when the Gateway Shared Authentication feature is explicitly disabled. It checks
 * the following condition:
 *
 * <pre>
 * geoserver.extension.security.gateway-shared-auth.enabled = false
 * </pre>
 *
 * <p>When the feature is disabled, the {@link DisabledModeConfiguration} is activated,
 * which provides a no-op implementation of the authentication filter to maintain
 * backward compatibility when the feature was previously enabled.
 *
 * @see GatewaySharedAuthConfigProperties
 * @see ConditionalOnGatewaySharedAuthEnabled
 * @see DisabledModeConfiguration
 * @since 1.9
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnProperty(
        name = GatewaySharedAuthConfigProperties.ENABLED_PROP,
        havingValue = "false",
        matchIfMissing = true)
public @interface ConditionalOnGatewaySharedAuthDisabled {}
