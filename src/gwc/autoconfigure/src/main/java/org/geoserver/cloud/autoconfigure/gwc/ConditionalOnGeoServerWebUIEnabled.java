/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Conditionals:
 *
 * <ul>
 *   <li>The GeoServer webui service is enabled ({@link ConditionalOnGeoServerWebUI})
 *   <li>The {@literal gs-web-gwc} jar is in the classpath
 *   <li>{@literal gwc.enabled=true}: Core gwc integration is enabled
 *   <li>{@literal geoserver.web-ui.gwc.enabled=true}: GeoServer gwc web-ui integration is enabled
 * </ul>
 *
 * <p>The {@literal gs-web-gwc} classpath check alone no longer identifies the webui application: on a combined app
 * classpath that unions every GeoServer service, the jar can be present even when the webui service itself is disabled.
 * {@link ConditionalOnGeoServerWebUI} makes that explicit. These components activate only where the webui service is
 * enabled.
 *
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnGeoWebCacheEnabled
@ConditionalOnGeoServerWebUI
@ConditionalOnClass(name = "org.geoserver.gwc.web.GWCSettingsPage")
@ConditionalOnProperty(
        name = GoServerWebUIConfigurationProperties.GWC_WEBUI_ENABLED_PROPERTY,
        havingValue = "true",
        matchIfMissing = false)
public @interface ConditionalOnGeoServerWebUIEnabled {}
