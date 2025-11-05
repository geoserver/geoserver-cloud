/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.catalog.backend.datadir;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.geoserver.cloud.catalog.backend.datadir.EventualConsistencyEnforcer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Test {@link RemoteEventDataDirectoryAutoConfiguration} to verify that {@link
 * EventualConsistencyEnforcer} bean is created based on the configuration property {@code
 * geoserver.backend.data-directory.eventual-consistency.enabled}.
 */
class RemoteEventDataDirectoryAutoConfigurationTest {

    static @TempDir Path datadir;

    private ApplicationContextRunner runner = new ApplicationContextRunner()
            .withAllowBeanDefinitionOverriding(true)
            .withAllowCircularReferences(true)
            .withConfiguration(AutoConfigurations.of(
                    // AutoConfigurations from gs-cloud-catalog-backend-common
                    org.geoserver.cloud.autoconfigure.geotools.GeoToolsHttpClientAutoConfiguration.class,
                    org.geoserver.cloud.autoconfigure.catalog.backend.core.GeoServerBackendAutoConfiguration.class,
                    org.geoserver.cloud.autoconfigure.catalog.backend.core.DefaultUpdateSequenceAutoConfiguration.class,
                    org.geoserver.cloud.autoconfigure.catalog.backend.core.XstreamServiceLoadersAutoConfiguration.class,
                    org.geoserver.cloud.autoconfigure.catalog.backend.core
                            .RemoteEventResourcePoolCleanupUpAutoConfiguration.class,
                    org.geoserver.cloud.autoconfigure.catalog.event.LocalCatalogEventsAutoConfiguration.class,
                    //
                    org.geoserver.cloud.autoconfigure.security.GeoServerSecurityAutoConfiguration.class,
                    org.geoserver.cloud.autoconfigure.metrics.catalog.CatalogMetricsAutoConfiguration.class,
                    // AutoConfigurations from gs-cloud-catalog-backend-datadir
                    org.geoserver.cloud.autoconfigure.catalog.backend.datadir.DataDirectoryAutoConfiguration.class,
                    org.geoserver.cloud.autoconfigure.catalog.backend.datadir.RemoteEventDataDirectoryAutoConfiguration
                            .class))
            .withPropertyValues(
                    "geoserver.backend.dataDirectory.enabled=true",
                    "geoserver.backend.dataDirectory.location=%s".formatted(datadir.toAbsolutePath()),
                    "geoserver.catalog.events.enabled=true");

    @Test
    void testEventualConsistencyEnforcerEnabledByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(EventualConsistencyEnforcer.class);
        });
    }

    @Test
    void testEventualConsistencyEnforcerExplicitlyEnabled() {
        runner.withPropertyValues("geoserver.backend.data-directory.eventual-consistency.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(EventualConsistencyEnforcer.class);
                });
    }

    @Test
    void testEventualConsistencyEnforcerDisabled() {
        runner.withPropertyValues("geoserver.backend.data-directory.eventual-consistency.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(EventualConsistencyEnforcer.class);
                });
    }
}
