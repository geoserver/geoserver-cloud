/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.inspire;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.inspire.InspireXStreamPersisterInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * The INSPIRE spatial dataset identifiers live in metadata maps every service reads and writes, so the initializer that
 * keeps {@code XStreamPersister} from mangling them must not depend on the running service or on the extension being
 * enabled. See issue #872.
 */
class InspireXStreamPersisterAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(InspireXStreamPersisterAutoConfiguration.class));

    @Test
    void contributedWithoutTheExtensionEnabled() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(InspireXStreamPersisterInitializer.class)
                .hasBean("inspireXStreamConfigurer"));
    }

    @Test
    void absentWithoutTheInspireClasses() {
        runner.withClassLoader(new FilteredClassLoader(InspireXStreamPersisterInitializer.class))
                .run(context ->
                        assertThat(context).hasNotFailed().doesNotHaveBean(InspireXStreamPersisterInitializer.class));
    }
}
