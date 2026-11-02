/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.tileverse.parquetry;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.wfs.DefaultWebFeatureService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Test suite for {@link ParquetryArrowIpcOutputFormatAutoConfiguration} */
class ParquetryArrowIpcOutputFormatAutoConfigurationTest {

    private static final String PREFIX = "geoserver.extension.tileverse.parquetry";
    private static final String FORMAT = "arrowIpcOutputFormat";
    private static final String STATUS = "arrowIpcWfsOutputFormatModuleStatus";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean("geoServer", GeoServer.class, GeoServerImpl::new)
            .withConfiguration(AutoConfigurations.of(
                    ParquetryAutoConfiguration.class, ParquetryArrowIpcOutputFormatAutoConfiguration.class));

    private final ApplicationContextRunner wfsRunner =
            contextRunner.withPropertyValues("geoserver.service.wfs.enabled=true");

    private final ApplicationContextRunner webUiRunner =
            contextRunner.withPropertyValues("geoserver.service.webui.enabled=true");

    @Test
    void formatPresentInWfsService() {
        wfsRunner.run(
                context -> assertThat(context).hasNotFailed().hasBean(FORMAT).hasBean(STATUS));
    }

    @Test
    void formatPresentInWebUiService() {
        webUiRunner.run(
                context -> assertThat(context).hasNotFailed().hasBean(FORMAT).hasBean(STATUS));
    }

    @Test
    void formatAbsentInOtherServices() {
        contextRunner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(FORMAT));
    }

    @Test
    void formatAbsentWithoutWfsClasses() {
        wfsRunner
                .withClassLoader(new FilteredClassLoader(DefaultWebFeatureService.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(FORMAT));
    }

    @Test
    void formatAbsentWhenFeatureIsOff() {
        wfsRunner
                .withPropertyValues(PREFIX + ".output-formats.arrow-ipc.enabled=false")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(FORMAT)
                        .doesNotHaveBean(STATUS));
    }

    @Test
    void formatAbsentWhenUmbrellaIsOff() {
        wfsRunner
                .withPropertyValues(PREFIX + ".enabled=false")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(FORMAT));
    }

    @Test
    void formatPresentWhileSiblingFeaturesAreOff() {
        wfsRunner
                .withPropertyValues(
                        PREFIX + ".parquet.enabled=false", PREFIX + ".output-formats.geoparquet.enabled=false")
                .run(context -> assertThat(context).hasNotFailed().hasBean(FORMAT));
    }
}
