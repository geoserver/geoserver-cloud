/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYER;

import java.time.Duration;
import java.util.List;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup.Absent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.listener.ConsumeOkEvent;

class EntityCacheBusListenerTest {

    private static final long ONE_MIB = 1024 * 1024;
    private static final CacheKey KEY = CacheKey.entity(LAYER, "layer-1");

    // the events sent while the consumer was not subscribed are lost
    @Test
    void subscriptionClearsCaches() {
        List<EntityCache> caches = List.of(newCache("gs-catalog"), newCache("gs-config"));
        for (EntityCache cache : caches) {
            cache.put(KEY, "{}".getBytes(UTF_8), cache.epoch());
        }
        EntityCacheBusListener listener = new EntityCacheBusListener(caches);

        listener.onConsumerSubscribed(new ConsumeOkEvent(this, "geoserver.anonymous.queue", "consumer-1"));

        assertThat(caches).allMatch(cache -> cache.lookup(KEY) instanceof Absent, "empty");
    }

    private static EntityCache newCache(String name) {
        return new EntityCache(name, ONE_MIB, Duration.ofSeconds(30), new NoopCodec());
    }
}
