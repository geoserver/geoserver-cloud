/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.geoserver.cloud.gwc.config.core.GeoWebCacheConfigurationProperties;
import org.geowebcache.storage.blobstore.gcs.GoogleCloudStorageBlobStoreInfo;
import org.geowebcache.storage.blobstore.gcs.GoogleCloudStorageConfigProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Conditionals:
 *
 * <ul>
 *   <li>{@literal gwc.enabled=true}: Core gwc integration is enabled
 *   <li>{@literal gs-gwc-gcs-blob.jar}: is in the classpath
 *   <li>{@literal gwc.blobstores.gcs=true}: GCS blobstore integration is enabled
 * </ul>
 *
 * @since 2.28.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnGeoWebCacheEnabled
@ConditionalOnClass({GoogleCloudStorageBlobStoreInfo.class, GoogleCloudStorageConfigProvider.class})
@ConditionalOnProperty(
        name = GeoWebCacheConfigurationProperties.BLOBSTORE_GCS_ENABLED,
        havingValue = "true",
        matchIfMissing = false)
public @interface ConditionalOnGoogleCloudStorageBlobstoreEnabled {}
