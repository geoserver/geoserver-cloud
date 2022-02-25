/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.blobstore;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnAzureBlobstoreEnabled;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoServerWebUIEnabled;
import org.geoserver.gwc.web.blob.AzureBlobStoreType;
import org.geoserver.platform.ModuleStatusImpl;
import org.geowebcache.azure.AzureBlobStoreConfigProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;

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
@Import(AzureBlobstoreAutoConfiguration.AzureBlobstoreGsWebUIConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.blobstore")
public class AzureBlobstoreAutoConfiguration {

    public @PostConstruct void log() {
        log.info("GeoWebCache Azure BlobStore integration enabled");
    }

    @Bean(name = "AzureBlobStoreConfigProvider")
    public AzureBlobStoreConfigProvider azureBlobStoreConfigProvider() {
        return new AzureBlobStoreConfigProvider();
    }

    @ConditionalOnGeoServerWebUIEnabled
    static @Configuration class AzureBlobstoreGsWebUIConfiguration {

        @Bean(name = "AzureBlobStoreType")
        public AzureBlobStoreType azureBlobStoreType() {
            return new AzureBlobStoreType();
        }

        @Bean(name = "GWC-AzureExtension")
        public ModuleStatusImpl gwcAzureExtension() {
            ModuleStatusImpl module = new ModuleStatusImpl();
            module.setModule("gs-gwc-azure");
            module.setName("GeoWebCache Azure Extension");
            module.setComponent("GeoWebCache Azure BlobStore plugin");
            module.setEnabled(true);
            module.setAvailable(true);
            return module;
        }
    }
}
