/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.cloud.event.lifecycle.LifecycleEvent;
import org.geoserver.config.plugin.GeoServerImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class LifecycleEventAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean("geoServer", GeoServerImpl.class)
            .withConfiguration(AutoConfigurations.of(LifecycleEventAutoConfiguration.class));

    @Test
    void testDefaultAppContextContributions() {
        runner.run(context -> assertThat(context).hasNotFailed().hasBean("lifecycleEventProcessor"));
    }

    @Test
    void whenDependentClassesAreNotPresent_thenBeanMissing() {
        runner.withClassLoader(new FilteredClassLoader(LifecycleEvent.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("lifecycleEventProcessor"));
    }
}
