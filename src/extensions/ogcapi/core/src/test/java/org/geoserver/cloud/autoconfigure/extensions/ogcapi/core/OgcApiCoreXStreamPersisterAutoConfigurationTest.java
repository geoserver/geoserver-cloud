/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.ogcapi.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.ogcapi.OGCAPIXStreamPersisterInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The {@code ogcApiLinks} metadata entries are read and written by every service, so the initializer that teaches
 * {@code XStreamPersister} about them must not depend on which GeoServer service the application runs. See issue #872.
 */
class OgcApiCoreXStreamPersisterAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OgcApiCoreXStreamPersisterAutoConfiguration.class));

    @Test
    void contributedWithoutAnyServiceEnabled() {
        runner.run(
                context -> assertThat(context).hasNotFailed().hasSingleBean(OGCAPIXStreamPersisterInitializer.class));
    }

    @Test
    void backsOffWhenTheOgcApiCoreConfigurationAlreadyContributedIt() {
        runner.withUserConfiguration(ComponentScannedInitializer.class).run(context -> assertThat(context)
                .as("the services running an OGC API must keep a single initializer")
                .hasNotFailed()
                .hasSingleBean(OGCAPIXStreamPersisterInitializer.class)
                .hasBean("OGCAPIXStreamPersisterInitializer"));
    }

    @Test
    void absentWithoutTheOgcApiCoreClasses() {
        runner.withClassLoader(new FilteredClassLoader(OGCAPIXStreamPersisterInitializer.class))
                .run(context ->
                        assertThat(context).hasNotFailed().doesNotHaveBean(OGCAPIXStreamPersisterInitializer.class));
    }

    /** Stands in for the bean the {@code gs-ogcapi-core} component scan registers. */
    @Configuration
    static class ComponentScannedInitializer {

        @Bean(name = "OGCAPIXStreamPersisterInitializer")
        OGCAPIXStreamPersisterInitializer scanned() {
            return new OGCAPIXStreamPersisterInitializer();
        }
    }
}
