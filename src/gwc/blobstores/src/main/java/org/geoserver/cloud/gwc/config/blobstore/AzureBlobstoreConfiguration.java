/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.config.blobstore;

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
public class AzureBlobstoreConfiguration {

    @Bean(name = "AzureBlobStoreConfigProvider")
    AzureBlobStoreConfigProvider azureBlobStoreConfigProvider() {
        return new AzureBlobStoreConfigProvider();
    }
}
