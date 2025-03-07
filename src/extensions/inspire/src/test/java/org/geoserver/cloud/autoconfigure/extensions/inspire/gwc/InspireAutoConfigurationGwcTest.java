/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.inspire.gwc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.geoserver.cloud.autoconfigure.extensions.inspire.InspireAutoConfigurationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class InspireAutoConfigurationGwcTest extends InspireAutoConfigurationTest {

    @Override
    protected ApplicationContextRunner createContextRunner(File tempDir) {
        return super.createContextRunner(tempDir)
                .withConfiguration(AutoConfigurations.of(InspireAutoConfigurationGwc.class));
    }

    @Test
    void testDisabledByDefault() {
        runner.run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .doesNotHaveBean(org.geoserver.inspire.wmts.WMTSExtendedCapabilitiesProvider.class)
                    .doesNotHaveBean(org.geoserver.inspire.LanguagesDispatcherCallback.class)
                    .doesNotHaveBean(org.geoserver.inspire.wmts.InspireGridSetLoader.class)
                    .doesNotHaveBean(org.geoserver.inspire.InspireDirectoryManager.class);
        });
    }

    @Test
    void testDisabledExplicitly() {
        runner.withPropertyValues("geoserver.extension.inspire.enabled=false", "geoserver.service.gwc.enabled=true")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .doesNotHaveBean(org.geoserver.inspire.wmts.WMTSExtendedCapabilitiesProvider.class)
                            .doesNotHaveBean(org.geoserver.inspire.LanguagesDispatcherCallback.class)
                            .doesNotHaveBean(org.geoserver.inspire.wmts.InspireGridSetLoader.class)
                            .doesNotHaveBean(org.geoserver.inspire.InspireDirectoryManager.class);
                });
    }

    @Test
    void testEnabled() {
        runner.withPropertyValues("geoserver.extension.inspire.enabled=true", "geoserver.service.gwc.enabled=true")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(org.geoserver.inspire.wmts.WMTSExtendedCapabilitiesProvider.class)
                            .hasSingleBean(org.geoserver.inspire.LanguagesDispatcherCallback.class)
                            .hasSingleBean(org.geoserver.inspire.wmts.InspireGridSetLoader.class)
                            .hasSingleBean(org.geoserver.inspire.InspireDirectoryManager.class);
                });
    }
}
