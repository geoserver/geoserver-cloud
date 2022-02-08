/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.blobstore;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnAzureBlobstoreEnabled;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoServerWebUIEnabled;
import org.geoserver.gwc.web.blob.AzureBlobStoreType;
import org.geowebcache.azure.AzureBlobStoreConfigProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Original:
 *
 * <pre>{@code
 * <bean id="AzureBlobStoreConfigProvider" class=
 * "org.geowebcache.azure.AzureBlobStoreConfigProvider">
 *     <description>
 *       Contributes XStream configuration settings to org.geowebcache.config.XMLConfiguration to encode AzureBlobStoreInfo instances
 *     </description>
 *   </bean>
 *
 *   <bean class="org.geoserver.gwc.web.blob.AzureBlobStoreType" />
 *
 * }</pre>
 *
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnAzureBlobstoreEnabled
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.blobstore")
public class AzureBlobstoreAutoConfiguration {

    public @PostConstruct void log() {
        log.info("GeoWebCache Azure BlobStore integration enabled");
    }

    @Bean(name = "AzureBlobStoreConfigProvider")
    public AzureBlobStoreConfigProvider azureBlobStoreConfigProvider() {
        return new AzureBlobStoreConfigProvider();
    }

    @Bean(name = "AzureBlobStoreType")
    @ConditionalOnGeoServerWebUIEnabled
    public AzureBlobStoreType azureBlobStoreType() {
        return new AzureBlobStoreType();
    }
}
