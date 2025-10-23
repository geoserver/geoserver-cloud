/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.inspire.webui;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.cloud.autoconfigure.extensions.inspire.ConditionalOnInspire;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * Conditional annotation that only matches when:
 * <ul>
 *   <li>A GeoServer instance is available in the application context
 *   <li>The INSPIRE extension is enabled via configuration property
 *   <li>WebUI is available
 * </ul>
 *
 * @since 2.27.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
@ConditionalOnInspire
@ConditionalOnGeoServerWebUI
@ConditionalOnClass(name = "org.geoserver.inspire.web.InspireAdminPanel")
public @interface ConditionalOnInspireWebUI {}
