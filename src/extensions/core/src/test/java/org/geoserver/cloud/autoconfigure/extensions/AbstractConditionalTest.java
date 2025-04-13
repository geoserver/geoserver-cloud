/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerImpl;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Base test class for testing conditional annotations.
 */
public abstract class AbstractConditionalTest {

    /**
     * Creates and returns a new {@link ApplicationContextRunner} with basic GeoServer context.
     */
    protected ApplicationContextRunner createContextRunner() {
        org.geoserver.config.GeoServer geoServer = new GeoServerImpl();
        return new ApplicationContextRunner().withBean("geoServer", GeoServer.class, () -> geoServer);
    }

    /**
     * Verifies that the condition properly activates based on the property.
     *
     * @param contextRunner The context runner
     * @param conditionalProperty The property name to enable the condition
     * @param conditionalBeanClass The class to register conditionally
     */
    protected void verifyConditionalActivation(
            ApplicationContextRunner contextRunner, String conditionalProperty, Class<?> conditionalBeanClass) {

        // Test without the property set - condition should not activate
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(conditionalBeanClass);
        });

        // Test with the property set to false - condition should not activate
        contextRunner.withPropertyValues(conditionalProperty + "=false").run(context -> {
            assertThat(context).doesNotHaveBean(conditionalBeanClass);
        });

        // Test with the property set to true - condition should activate
        contextRunner.withPropertyValues(conditionalProperty + "=true").run(context -> {
            assertThat(context).hasSingleBean(conditionalBeanClass);
        });
    }
}
