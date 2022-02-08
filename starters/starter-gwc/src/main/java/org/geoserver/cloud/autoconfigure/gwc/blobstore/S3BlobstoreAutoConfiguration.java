/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.blobstore;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoServerWebUIEnabled;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnS3BlobstoreEnabled;
import org.geoserver.gwc.web.blob.S3BlobStoreType;
import org.geoserver.platform.ModuleStatusImpl;
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
@ConditionalOnS3BlobstoreEnabled
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.blobstore")
public class S3BlobstoreAutoConfiguration {

    public @PostConstruct void log() {
        log.info("GeoWebCache S3BlobStore integration enabled");
    }

    @Bean(name = "S3BlobStoreConfigProvider")
    public S3BlobStoreConfigProvider s3BlobStoreConfigProvider() {
        return new S3BlobStoreConfigProvider();
    }

    @Bean(name = "S3BlobStoreType")
    @ConditionalOnGeoServerWebUIEnabled
    public S3BlobStoreType s3BlobStoreType() {
        return new S3BlobStoreType();
    }

    @Bean(name = "GWC-S3Extension")
    @ConditionalOnGeoServerWebUIEnabled
    public ModuleStatusImpl gwcS3Extension() {
        ModuleStatusImpl module = new ModuleStatusImpl();
        module.setModule("gs-gwc-s3");
        module.setName("GeoWebCache S3 Extension");
        module.setComponent("GeoWebCache S3 support plugin");
        module.setEnabled(true);
        module.setAvailable(true);
        return module;
    }
}
