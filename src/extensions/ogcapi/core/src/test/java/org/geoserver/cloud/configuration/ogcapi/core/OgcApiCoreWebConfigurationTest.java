/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.configuration.ogcapi.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class OgcApiCoreWebConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withConfiguration(UserConfigurations.of(OgcApiCoreWebConfiguration.class));

    @Test
    void testConditionalOnGeoServerWEBIO() {
        runner.withPropertyValues("geoserver.service.wfs.enabled=true").run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean("ogcapiLayerConfig")
                .doesNotHaveBean("ogcapiLinksSettings"));
    }

    @Test
    void testExpectedComponents() {
        runner.withPropertyValues("geoserver.service.webui.enabled=true").run(context -> assertThat(context)
                .hasNotFailed()
                .hasBean("ogcapiLayerConfig")
                .hasBean("ogcapiLinksSettings"));
    }
}
