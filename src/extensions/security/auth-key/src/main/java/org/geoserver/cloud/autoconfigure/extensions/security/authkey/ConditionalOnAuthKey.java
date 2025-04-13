/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.authkey;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServer;
import org.geoserver.security.GeoServerAuthenticationKeyProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Conditional annotation that only matches when:
 * <ul>
 *   <li>A GeoServer bean instance is available in the application context
 *   <li>The application is a servlet web application
 *   <li>The AuthKey extension is in the classpath
 *   <li>The AuthKey extension is enabled via configuration property
 * </ul>
 *
 * <p>This can be used on any Spring components that should only be registered when the
 * AuthKey extension is enabled.
 *
 * <p>Usage example:
 * <pre>{@code
 * @Configuration
 * @ConditionalOnAuthKey
 * public class MyAuthKeyConfiguration {
 *     // Configuration only activated when AuthKey is enabled
 * }
 * }</pre>
 *
 * @since 2.27.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
@ConditionalOnGeoServer
@ConditionalOnClass(GeoServerAuthenticationKeyProvider.class)
@ConditionalOnProperty(
        prefix = AuthKeyConfigProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = AuthKeyConfigProperties.DEFAULT)
public @interface ConditionalOnAuthKey {}
