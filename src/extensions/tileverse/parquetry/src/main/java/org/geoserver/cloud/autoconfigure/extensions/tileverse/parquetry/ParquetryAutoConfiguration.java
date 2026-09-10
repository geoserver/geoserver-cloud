/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.tileverse.parquetry;

import io.tileverse.parquetry.geotools.parquet.GeoParquetDataStoreFactory;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServer;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the Parquetry plugin extension: binds {@link ParquetryConfigProperties} and reports the
 * plugin's overall state as the {@code gs-parquetry} module status.
 *
 * <p>The module-status bean is registered whenever the parquetry classes are present: a switched-off plugin shows up as
 * disabled instead of vanishing from the modules list. It reports enabled while the umbrella switch is on and at least
 * one feature is on, the "Parquet" datastore or one of the WFS output formats, each auto-configured on its own.
 * Datastore availability itself is controlled by the {@code geotools.data.filtering} configuration through the factory
 * display name "Parquet", and by {@link ParquetryContextInitializer}.
 *
 * @since 3.1.0
 */
@AutoConfiguration
@ConditionalOnGeoServer
@ConditionalOnClass(GeoParquetDataStoreFactory.class)
@EnableConfigurationProperties(ParquetryConfigProperties.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.tileverse.parquetry")
public class ParquetryAutoConfiguration {

    @Bean
    ModuleStatusImpl parquetryExtension(ParquetryConfigProperties config) {
        boolean enabled = config.anyEnabled();
        ModuleStatusImpl module =
                new ModuleStatusImpl("gs-parquetry", "Parquetry Plugin", "GeoParquet DataStore and WFS output formats");
        module.setAvailable(true);
        module.setEnabled(enabled);
        module.setCategory(ModuleStatus.Category.COMMUNITY);
        log.info("Parquetry extension {}", enabled ? "enabled" : "disabled");
        return module;
    }
}
