/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.inspire.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.geoserver.cloud.autoconfigure.extensions.inspire.InspireAutoConfigurationTestSupport.createContextRunner;

import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class InspireAutoConfigurationWmsTest {

    private ApplicationContextRunner runner;

    @BeforeEach
    void setUp(@TempDir File tempDir) {
        runner = createContextRunner(tempDir)
                .withConfiguration(AutoConfigurations.of(InspireAutoConfigurationWms.class));
    }

    @Test
    void testDisabledByDefault() {
        runner.run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .doesNotHaveBean(org.geoserver.inspire.wmts.WMTSExtendedCapabilitiesProvider.class)
                    .doesNotHaveBean(org.geoserver.inspire.LanguagesDispatcherCallback.class)
                    .doesNotHaveBean(org.geoserver.inspire.InspireDirectoryManager.class);
        });
    }

    @Test
    void testDisabledExplicitly() {
        runner.withPropertyValues("geoserver.extension.inspire.enabled=false", "geoserver.service.wms.enabled=true")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .doesNotHaveBean(org.geoserver.inspire.wcs.WCSExtendedCapabilitiesProvider.class)
                            .doesNotHaveBean(org.geoserver.inspire.LanguagesDispatcherCallback.class)
                            .doesNotHaveBean(org.geoserver.inspire.InspireDirectoryManager.class);
                });
    }

    @Test
    void testEnabled() {
        runner.withPropertyValues("geoserver.extension.inspire.enabled=true", "geoserver.service.wms.enabled=true")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(org.geoserver.inspire.wms.WMSExtendedCapabilitiesProvider.class)
                            .hasSingleBean(org.geoserver.inspire.LanguagesDispatcherCallback.class)
                            .hasSingleBean(org.geoserver.inspire.InspireDirectoryManager.class);
                });
    }
}
