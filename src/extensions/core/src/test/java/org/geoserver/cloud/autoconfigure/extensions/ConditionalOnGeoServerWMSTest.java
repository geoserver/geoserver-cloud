/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.wms.DefaultWebMapService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Tests for {@link ConditionalOnGeoServerWMS}. */
class ConditionalOnGeoServerWMSTest extends AbstractConditionalTest {

    // No setup needed

    @Test
    void testConditionalActivation() {
        verifyConditionalActivation(
                createContextRunner().withUserConfiguration(WmsTestConfiguration.class),
                "geoserver.service.wms.enabled",
                ConditionalTestComponent.class);
    }

    @Test
    void testConditionalActivationWithFilteredClassLoader() {
        createContextRunner()
                .withClassLoader(new FilteredClassLoader(DefaultWebMapService.class))
                .withUserConfiguration(WmsTestConfiguration.class)
                .withPropertyValues("geoserver.service.wms.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(ConditionalTestComponent.class));
    }

    @Configuration
    static class WmsTestConfiguration {
        @Bean
        @ConditionalOnGeoServerWMS
        ConditionalTestComponent conditionalComponent() {
            return new ConditionalTestComponent();
        }
    }

    static class ConditionalTestComponent {
        // Simple component for testing conditional activation
    }
}
