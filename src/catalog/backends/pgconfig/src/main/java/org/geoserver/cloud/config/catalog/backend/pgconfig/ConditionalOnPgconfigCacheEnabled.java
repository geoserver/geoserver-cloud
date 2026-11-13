/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.config.catalog.backend.pgconfig;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/** Matches when {@code geoserver.backend.pgconfig.cache.enabled} is true, or not set. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnProperty(
        prefix = "geoserver.backend.pgconfig.cache",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public @interface ConditionalOnPgconfigCacheEnabled {}
