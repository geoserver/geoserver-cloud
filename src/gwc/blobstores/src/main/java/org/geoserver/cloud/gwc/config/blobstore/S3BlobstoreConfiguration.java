/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.config.blobstore;

import org.geowebcache.s3.S3BlobStoreConfigProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Original:
 *
 * <pre>
 * {@code
 *   <bean id="S3BlobStoreConfigProvider" class=
 * "org.geowebcache.s3.S3BlobStoreConfigProvider" depends-on="geoWebCacheExtensions">
 *     <description>
 *       Contributes XStream configuration settings to org.geowebcache.config.XMLConfiguration to encode S3BlobStoreInfo instances
 *     </description>
 *   </bean>
 *
 *   <bean class="org.geoserver.gwc.web.blob.S3BlobStoreType" />
 *   <bean id="GWC-S3Extension" class="org.geoserver.platform.ModuleStatusImpl">
 *     <property name="module" value="gs-gwc-s3" />
 *     <property name="name" value="GeoWebCache S3 Extension"/>
 *     <property name="component" value="GeoWebCache S3 support plugin"/>
 *     <property name="available" value="true"/>
 *     <property name="enabled" value="true"/>
 *   </bean>
 *
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
public class S3BlobstoreConfiguration {

    @Bean(name = "S3BlobStoreConfigProvider")
    S3BlobStoreConfigProvider s3BlobStoreConfigProvider() {
        return new S3BlobStoreConfigProvider();
    }
}
