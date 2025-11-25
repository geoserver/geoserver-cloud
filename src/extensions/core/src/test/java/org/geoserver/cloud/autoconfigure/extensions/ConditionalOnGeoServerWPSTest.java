/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions;

import static org.mockito.Mockito.mock;

import org.geoserver.wps.DefaultWebProcessingService;
import org.geoserver.wps.resource.WPSResourceManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for {@link ConditionalOnGeoServerWPS}.
 */
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

    @Configuration
    static class WpsTestConfiguration {
        @Bean
        DefaultWebProcessingService wpsService() {
            return Mockito.mock(DefaultWebProcessingService.class);
        }

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
