/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.ogcapi.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.ogcapi.FilterConformanceXStreamPersisterInitializer;
import org.geoserver.ogcapi.OGCAPIXStreamPersisterInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * The {@code ogcApiLinks}, {@code cql2} and {@code ecql} metadata entries are read and written by all services. The
 * initializers teaching {@code XStreamPersister} about them must not depend on which GeoServer service the application
 * runs. See issue #872.
 */
class OgcApiCoreXStreamPersisterAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OgcApiCoreXStreamPersisterAutoConfiguration.class));

    @Test
    void contributedWithoutAnyServiceEnabled() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(OGCAPIXStreamPersisterInitializer.class)
                .hasSingleBean(FilterConformanceXStreamPersisterInitializer.class)
                .hasBean("filterConformanceXStreamInitializer"));
    }

    @Test
    void absentWithoutTheOgcApiCoreClasses() {
        runner.withClassLoader(new FilteredClassLoader(OGCAPIXStreamPersisterInitializer.class))
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(OGCAPIXStreamPersisterInitializer.class)
                        .doesNotHaveBean(FilterConformanceXStreamPersisterInitializer.class));
    }
}
