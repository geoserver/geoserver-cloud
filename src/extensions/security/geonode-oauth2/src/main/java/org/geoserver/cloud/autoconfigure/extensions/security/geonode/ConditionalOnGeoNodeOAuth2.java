/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.geonode;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Composite annotation that combines conditions required for GeoNode OAuth2 extension support.
 *
 * <p>
 * This conditional activates when:
 * <ul>
 *   <li>The geoserver.extension.security.geonode-oauth2.enabled property is true (the default)</li>
 * </ul>
 *
 * <p>
 * This annotation can be used on configuration classes or bean methods to make them
 * conditional on GeoNode OAuth2 being enabled.
 *
 * @see ConditionalOnProperty
 * @since 2.27.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
@ConditionalOnProperty(
        name = "geoserver.extension.security.geonode-oauth2.enabled",
        havingValue = "true",
        matchIfMissing = GeoNodeOAuth2ConfigProperties.DEFAULT)
public @interface ConditionalOnGeoNodeOAuth2 {}
