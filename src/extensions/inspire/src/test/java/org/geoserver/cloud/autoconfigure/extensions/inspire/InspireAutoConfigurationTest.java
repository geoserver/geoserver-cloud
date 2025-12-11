/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.inspire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.geoserver.cloud.autoconfigure.extensions.inspire.InspireAutoConfigurationTestSupport.createContextRunner;

import java.io.File;
import org.geoserver.platform.ModuleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class InspireAutoConfigurationTest {

    protected ApplicationContextRunner runner;

    @BeforeEach
    void setUp(@TempDir File tempDir) {
        runner = createContextRunner(tempDir);
    }

    /**
     *
     */
    @Test
    void testDisabledByDefault() {
        runner.run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasBean("inspireExtension")
                    .getBean("inspireExtension", ModuleStatus.class)
                    .hasFieldOrPropertyWithValue("available", true)
                    .hasFieldOrPropertyWithValue("enabled", false);
        });
    }

    @Test
    void testDisabledExplicitly() {
        runner.withPropertyValues("geoserver.extension.inspire.enabled=false").run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasBean("inspireExtension")
                    .getBean("inspireExtension", ModuleStatus.class)
                    .hasFieldOrPropertyWithValue("available", true)
                    .hasFieldOrPropertyWithValue("enabled", false);
        });
    }

    @Test
    void testEnabled() {
        runner.withPropertyValues("geoserver.extension.inspire.enabled=true").run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasBean("inspireExtension")
                    .getBean("inspireExtension", ModuleStatus.class)
                    .hasFieldOrPropertyWithValue("available", true)
                    .hasFieldOrPropertyWithValue("enabled", true);
        });
    }
}
