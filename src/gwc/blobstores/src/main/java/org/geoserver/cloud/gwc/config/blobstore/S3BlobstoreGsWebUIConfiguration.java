/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.config.blobstore;

import org.geoserver.gwc.web.blob.S3BlobStoreType;
import org.geoserver.platform.ModuleStatusImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class S3BlobstoreGsWebUIConfiguration {

    @Bean
    ModuleStatusImpl gwcS3Extension() {
        ModuleStatusImpl module = new ModuleStatusImpl();
        module.setModule("gs-gwc-s3");
        module.setName("GeoWebCache S3 Extension");
        module.setComponent("GeoWebCache S3 support plugin");
        module.setEnabled(true);
        module.setAvailable(true);
        return module;
    }

    @Bean
    S3BlobStoreType s3BlobStoreType() {
        return new S3BlobStoreType();
    }
}
