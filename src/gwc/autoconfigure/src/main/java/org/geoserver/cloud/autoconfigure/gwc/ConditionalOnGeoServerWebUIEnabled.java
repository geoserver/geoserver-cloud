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
import org.geoserver.gwc.web.GWCSettingsPage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Conditionals:
 *
 * <ul>
 *   <li>The {@literal gs-web-gwc} jar is in the classpath
 *   <li>{@literal gwc.enabled=true}: Core gwc integration is enabled
 *   <li>{@literal geoserver.web-ui.gwc.enabled=true}: GeoServer gwc web-ui integration is enabled
 * </ul>
 *
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnGeoWebCacheEnabled
@ConditionalOnClass(GWCSettingsPage.class)
@ConditionalOnProperty(
        name = GoServerWebUIConfigurationProperties.GWC_WEBUI_ENABLED_PROPERTY,
        havingValue = "true",
        matchIfMissing = false)
public @interface ConditionalOnGeoServerWebUIEnabled {}
