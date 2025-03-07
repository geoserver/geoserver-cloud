/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.inspire.webui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.geoserver.cloud.autoconfigure.extensions.inspire.InspireAutoConfigurationTest;
import org.geoserver.web.GeoServerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class InspireAutoConfigurationWebUITest extends InspireAutoConfigurationTest {

    @Override
    protected ApplicationContextRunner createContextRunner(File tempDir) {
        return super.createContextRunner(tempDir)
                .withBean("geoServerApplication", GeoServerApplication.class)
                .withConfiguration(AutoConfigurations.of(InspireAutoConfigurationWebUI.class));
    }

    @Test
    void testDisabledByDefault() {
        runner.run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .doesNotHaveBean("inspireWmsAdminPanel")
                    .doesNotHaveBean("inspireWfsAdminPanel")
                    .doesNotHaveBean("inspireWcsAdminPanel")
                    .doesNotHaveBean("inspireWmtsAdminPanel")
                    .doesNotHaveBean(org.geoserver.inspire.LanguagesDispatcherCallback.class)
                    .doesNotHaveBean(org.geoserver.inspire.InspireDirectoryManager.class);
        });
    }

    @Test
    void testDisabledExplicitly() {
        runner.withPropertyValues("geoserver.extension.inspire.enabled=false", "geoserver.service.webui.enabled=true")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .doesNotHaveBean("inspireWmsAdminPanel")
                            .doesNotHaveBean("inspireWfsAdminPanel")
                            .doesNotHaveBean("inspireWcsAdminPanel")
                            .doesNotHaveBean("inspireWmtsAdminPanel")
                            .doesNotHaveBean(org.geoserver.inspire.LanguagesDispatcherCallback.class)
                            .doesNotHaveBean(org.geoserver.inspire.InspireDirectoryManager.class);
                });
    }

    @Test
    void testEnabled() {
        runner.withPropertyValues("geoserver.extension.inspire.enabled=true", "geoserver.service.webui.enabled=true")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasBean("inspireWmsAdminPanel")
                            .hasBean("inspireWfsAdminPanel")
                            .hasBean("inspireWcsAdminPanel")
                            .hasBean("inspireWmtsAdminPanel")
                            .hasSingleBean(org.geoserver.inspire.LanguagesDispatcherCallback.class)
                            .hasSingleBean(org.geoserver.inspire.InspireDirectoryManager.class);
                });
    }
}
