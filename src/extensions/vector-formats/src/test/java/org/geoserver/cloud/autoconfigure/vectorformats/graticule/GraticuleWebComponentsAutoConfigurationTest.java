/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.vectorformats.graticule;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.web.GeoServerApplication;
import org.geotools.autoconfigure.vectorformats.DataAccessFactoryFilteringAutoConfiguration;
import org.geotools.data.graticule.GraticuleDataStoreFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Test suite for {@link GraticuleWebComponentsAutoConfiguration}
 *
 * @since 2.27.0
 */
class GraticuleWebComponentsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataAccessFactoryFilteringAutoConfiguration.class, GraticuleWebComponentsAutoConfiguration.class));

    @Test
    void testConditionalOnGeoServerWebUI() {
        contextRunner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("graticuleStorePanel"));

        contextRunner
                .withPropertyValues("geoserver.service.webui.enabled=true")
                .withClassLoader(new FilteredClassLoader(GeoServerApplication.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("graticuleStorePanel"));

        contextRunner
                .withPropertyValues("geoserver.service.webui.enabled=true")
                .run(context -> assertThat(context).hasNotFailed().hasBean("graticuleStorePanel"));
    }

    @Test
    void testConditionalOnGraticule() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(GraticuleDataStoreFactory.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("graticuleStorePanel"));
    }
}
