/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.tileverse.parquetry;

import io.tileverse.parquetry.geotools.parquet.GeoParquetDataStoreFactory;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Base conditional for the Parquetry plugin extension: the application is a GeoServer service, the parquetry-geotools
 * classes are on the classpath, and the umbrella switch {@code geoserver.extension.tileverse.parquetry.enabled} is
 * {@code true} (the default).
 *
 * <p>The per-feature conditionals compose this one with the feature's own toggle, see
 * {@link ConditionalOnParquetryParquet}.
 *
 * @since 3.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
@ConditionalOnGeoServer
@ConditionalOnClass(GeoParquetDataStoreFactory.class)
@ConditionalOnProperty(
        prefix = ParquetryConfigProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public @interface ConditionalOnParquetry {}
