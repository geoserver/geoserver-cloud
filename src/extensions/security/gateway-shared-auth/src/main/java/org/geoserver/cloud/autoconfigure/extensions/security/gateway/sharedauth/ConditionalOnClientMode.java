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
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUIUnavailable;

/**
 * Meta-annotation to conditionally register beans for Gateway Shared Auth client mode.
 *
 * <p>This conditional annotation activates the client-side configuration for the Gateway
 * Shared Authentication feature. It's designed for use in all services except the WebUI,
 * which act as authentication clients by processing authentication headers from incoming
 * requests.</p>
 *
 * <p>Client mode is now automatically activated when:
 * <ul>
 *   <li>Gateway Shared Authentication is enabled</li>
 *   <li>The current application is NOT the GeoServer WebUI</li>
 * </ul>
 *
 * <p>Note: Previously, this was controlled by the configuration property
 * {@code geoserver.extension.security.gateway-shared-auth.server=false}, but now the mode
 * is automatically determined based on the application type.
 *
 * @see ClientModeConfiguration
 * @see ConditionalOnServerMode
 * @see ConditionalOnGatewaySharedAuthEnabled
 * @see ConditionalOnGeoServerWebUIUnavailable
 * @since 1.9
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnGatewaySharedAuthEnabled
@ConditionalOnGeoServerWebUIUnavailable
public @interface ConditionalOnClientMode {}
