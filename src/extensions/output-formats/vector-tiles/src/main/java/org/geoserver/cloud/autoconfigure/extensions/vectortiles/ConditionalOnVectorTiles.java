/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.vectortiles;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWMS;
import org.geoserver.wms.vector.VectorTileMapOutputFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Composite annotation that combines conditions required for Vector Tiles extension support
 * across multiple GeoServer services.
 *
 * <p>
 * This conditional activates when:
 * <ul>
 *   <li>The VectorTileMapOutputFormat class is available on the classpath</li>
 *   <li>The geoserver.extension.vector-tiles.enabled property is true (the default)</li>
 * </ul>
 *
 * <p>
 * This annotation is used as a base condition for enabling vector tiles support. It is
 * designed to work in conjunction with service-specific conditions to enable vector tiles
 * across multiple services (WMS, WebUI, GWC) in a consistent manner.
 *
 * <p>
 * It provides a good example of how to create composable conditional annotations in
 * GeoServer Cloud that can work with different service-specific conditions to enable
 * functionality in multiple services while maintaining a single point of control for
 * the extension's overall enabled/disabled state.
 *
 * @see ConditionalOnGeoServerWMS
 * @see ConditionalOnGeoServerWebUI
 * @see ConditionalOnGeoServerGWC
 * @see ConditionalOnClass
 * @see ConditionalOnProperty
 * @since 2.27.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
@ConditionalOnClass(VectorTileMapOutputFormat.class)
@ConditionalOnProperty(name = "geoserver.extension.vector-tiles.enabled", havingValue = "true", matchIfMissing = true)
public @interface ConditionalOnVectorTiles {}
