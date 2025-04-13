/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions;

import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for {@link ConditionalOnGeoServerWCS}.
 */
class ConditionalOnGeoServerWCSTest extends AbstractConditionalTest {

    // No setup needed

    @Test
    void testConditionalActivation() {
        verifyConditionalActivation(
                createContextRunner().withUserConfiguration(WcsTestConfiguration.class),
                "geoserver.service.wcs.enabled",
                ConditionalTestComponent.class);
    }

    @Configuration
    static class WcsTestConfiguration {
        @Bean
        CoverageResponseDelegateFinder coverageResponseDelegateFinder() {
            return Mockito.mock(CoverageResponseDelegateFinder.class);
        }

        @Bean
        @ConditionalOnGeoServerWCS
        ConditionalTestComponent conditionalComponent() {
            return new ConditionalTestComponent();
        }
    }

    static class ConditionalTestComponent {
        // Simple component for testing conditional activation
    }
}
