/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions;

import org.geoserver.rest.security.RestConfigXStreamPersister;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for {@link ConditionalOnGeoServerREST}.
 */
class ConditionalOnGeoServerRESTTest extends AbstractConditionalTest {

    // No setup needed

    @Test
    void testConditionalActivation() {
        verifyConditionalActivation(
                createContextRunner().withUserConfiguration(RestTestConfiguration.class),
                "geoserver.service.restconfig.enabled",
                ConditionalTestComponent.class);
    }

    @Configuration
    static class RestTestConfiguration {
        @Bean
        RestConfigXStreamPersister restConfigXStreamPersister() {
            return Mockito.mock(RestConfigXStreamPersister.class);
        }

        @Bean
        @ConditionalOnGeoServerREST
        ConditionalTestComponent conditionalComponent() {
            return new ConditionalTestComponent();
        }
    }

    static class ConditionalTestComponent {
        // Simple component for testing conditional activation
    }
}
