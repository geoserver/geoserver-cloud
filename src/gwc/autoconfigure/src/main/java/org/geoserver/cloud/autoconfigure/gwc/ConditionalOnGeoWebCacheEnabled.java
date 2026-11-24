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
import org.geoserver.gwc.GWC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;

/**
 * Conditionals:
 *
 * <ul>
 *   <li>The {@literal gs-gwc} jar is in the classpath
 *   <li>{@literal gwc.enabled=true}, the default when unset
 *   <li>The running instance serves a role that hosts GeoWebCache: wms, gwc, webui, or restconfig (see
 *       {@link OnGeoWebCacheHostingService})
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnClass(GWC.class)
@ConditionalOnProperty(
        name = GeoWebCacheConfigurationProperties.GWC_ENABLED_PROPERTY,
        havingValue = "true",
        matchIfMissing = true)
@Conditional(OnGeoWebCacheHostingService.class)
public @interface ConditionalOnGeoWebCacheEnabled {}
