/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.web.GeoServerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for {@link ConditionalOnGeoServerWebUI}.
 */
class ConditionalOnGeoServerWebUITest extends AbstractConditionalTest {

    // No setup needed

    @Test
    void testConditionalActivation() {
        // Test the property-based condition like other tests
        verifyConditionalActivation(
                createContextRunner().withUserConfiguration(WebUITestConfiguration.class),
                "geoserver.service.webui.enabled",
                ConditionalTestComponent.class);
    }

    @Test
    void testConditionalActivationWithFilteredClassLoader() {
        // Test with FilteredClassLoader to exclude GeoServerApplication
        createContextRunner()
                .withClassLoader(new FilteredClassLoader(GeoServerApplication.class))
                .withUserConfiguration(WebUITestConfiguration.class)
                .withPropertyValues("geoserver.service.webui.enabled=true")
                .run(context -> {
                    // Even with the property set to true, the bean should not be created
                    // because the GeoServerApplication class is not available
                    assertThat(context).doesNotHaveBean(ConditionalTestComponent.class);
                });
    }

    @Configuration
    static class WebUITestConfiguration {
        @Bean
        @ConditionalOnGeoServerWebUI
        ConditionalTestComponent conditionalComponent() {
            return new ConditionalTestComponent();
        }
    }

    static class ConditionalTestComponent {
        // Simple component for testing conditional activation
    }
}
