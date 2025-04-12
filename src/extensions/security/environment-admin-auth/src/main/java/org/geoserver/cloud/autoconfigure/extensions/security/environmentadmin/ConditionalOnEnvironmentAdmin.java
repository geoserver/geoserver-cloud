/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.environmentadmin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Conditional annotation that only matches when:
 * <ul>
 *   <li>GeoServer security is enabled (default behavior)</li>
 *   <li>The Environment Admin authentication extension is enabled via configuration property</li>
 * </ul>
 *
 * <p>This can be used on any Spring components that should only be registered when the
 * Environment Admin authentication extension is enabled.
 *
 * <p>Usage example:
 * <pre>{@code
 * @Configuration
 * @ConditionalOnEnvironmentAdmin
 * public class MyEnvironmentAdminConfiguration {
 *     // Configuration only activated when Environment Admin authentication is enabled
 * }
 * }</pre>
 *
 * @since 2.27.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
@ConditionalOnProperty(
        prefix = EnvironmentAdminConfigProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = EnvironmentAdminConfigProperties.DEFAULT_ENABLED)
public @interface ConditionalOnEnvironmentAdmin {}
