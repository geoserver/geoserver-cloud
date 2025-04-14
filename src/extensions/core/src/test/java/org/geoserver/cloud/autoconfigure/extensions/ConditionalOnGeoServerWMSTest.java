/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions;

import org.geoserver.wms.DefaultWebMapService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for {@link ConditionalOnGeoServerWMS}.
 */
class ConditionalOnGeoServerWMSTest extends AbstractConditionalTest {

    // No setup needed

    @Test
    void testConditionalActivation() {
        verifyConditionalActivation(
                createContextRunner().withUserConfiguration(WmsTestConfiguration.class),
                "geoserver.service.wms.enabled",
                ConditionalTestComponent.class);
    }

    @Configuration
    static class WmsTestConfiguration {
        @Bean
        DefaultWebMapService wmsService() {
            return Mockito.mock(DefaultWebMapService.class);
        }

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
