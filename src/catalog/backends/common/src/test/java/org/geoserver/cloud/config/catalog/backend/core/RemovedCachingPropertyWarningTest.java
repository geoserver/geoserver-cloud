/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.config.catalog.backend.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class RemovedCachingPropertyWarningTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(RemovedCachingPropertyWarning.class);

    @Test
    void warnsWhenPropertyIsTrue(CapturedOutput output) {
        runner.withPropertyValues("geoserver.catalog.caching.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(RemovedCachingPropertyWarning.class));
        assertThat(output)
                .contains("geoserver.catalog.caching.enabled has no effect")
                .contains("geoserver.backend.pgconfig.cache.enabled");
    }

    // a user who turned the old cache off gets the pgconfig caches by default, and needs the warning too
    @Test
    void warnsWhenPropertyIsFalse() {
        runner.withPropertyValues("geoserver.catalog.caching.enabled=false")
                .run(context -> assertThat(context).hasSingleBean(RemovedCachingPropertyWarning.class));
    }

    @Test
    void silentWithoutProperty() {
        runner.run(context -> assertThat(context).doesNotHaveBean(RemovedCachingPropertyWarning.class));
    }
}
