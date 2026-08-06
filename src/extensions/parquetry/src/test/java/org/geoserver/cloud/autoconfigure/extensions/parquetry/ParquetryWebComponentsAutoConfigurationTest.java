/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.parquetry;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.web.GeoServerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Test suite for {@link ParquetryWebComponentsAutoConfiguration} */
class ParquetryWebComponentsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ParquetryAutoConfiguration.class, ParquetryWebComponentsAutoConfiguration.class));

    @Test
    void panelAbsentOutsideWebUiService() {
        contextRunner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("geoParquetDataStorePanel"));
    }

    @Test
    void panelAbsentWithoutWebUiClasses() {
        contextRunner
                .withPropertyValues("geoserver.service.webui.enabled=true")
                .withClassLoader(new FilteredClassLoader(GeoServerApplication.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("geoParquetDataStorePanel"));
    }

    @Test
    void panelPresentInWebUiService() {
        contextRunner
                .withPropertyValues("geoserver.service.webui.enabled=true")
                .run(context -> assertThat(context).hasNotFailed().hasBean("parquetryGeoParquetDataStorePanel"));
    }

    @Test
    void panelAbsentWhenExtensionDisabled() {
        contextRunner
                .withPropertyValues(
                        "geoserver.service.webui.enabled=true", "geoserver.extension.parquetry.enabled=false")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("geoParquetDataStorePanel"));
    }

    @Test
    void icebergAndStacPanelsNeverRegistered() {
        contextRunner.withPropertyValues("geoserver.service.webui.enabled=true").run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean("icebergDataStorePanel")
                .doesNotHaveBean("stacDataStorePanel"));
    }
}
