/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc;

import org.geowebcache.s3.S3BlobStoreConfigProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Conditionals:
 *
 * <ul>
 *   <li>{@literal gwc.enabled=true}: Core gwc integration is enabled
 *   <li>{@literal gs-gwc-s3-blob.jar}: is in the classpath
 *   <li>{@literal gwc.blobstores.s3=true}: S3 blobstore integration is enabled
 * </ul>
 *
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnGeoWebCacheEnabled
@ConditionalOnClass(S3BlobStoreConfigProvider.class)
@ConditionalOnProperty(
        name = GeoWebCacheConfigurationProperties.BLOBSTORE_S3_ENABLED,
        havingValue = "true",
        matchIfMissing = false)
public @interface ConditionalOnS3BlobstoreEnabled {}
