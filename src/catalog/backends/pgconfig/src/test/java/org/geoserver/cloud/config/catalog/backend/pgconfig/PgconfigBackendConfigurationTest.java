/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.config.catalog.backend.pgconfig;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCodec;
import org.geoserver.cloud.backend.pgconfig.cache.NoopCodec;
import org.geoserver.cloud.backend.pgconfig.support.LogCapture;
import org.junit.jupiter.api.Test;

class PgconfigBackendConfigurationTest {

    private static final String CONFIGURATION_LOGGER = "org.geoserver.cloud.config.catalog.backend.pgconfig";

    // the backend must start on a platform without the native zstd library
    @Test
    void zstdFailureFallsBackToNoop() {
        try (LogCapture log = LogCapture.of(CONFIGURATION_LOGGER)) {
            EntityCodec codec = PgconfigBackendConfiguration.zstdOrUncompressed(() -> {
                throw new UnsatisfiedLinkError("no zstd-jni in java.library.path");
            });

            assertThat(codec).isInstanceOf(NoopCodec.class);
            assertThat(log.messages(Level.WARN))
                    .singleElement()
                    .asString()
                    .contains("geoserver.backend.pgconfig.cache.compression", "no zstd-jni in java.library.path");
        }
    }

    // the configuration holds few objects: a large heap does not make its cache larger than 16 MiB
    @Test
    void configCacheCappedAt16MiB() {
        long sixteenMiB = 16L * 1024 * 1024;

        assertThat(PgconfigBackendConfiguration.configCacheMaximumWeight(4 * sixteenMiB))
                .isEqualTo(sixteenMiB);
        assertThat(PgconfigBackendConfiguration.configCacheMaximumWeight(1024)).isEqualTo(1024);
    }
}
