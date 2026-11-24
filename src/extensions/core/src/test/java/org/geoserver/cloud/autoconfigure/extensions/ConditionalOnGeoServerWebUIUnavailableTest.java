/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Tests for {@link ConditionalOnGeoServerWebUIUnavailable}. */
class ConditionalOnGeoServerWebUIUnavailableTest extends AbstractConditionalTest {

    @Test
    void enabledWhenWebUIServiceNotInList() {
        createContextRunner()
                .withUserConfiguration(WebUIUnavailableTestConfiguration.class)
                .withPropertyValues("geoserver.services=wms")
                .run(context -> assertThat(context).hasBean("conditionalComponent"));
    }

    @Test
    void disabledWhenWebUIServiceInList() {
        createContextRunner()
                .withUserConfiguration(WebUIUnavailableTestConfiguration.class)
                .withPropertyValues("geoserver.services=webui")
                .run(context -> assertThat(context).doesNotHaveBean("conditionalComponent"));
    }

    @Test
    void disabledWhenWebUIServiceExplicitlyEnabled() {
        createContextRunner()
                .withUserConfiguration(WebUIUnavailableTestConfiguration.class)
                .withPropertyValues("geoserver.service.webui.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean("conditionalComponent"));
    }

    @Configuration
    static class WebUIUnavailableTestConfiguration {
        @Bean
        @ConditionalOnGeoServerWebUIUnavailable
        String conditionalComponent() {
            return "service-only";
        }
    }
}
