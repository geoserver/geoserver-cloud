/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.tileverse.parquetry;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.web.GeoServerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Test suite for {@link ParquetryWebComponentsAutoConfiguration} */
class ParquetryWebComponentsAutoConfigurationTest {

    private static final String PREFIX = "geoserver.extension.tileverse.parquetry";
    private static final String PANEL = "parquetryGeoParquetDataStorePanel";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ParquetryAutoConfiguration.class, ParquetryWebComponentsAutoConfiguration.class));

    private final ApplicationContextRunner webUiRunner =
            contextRunner.withPropertyValues("geoserver.service.webui.enabled=true");

    @Test
    void panelAbsentOutsideWebUiService() {
        contextRunner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(PANEL));
    }

    @Test
    void panelAbsentWithoutWebUiClasses() {
        webUiRunner
                .withClassLoader(new FilteredClassLoader(GeoServerApplication.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(PANEL));
    }

    @Test
    void panelPresentInWebUiService() {
        webUiRunner.run(context -> assertThat(context).hasNotFailed().hasBean(PANEL));
    }

    @Test
    void panelAbsentWhenUmbrellaIsOff() {
        webUiRunner
                .withPropertyValues(PREFIX + ".enabled=false")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(PANEL));
    }

    @Test
    void panelAbsentWhenParquetStoreIsOff() {
        webUiRunner
                .withPropertyValues(PREFIX + ".parquet.enabled=false")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(PANEL));
    }

    @Test
    void panelPresentWhileOnlyOutputFormatsAreOff() {
        webUiRunner
                .withPropertyValues(
                        PREFIX + ".output-formats.geoparquet.enabled=false",
                        PREFIX + ".output-formats.arrow-ipc.enabled=false")
                .run(context -> assertThat(context).hasNotFailed().hasBean(PANEL));
    }

    @Test
    void icebergAndStacPanelsNeverRegistered() {
        webUiRunner.run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean("parquetryIcebergDataStorePanel")
                .doesNotHaveBean("parquetryStacDataStorePanel"));
    }
}
