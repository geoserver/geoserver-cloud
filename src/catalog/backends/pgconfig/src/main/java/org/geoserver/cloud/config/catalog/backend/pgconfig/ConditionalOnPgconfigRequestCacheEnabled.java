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
import org.springframework.context.annotation.Conditional;

/**
 * Matches when {@code geoserver.backend.pgconfig.cache.request.enabled} is true, or not set. {@code 1}, {@code on} and
 * {@code yes} are true, as for the bound properties, see {@link PgconfigRequestCacheCondition}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Conditional(PgconfigRequestCacheCondition.Enabled.class)
public @interface ConditionalOnPgconfigRequestCacheEnabled {}
