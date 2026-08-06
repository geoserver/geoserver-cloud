/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.parquetry;

import static org.assertj.core.api.Assertions.assertThat;

import io.tileverse.parquetry.geotools.parquet.GeoParquetDataStoreFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Test suite for {@link ParquetryAutoConfiguration} */
class ParquetryAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ParquetryAutoConfiguration.class));

    @Test
    void moduleStatusEnabledByDefault() {
        contextRunner.run(context -> assertThat(context)
                .hasNotFailed()
                .getBean("parquetryExtension")
                .hasFieldOrPropertyWithValue("enabled", true));
    }

    @Test
    void moduleStatusReportsDisabledExtension() {
        contextRunner
                .withPropertyValues("geoserver.extension.parquetry.enabled=false")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .getBean("parquetryExtension")
                        .hasFieldOrPropertyWithValue("enabled", false));
    }

    @Test
    void backsOffWithoutParquetryClasses() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(GeoParquetDataStoreFactory.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("parquetryExtension"));
    }
}
