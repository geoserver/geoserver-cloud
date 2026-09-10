/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.tileverse.parquetry;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Conditional for the {@code arrow-ipc} WFS output format feature of the Parquetry plugin:
 * {@link ConditionalOnParquetry} plus {@code geoserver.extension.tileverse.parquetry.output-formats.arrow-ipc.enabled}
 * being {@code true} (the default).
 *
 * @since 3.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
@ConditionalOnParquetry
@ConditionalOnProperty(
        prefix = ParquetryConfigProperties.PREFIX + ".output-formats.arrow-ipc",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public @interface ConditionalOnParquetryArrowIpcOutputFormat {}
