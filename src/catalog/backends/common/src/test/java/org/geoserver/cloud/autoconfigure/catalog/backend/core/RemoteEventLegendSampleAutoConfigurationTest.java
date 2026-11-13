/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.catalog.backend.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.catalog.Catalog;
import org.geoserver.wms.capabilities.LegendSampleImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RemoteEventLegendSampleAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean("rawCatalog", Catalog.class, () -> Mockito.mock(Catalog.class))
            .withConfiguration(AutoConfigurations.of(RemoteEventLegendSampleAutoConfiguration.class));

    @Test
    void processorDefinedWithTheClass() {
        runner.run(context -> assertThat(context).hasNotFailed().hasBean("remoteEventLegendSampleProcessor"));
    }

    @Test
    void noProcessorWithoutLegendSample() {
        runner.withClassLoader(new FilteredClassLoader(LegendSampleImpl.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("remoteEventLegendSampleProcessor"));
    }

    @Test
    void noProcessorWithoutCatalogEvents() {
        runner.withPropertyValues("geoserver.catalog.events.enabled=false")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("remoteEventLegendSampleProcessor"));
    }
}
