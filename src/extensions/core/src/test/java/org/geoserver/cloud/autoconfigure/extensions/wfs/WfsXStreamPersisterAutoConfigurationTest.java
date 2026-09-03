/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.wfs;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.wfs.WFSXStreamPersisterInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * A cascaded stored query configuration lives in a feature type's metadata map, which every service reads and writes,
 * so the initializer that teaches {@code XStreamPersister} about it must not depend on
 * {@code geoserver.service.wfs.enabled}. See issue #872.
 */
class WfsXStreamPersisterAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WfsXStreamPersisterAutoConfiguration.class));

    @Test
    void contributedWithoutTheWfsServiceEnabled() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(WFSXStreamPersisterInitializer.class)
                .hasBean("wfsXStreamPersisterInitializer"));
    }

    @Test
    void absentWithoutTheWfsCoreClasses() {
        runner.withClassLoader(new FilteredClassLoader(WFSXStreamPersisterInitializer.class))
                .run(context ->
                        assertThat(context).hasNotFailed().doesNotHaveBean(WFSXStreamPersisterInitializer.class));
    }
}
