/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheContextRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/** @since 3.0.3 */
class GwcWMTSServiceAutoConfigurationTest {

    @TempDir
    File tmpDir;

    WebApplicationContextRunner runner;

    @BeforeEach
    void setUp() {
        runner = GeoWebCacheContextRunner.newMinimalGeoWebCacheContextRunner(tmpDir)
                .withConfiguration(AutoConfigurations.of(GwcWMTSServiceAutoConfiguration.class));
    }

    @Test
    void disabledByDefault() {
        runner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("gwcServiceWMTSTarget"));
    }

    @Test
    void enabled() {
        runner.withPropertyValues("gwc.services.wmts=true")
                .run(context -> assertThat(context).hasNotFailed().hasBean("gwcServiceWMTSTarget"));
    }

    @Test
    void disabledWhenGwcDisabled() {
        runner.withPropertyValues("gwc.enabled=false", "gwc.services.wmts=true")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("gwcServiceWMTSTarget"));
    }
}
