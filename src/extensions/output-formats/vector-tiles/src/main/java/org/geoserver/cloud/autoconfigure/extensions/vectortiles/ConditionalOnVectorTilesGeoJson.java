/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.vectortiles;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Composite annotation that combines conditions required for GeoJSON Vector Tiles format.
 *
 * <p>
 * This conditional activates when:
 * <ul>
 *   <li>The Vector Tiles extension is enabled</li>
 *   <li>The geoserver.extension.vector-tiles.geojson property is true (the default)</li>
 * </ul>
 *
 * <p>
 * This annotation can be used on configuration classes or bean methods to make them
 * conditional on the GeoJSON vector tiles format being enabled.
 *
 * @see ConditionalOnVectorTiles
 * @see ConditionalOnProperty
 * @since 2.27.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ConditionalOnVectorTiles
@ConditionalOnProperty(
        prefix = VectorTilesConfigProperties.PREFIX,
        name = "geojson",
        havingValue = "true",
        matchIfMissing = true)
public @interface ConditionalOnVectorTilesGeoJson {}
