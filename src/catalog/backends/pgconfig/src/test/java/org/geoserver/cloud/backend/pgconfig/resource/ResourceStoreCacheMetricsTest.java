/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.geoserver.platform.resource.Resource.Type.DIRECTORY;
import static org.geoserver.platform.resource.Resource.Type.RESOURCE;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResourceStoreCacheMetricsTest {

    private static final Tags TAGS =
            Tags.of("cache", "gs-resources", "cache.manager", "pgconfig", "name", "gs-resources");
    private static final byte[] CONTENT = "<sld/>".getBytes(UTF_8);

    private final ResourceRow root = new ResourceRow(0, 0, DIRECTORY, "", 1_000, 0);
    private final ResourceRow style = new ResourceRow(1, 0, RESOURCE, "a.sld", 1_000, CONTENT.length);
    private final ResourceStoreCache cache =
            new ResourceStoreCache("gs-resources", () -> List.of(root, style), 1024 * 1024);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @Test
    void metricsRegistered() {
        new ResourceStoreCacheMetrics(cache).bindTo(registry);

        cache.find("a.sld");
        cache.content(style.id(), () -> CONTENT);
        cache.contentCache().cleanUp();

        assertThat(registry.find("cache.gets").tags(TAGS).functionCounters()).isNotEmpty();
        Gauge weight = registry.get("cache.weight").tags(TAGS).gauge();
        assertThat(weight.value()).isEqualTo(CONTENT.length + 32.0);
        Gauge rows = registry.get("resourcestore.index.rows").tags(TAGS).gauge();
        assertThat(rows.value()).isEqualTo(2.0);
    }
}
