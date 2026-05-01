/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.wps.DefaultWebProcessingService;
import org.geoserver.wps.resource.WPSResourceManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Tests for {@link ConditionalOnGeoServerWPS}. */
class ConditionalOnGeoServerWPSTest extends AbstractConditionalTest {

    // No setup needed

    @Test
    void testConditionalActivation() {
        verifyConditionalActivation(
                createContextRunner()
                        .withBean("wpsResourceManager", WPSResourceManager.class, () -> mock(WPSResourceManager.class))
                        .withUserConfiguration(WpsTestConfiguration.class),
                "geoserver.service.wps.enabled",
                ConditionalTestComponent.class);
    }

    @Test
    void testConditionalActivationWithFilteredClassLoader() {
        createContextRunner()
                .withBean("wpsResourceManager", WPSResourceManager.class, () -> mock(WPSResourceManager.class))
                .withClassLoader(new FilteredClassLoader(DefaultWebProcessingService.class))
                .withUserConfiguration(WpsTestConfiguration.class)
                .withPropertyValues("geoserver.service.wps.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(ConditionalTestComponent.class));
    }

    @Configuration
    static class WpsTestConfiguration {
        @Bean
        @ConditionalOnGeoServerWPS
        ConditionalTestComponent conditionalComponent() {
            return new ConditionalTestComponent();
        }
    }

    static class ConditionalTestComponent {
        // Simple component for testing conditional activation
    }
}
