/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.blobstore;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoServerWebUIEnabled;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGoogleCloudStorageBlobstoreEnabled;
import org.geoserver.cloud.gwc.config.blobstore.GoogleCloudStoargeBlobStoreGsWebUIConfiguration;
import org.geoserver.cloud.gwc.config.blobstore.GoogleCloudStorageBlobstoreConfiguration;
import org.geoserver.gwc.web.blob.BlobStorePage;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @see ConditionalOnGoogleCloudStorageBlobstoreEnabled
 * @since 2.28.0
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnGoogleCloudStorageBlobstoreEnabled
@Import({
    GoogleCloudStorageBlobstoreConfiguration.class,
    GoogleCloudStorageBlobstoreAutoConfiguration.GsWebUIAutoConfiguration.class
})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.blobstore")
public class GoogleCloudStorageBlobstoreAutoConfiguration {

    public @PostConstruct void log() {
        log.info("GeoWebCache Google Cloud Storage BlobStore integration enabled");
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnGeoServerWebUIEnabled
    @ConditionalOnClass(BlobStorePage.class)
    @Import(GoogleCloudStoargeBlobStoreGsWebUIConfiguration.class)
    static class GsWebUIAutoConfiguration {}
}
