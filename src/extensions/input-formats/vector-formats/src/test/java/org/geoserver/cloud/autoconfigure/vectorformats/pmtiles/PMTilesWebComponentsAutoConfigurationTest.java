/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.vectorformats.pmtiles;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.web.GeoServerApplication;
import org.geotools.autoconfigure.vectorformats.DataAccessFactoryFilteringAutoConfiguration;
import org.geotools.pmtiles.store.PMTilesDataStoreFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Test suite for {@link PMTilesWebComponentsAutoConfiguration}
 *
 * @since 2.28.0
 */
class PMTilesWebComponentsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataAccessFactoryFilteringAutoConfiguration.class, PMTilesWebComponentsAutoConfiguration.class));

    @Test
    void testConditionalOnGeoServerWebUI() {
        contextRunner.run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean("pmtilesDataStorePanel")
                .doesNotHaveBean("aclSwitchFieldCssContribution"));

        contextRunner
                .withPropertyValues("geoserver.service.webui.enabled=true")
                .withClassLoader(new FilteredClassLoader(GeoServerApplication.class))
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean("pmtilesDataStorePanel")
                        .doesNotHaveBean("aclSwitchFieldCssContribution"));

        contextRunner.withPropertyValues("geoserver.service.webui.enabled=true").run(context -> assertThat(context)
                .hasNotFailed()
                .hasBean("pmtilesDataStorePanel")
                .hasBean("aclSwitchFieldCssContribution"));
    }

    @Test
    void testConditionalPMTilesStoreFactoryClass() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(PMTilesDataStoreFactory.class))
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean("pmtilesDataStorePanel")
                        .doesNotHaveBean("aclSwitchFieldCssContribution"));
    }

    @Test
    void testConditionalPMTilesExtension() {
        contextRunner
                .withPropertyValues("geoserver.extension.pmtiles.enabled=false")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean("pmtilesDataStorePanel")
                        .doesNotHaveBean("aclSwitchFieldCssContribution"));
    }
}
