/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.metrics.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.config.UpdateSequence;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CatalogMetricsAutoConfigurationTest {

    private ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CatalogMetricsAutoConfiguration.class))
            .withBean("catalog", Catalog.class, () -> mock(Catalog.class))
            .withBean("geoServer", GeoServer.class, () -> mock(GeoServer.class))
            .withBean(UpdateSequence.class, () -> mock(UpdateSequence.class));

    @Test
    void enabledByDefaultWhenMeterRegistryPresent() {
        runner.withBean(SimpleMeterRegistry.class)
                .run(context -> assertThat(context).hasSingleBean(CatalogMetrics.class));
    }

    @Test
    void disabledByProperty() {
        runner.withBean(SimpleMeterRegistry.class)
                .withPropertyValues("geoserver.metrics.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(CatalogMetrics.class));
    }

    @Test
    void disabledWithoutMeterRegistry() {
        runner.run(context -> assertThat(context).doesNotHaveBean(CatalogMetrics.class));
    }
}
