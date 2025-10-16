/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.config.blobstore;

import org.geoserver.gwc.blob.gcs.web.GcsBlobStoreType;
import org.geoserver.platform.ModuleStatusImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GoogleCloudStoargeBlobStoreGsWebUIConfiguration {

    @Bean
    GcsBlobStoreType gcsBlobStoreType() {
        return new GcsBlobStoreType();
    }

    @Bean
    ModuleStatusImpl gwcGcsExtension() {
        ModuleStatusImpl module = new ModuleStatusImpl();
        module.setModule("gs-gwc-gcs-blob");
        module.setName("GeoWebCache GCS Extension");
        module.setComponent("GeoWebCache Google Cloud Storage BlobStore plugin");
        module.setEnabled(true);
        module.setAvailable(true);
        return module;
    }
}
