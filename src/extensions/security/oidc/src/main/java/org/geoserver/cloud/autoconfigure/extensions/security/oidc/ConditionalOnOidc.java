/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.oidc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServer;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Conditional annotation that matches when:
 *
 * <ul>
 *   <li>A {@link org.geoserver.config.GeoServer} bean is available in the application context
 *   <li>The OIDC extension is on the classpath (anchor: {@link GeoServerOAuth2LoginAuthenticationFilter})
 *   <li>{@code geoserver.extension.security.oidc.enabled} is {@code true} or unset
 * </ul>
 *
 * <p>Applied to {@code @Configuration} classes or {@code @Bean} methods that should only be registered when OIDC is
 * enabled.
 *
 * @since 3.0.3
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
@ConditionalOnGeoServer
@ConditionalOnClass(GeoServerOAuth2LoginAuthenticationFilter.class)
@ConditionalOnProperty(
        prefix = OidcConfigProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = OidcConfigProperties.DEFAULT)
public @interface ConditionalOnOidc {}
