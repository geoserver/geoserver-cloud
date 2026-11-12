/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheProperties.Compression;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class EntityCachePropertiesTest {

    private static final String PREFIX = "geoserver.backend.pgconfig.cache";

    @Test
    void defaultValues() {
        EntityCacheProperties properties = new EntityCacheProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getMaxHeapPercent()).isEqualTo(5);
        assertThat(properties.getCompression()).isEqualTo(Compression.ZSTD);
        assertThat(properties.getMissTtl()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.isListingWriteThrough()).isFalse();
        assertThat(properties.getRequest().isEnabled()).isTrue();
        assertThat(properties.getRequest().getMaxEntries()).isEqualTo(10_000);
    }

    // a configuration written for a release with entry expiry still starts
    @Test
    void entryTtlIgnored() {
        Map<String, String> values = Map.of(PREFIX + ".entry-ttl", "5m", PREFIX + ".miss-ttl", "5s");
        Binder binder = new Binder(new MapConfigurationPropertySource(values));

        BindResult<EntityCacheProperties> bound = binder.bind(PREFIX, EntityCacheProperties.class);

        assertThat(bound.get().getMissTtl()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void bindsLowercaseValues() {
        Map<String, String> values = Map.of(
                PREFIX + ".compression", "none",
                PREFIX + ".max-heap-percent", "2",
                PREFIX + ".miss-ttl", "5s",
                PREFIX + ".listing-write-through", "true",
                PREFIX + ".request.max-entries", "50");
        Binder binder = new Binder(new MapConfigurationPropertySource(values));

        BindResult<EntityCacheProperties> bound = binder.bind(PREFIX, EntityCacheProperties.class);
        EntityCacheProperties properties = bound.get();

        assertThat(properties.getCompression()).isEqualTo(Compression.NONE);
        assertThat(properties.getMaxHeapPercent()).isEqualTo(2);
        assertThat(properties.getMissTtl()).isEqualTo(Duration.ofSeconds(5));
        assertThat(properties.isListingWriteThrough()).isTrue();
        assertThat(properties.getRequest().getMaxEntries()).isEqualTo(50);
    }
}
