/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.blobstore;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnAzureBlobstoreEnabled;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoServerWebUIEnabled;
import org.geoserver.cloud.autoconfigure.gwc.blobstore.S3BlobstoreAutoConfiguration.GsWebUIAutoConfiguration;
import org.geoserver.cloud.gwc.config.blobstore.AzureBlobstoreConfiguration;
import org.geoserver.cloud.gwc.config.blobstore.AzureBlobstoreGsWebUIConfiguration;
import org.geoserver.gwc.web.blob.BlobStorePage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;

/**
 * @see ConditionalOnAzureBlobstoreEnabled
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnAzureBlobstoreEnabled
@Import({AzureBlobstoreConfiguration.class, GsWebUIAutoConfiguration.class})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.blobstore")
public class AzureBlobstoreAutoConfiguration {

    public @PostConstruct void log() {
        log.info("GeoWebCache Azure BlobStore integration enabled");
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnGeoServerWebUIEnabled
    @ConditionalOnClass(BlobStorePage.class)
    @Import(AzureBlobstoreGsWebUIConfiguration.class)
    static class GsWebUIAutoConfiguration {}
}
