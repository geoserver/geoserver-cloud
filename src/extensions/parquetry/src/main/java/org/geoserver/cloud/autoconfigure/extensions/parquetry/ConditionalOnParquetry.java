/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.parquetry;

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
 * Composite conditional for the Parquetry extension.
 *
 * <p>Activates when the application is a GeoServer service, the parquetry-geotools classes are on the classpath, and
 * {@code geoserver.extension.parquetry.enabled} is {@code true} (the default).
 *
 * @since 3.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
@ConditionalOnGeoServer
@ConditionalOnClass(GeoParquetDataStoreFactory.class)
@ConditionalOnProperty(name = "geoserver.extension.parquetry.enabled", havingValue = "true", matchIfMissing = true)
public @interface ConditionalOnParquetry {}
