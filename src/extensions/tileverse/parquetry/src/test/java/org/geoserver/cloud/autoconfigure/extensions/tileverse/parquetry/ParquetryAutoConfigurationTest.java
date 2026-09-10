/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.tileverse.parquetry;

import static org.assertj.core.api.Assertions.assertThat;

import io.tileverse.parquetry.geotools.parquet.GeoParquetDataStoreFactory;
import org.geoserver.platform.ModuleStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Test suite for {@link ParquetryAutoConfiguration} */
class ParquetryAutoConfigurationTest {

    private static final String PREFIX = "geoserver.extension.tileverse.parquetry";

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ParquetryAutoConfiguration.class));

    @Test
    void propertiesDefaultToEverythingEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(ParquetryConfigProperties.class);
            ParquetryConfigProperties config = context.getBean(ParquetryConfigProperties.class);
            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getParquet().isEnabled()).isTrue();
            assertThat(config.getOutputFormats().getGeoparquet().isEnabled()).isTrue();
            assertThat(config.getOutputFormats().getArrowIpc().isEnabled()).isTrue();
            assertThat(config.anyEnabled()).isTrue();
        });
    }

    @Test
    void propertiesBindKebabCaseNames() {
        contextRunner
                .withPropertyValues(
                        PREFIX + ".parquet.enabled=false",
                        PREFIX + ".output-formats.geoparquet.enabled=false",
                        PREFIX + ".output-formats.arrow-ipc.enabled=false")
                .run(context -> {
                    ParquetryConfigProperties config = context.getBean(ParquetryConfigProperties.class);
                    assertThat(config.isEnabled()).isTrue();
                    assertThat(config.getParquet().isEnabled()).isFalse();
                    assertThat(config.getOutputFormats().getGeoparquet().isEnabled())
                            .isFalse();
                    assertThat(config.getOutputFormats().getArrowIpc().isEnabled())
                            .isFalse();
                });
    }

    @Test
    void moduleStatusEnabledByDefault() {
        contextRunner.run(context -> assertModuleStatusEnabled(context, true));
    }

    @Test
    void moduleStatusDisabledWhenUmbrellaIsOff() {
        contextRunner
                .withPropertyValues(PREFIX + ".enabled=false")
                .run(context -> assertModuleStatusEnabled(context, false));
    }

    @Test
    void moduleStatusDisabledWhenEveryFeatureIsOff() {
        contextRunner
                .withPropertyValues(
                        PREFIX + ".parquet.enabled=false",
                        PREFIX + ".output-formats.geoparquet.enabled=false",
                        PREFIX + ".output-formats.arrow-ipc.enabled=false")
                .run(context -> assertModuleStatusEnabled(context, false));
    }

    @Test
    void moduleStatusEnabledWhileOneFeatureIsOn() {
        contextRunner
                .withPropertyValues(
                        PREFIX + ".parquet.enabled=false", PREFIX + ".output-formats.geoparquet.enabled=false")
                .run(context -> assertModuleStatusEnabled(context, true));
    }

    @Test
    void backsOffWithoutParquetryClasses() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(GeoParquetDataStoreFactory.class))
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean("parquetryExtension")
                        .doesNotHaveBean(ParquetryConfigProperties.class));
    }

    private void assertModuleStatusEnabled(AssertableApplicationContext context, boolean enabled) {
        assertThat(context)
                .hasNotFailed()
                .getBean("parquetryExtension", ModuleStatus.class)
                .hasFieldOrPropertyWithValue("module", "gs-parquetry")
                .hasFieldOrPropertyWithValue("available", true)
                .hasFieldOrPropertyWithValue("enabled", enabled);
    }
}
