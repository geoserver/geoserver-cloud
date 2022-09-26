/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.pgraster;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.resource.DataStorePanelInfo;
import org.geoserver.web.data.store.pgraster.PGRasterCoverageStoreEditPanel;
import org.geotools.gce.pgraster.PostgisRasterFormat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PostgisRasterWebUIAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(PostgisRasterWebUIAutoConfiguration.class));

    @Test
    void testConditionalOnPostgisRasterWebUI_enabled() {

        contextRunner.run(
                context -> {
                    assertThat(context)
                            .hasBean("pgrasterCoverageStorePanel")
                            .getBean("pgrasterCoverageStorePanel")
                            .isInstanceOf(DataStorePanelInfo.class);

                    assertThat(
                                    context.getBean(
                                            "pgrasterCoverageStorePanel", DataStorePanelInfo.class))
                            .hasFieldOrPropertyWithValue("factoryClass", PostgisRasterFormat.class);

                    assertThat(
                                    context.getBean(
                                            "pgrasterCoverageStorePanel", DataStorePanelInfo.class))
                            .hasFieldOrPropertyWithValue(
                                    "componentClass", PGRasterCoverageStoreEditPanel.class);
                });
    }

    @Test
    void testConditionalOnPostgisRasterWebUI_disabled() {

        contextRunner
                .withClassLoader(new FilteredClassLoader(GeoServerApplication.class))
                .run(
                        context -> {
                            assertThat(context).doesNotHaveBean("pgrasterCoverageStorePanel");
                        });
    }
}
