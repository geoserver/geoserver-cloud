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
 * Composite annotation that combines conditions required for Vector Tiles extension support.
 *
 * <p>
 * This conditional activates when:
 * <ul>
 *   <li>The GeoServer WMS module is available in the application context</li>
 *   <li>The VectorTileMapOutputFormat class is available on the classpath</li>
 *   <li>The geoserver.extension.vector-tiles.enabled property is true (the default)</li>
 * </ul>
 *
 * <p>
 * This annotation can be used on configuration classes or bean methods to make them
 * conditional on the Vector Tiles extension being enabled.
 *
 * @see ConditionalOnGeoServerWMS
 * @see ConditionalOnClass
 * @see ConditionalOnProperty
 * @since 2.27.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
@ConditionalOnGeoServerWMS
@ConditionalOnClass(VectorTileMapOutputFormat.class)
@ConditionalOnProperty(name = "geoserver.extension.vector-tiles.enabled", havingValue = "true", matchIfMissing = true)
public @interface ConditionalOnVectorTiles {}
