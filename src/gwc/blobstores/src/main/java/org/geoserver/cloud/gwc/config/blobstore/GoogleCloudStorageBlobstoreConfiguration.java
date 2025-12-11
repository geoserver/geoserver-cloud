/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.config.blobstore;

import org.geowebcache.storage.blobstore.gcs.GoogleCloudStorageConfigProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Original:
 *
 * <pre>{@code
 *   <bean id="GcsBlobStoreConfigProvider" class="org.geowebcache.storage.blobstore.gcs.GoogleCloudStorageConfigProvider" depends-on="gwcSynchEnv" >
 *     <description>
 *       Contributes XStream configuration settings to org.geowebcache.config.XMLConfiguration to encode GoogleCloudStorageBlobStoreInfo instances
 *     </description>
 *   </bean>
 *
 *   <bean class="org.geoserver.gwc.blob.gcs.web.GcsBlobStoreType" />
 *
 * }</pre>
 *
 * @since 2.28
 */
@Configuration(proxyBeanMethods = false)
public class GoogleCloudStorageBlobstoreConfiguration {

    @Bean(name = "GcsBlobStoreConfigProvider")
    @SuppressWarnings("java:S6830") // keeping the original bean name from xml
    GoogleCloudStorageConfigProvider googleCloudStorageConfigProvider() {
        return new GoogleCloudStorageConfigProvider();
    }
}
