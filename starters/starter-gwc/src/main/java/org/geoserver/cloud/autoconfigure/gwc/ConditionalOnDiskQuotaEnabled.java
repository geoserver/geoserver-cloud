/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.geowebcache.diskquota.DiskQuotaConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Disk Quota conditional, disabled by default
 *
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnGeoWebCacheEnabled
@ConditionalOnClass(DiskQuotaConfig.class) // i.e. gwc-diskquota.jar is in the classpath
@ConditionalOnProperty(
    name = GeoWebCacheConfigurationProperties.DISKQUOTA_ENABLED,
    havingValue = "true",
    matchIfMissing = false
)
public @interface ConditionalOnDiskQuotaEnabled {}
