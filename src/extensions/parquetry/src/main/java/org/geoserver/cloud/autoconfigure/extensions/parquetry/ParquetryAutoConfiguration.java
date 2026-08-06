/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.parquetry;

import io.tileverse.parquetry.geotools.parquet.GeoParquetDataStoreFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServer;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the Parquetry extension, contributing the "Parquet" datastore backed by parquetry-geotools.
 *
 * <p>The module-status bean is registered whenever the parquetry classes are present, reporting the state of the
 * {@code geoserver.extension.parquetry.enabled} property. Datastore availability itself is controlled by the
 * {@code geotools.data.filtering} configuration through the factory display name "Parquet".
 *
 * @since 3.1.0
 */
@AutoConfiguration
@ConditionalOnGeoServer
@ConditionalOnClass(GeoParquetDataStoreFactory.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.parquetry")
public class ParquetryAutoConfiguration {

    @Bean
    ModuleStatusImpl parquetryExtension(@Value("${geoserver.extension.parquetry.enabled:true}") boolean enabled) {
        ModuleStatusImpl module =
                new ModuleStatusImpl("gs-parquetry", "Parquetry GeoParquet Plugin", "GeoParquet DataStore");
        module.setAvailable(true);
        module.setEnabled(enabled);
        module.setCategory(ModuleStatus.Category.COMMUNITY);
        return module;
    }

    @PostConstruct
    void log() {
        log.info("Parquetry extension installed");
    }
}
