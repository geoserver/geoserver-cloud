/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Tests for {@link ConditionalOnGeoServerWCS}. */
class ConditionalOnGeoServerWCSTest extends AbstractConditionalTest {

    // No setup needed

    @Test
    void testConditionalActivation() {
        verifyConditionalActivation(
                createContextRunner().withUserConfiguration(WcsTestConfiguration.class),
                "geoserver.service.wcs.enabled",
                ConditionalTestComponent.class);
    }

    @Test
    void testConditionalActivationWithFilteredClassLoader() {
        createContextRunner()
                .withClassLoader(new FilteredClassLoader(CoverageResponseDelegateFinder.class))
                .withUserConfiguration(WcsTestConfiguration.class)
                .withPropertyValues("geoserver.service.wcs.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(ConditionalTestComponent.class));
    }

    @Configuration
    static class WcsTestConfiguration {
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
