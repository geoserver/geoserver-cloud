/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.geoserver.cloud.event.info.ConfigInfoType.COVERAGESTORE;
import static org.geoserver.cloud.event.info.ConfigInfoType.DATASTORE;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYER;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYERGROUP;
import static org.geoserver.cloud.event.info.ConfigInfoType.STYLE;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey.Kind;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup.Absent;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup.Hit;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup.Miss;
import org.junit.jupiter.api.Test;

class EntityCacheTest {

    private static final Duration A_DAY = Duration.ofDays(1);
    private static final Duration MISS_TTL = Duration.ofSeconds(30);
    private static final long ONE_MIB = 1024 * 1024;

    private static final CacheKey LAYER_KEY = CacheKey.entity(LAYER, "layer-1");
    private static final CacheKey UNKNOWN_KEY = CacheKey.entity(LAYER, "unknown");
    private static final CacheKey NAME_KEY = CacheKey.name(LAYER, "topp:roads");
    private static final CacheKey TILE_LAYER_KEY = CacheKey.tileLayer("layer-1");
    private static final byte[] JSON = "{\"id\": \"layer-1\", \"name\": \"roads\"}".getBytes(UTF_8);
    private static final byte[] TILE_LAYER_JSON = "{\"enabled\": true, \"metaTilingX\": 4}".getBytes(UTF_8);

    private final AtomicLong nanos = new AtomicLong();
    private final EntityCache cache = newCache(ONE_MIB);

    @Test
    void putReturnsStoredWeight() {
        long stored = cache.put(LAYER_KEY, JSON, cache.epoch());
        cache.setSuspended(true);
        long whileSuspended = cache.put(UNKNOWN_KEY, JSON, cache.epoch());

        assertThat(stored).isEqualTo(JSON.length + 64);
        assertThat(whileSuspended).isZero();
    }

    @Test
    void hitAfterPut() {
        cache.put(LAYER_KEY, JSON, cache.epoch());

        assertThat(cache.lookup(LAYER_KEY)).isEqualTo(new Hit(JSON));
    }

    @Test
    void absentWhenNeverPut() {
        assertThat(cache.lookup(LAYER_KEY)).isInstanceOf(Absent.class);
    }

    @Test
    void missAfterPutMiss() {
        cache.setInitialized(true);

        cache.putMiss(UNKNOWN_KEY, cache.epoch());

        assertThat(cache.lookup(UNKNOWN_KEY)).isInstanceOf(Miss.class);
    }

    @Test
    void lookupIdAfterPutId() {
        cache.putId(NAME_KEY, "layer-1", cache.epoch());

        assertThat(cache.lookupId(NAME_KEY)).contains("layer-1");
        assertThat(cache.lookup(NAME_KEY)).isEqualTo(new Hit("layer-1".getBytes(UTF_8)));
    }

    @Test
    void noIdForNameMiss() {
        cache.setInitialized(true);

        cache.putMiss(NAME_KEY, cache.epoch());

        assertThat(cache.lookupId(NAME_KEY)).isEmpty();
        assertThat(cache.lookup(NAME_KEY)).isInstanceOf(Miss.class);
    }

    // the kind of the key decides how lookup reads the bytes back: JSON through the codec, or an id
    @Test
    void rejectsMismatchedKeyKind() {
        long epoch = cache.epoch();

        assertThatIllegalArgumentException().isThrownBy(() -> cache.put(NAME_KEY, JSON, epoch));
        assertThatIllegalArgumentException().isThrownBy(() -> cache.putFromListing(NAME_KEY, JSON, epoch));
        assertThatIllegalArgumentException().isThrownBy(() -> cache.putId(LAYER_KEY, "layer-1", epoch));
        assertThatIllegalArgumentException().isThrownBy(() -> cache.putIdFromListing(LAYER_KEY, "layer-1", epoch));
        assertThatIllegalArgumentException().isThrownBy(() -> cache.lookupId(LAYER_KEY));
        assertThatIllegalArgumentException().isThrownBy(() -> cache.markUncacheable(NAME_KEY));
        assertThatIllegalArgumentException().isThrownBy(() -> cache.putId(TILE_LAYER_KEY, "layer-1", epoch));
        assertThatIllegalArgumentException().isThrownBy(() -> cache.lookupId(TILE_LAYER_KEY));
    }

    @Test
    void tileLayerHitAfterPut() {
        try (ZstdCodec zstd = ZstdCodec.withBundledDictionary(ZstdCodec.DEFAULT_LEVEL)) {
            EntityCache compressed = newCache(ONE_MIB, zstd);

            compressed.put(TILE_LAYER_KEY, TILE_LAYER_JSON, compressed.epoch());
            cache.put(TILE_LAYER_KEY, TILE_LAYER_JSON, cache.epoch());

            assertThat(compressed.lookup(TILE_LAYER_KEY)).isEqualTo(new Hit(TILE_LAYER_JSON));
            assertThat(storedBytes(compressed, TILE_LAYER_KEY)).isNotEqualTo(TILE_LAYER_JSON);
            assertThat(cache.lookup(TILE_LAYER_KEY)).isEqualTo(new Hit(TILE_LAYER_JSON));
        }
    }

    // an empty value says that the object has no tile layer: unlike a miss, it lasts as long as the other entries
    @Test
    void emptyTileLayerIsHit() {
        try (ZstdCodec zstd = ZstdCodec.withBundledDictionary(ZstdCodec.DEFAULT_LEVEL)) {
            EntityCache compressed = newCache(ONE_MIB, zstd);
            cache.put(TILE_LAYER_KEY, new byte[0], cache.epoch());
            compressed.put(TILE_LAYER_KEY, new byte[0], compressed.epoch());

            advance(MISS_TTL.plusSeconds(1));

            assertThat(cache.lookup(TILE_LAYER_KEY)).isEqualTo(new Hit(new byte[0]));
            assertThat(cache.contains(TILE_LAYER_KEY)).isTrue();
            assertThat(compressed.lookup(TILE_LAYER_KEY)).isEqualTo(new Hit(new byte[0]));
            assertThat(compressed.contains(TILE_LAYER_KEY)).isTrue();
        }
    }

    @Test
    void rejectsHeapPercentOutOfRange() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> EntityCache.maximumWeightFor(0))
                .withMessageContaining("max-heap-percent");
        assertThatIllegalArgumentException().isThrownBy(() -> EntityCache.maximumWeightFor(101));
        assertThat(EntityCache.maximumWeightFor(1)).isPositive();
        assertThat(EntityCache.maximumWeightFor(100)).isPositive();
    }

    @Test
    void decodesThroughCodec() {
        try (ZstdCodec zstd = ZstdCodec.withBundledDictionary(ZstdCodec.DEFAULT_LEVEL)) {
            EntityCache compressed = new EntityCache("gs-catalog", ONE_MIB, MISS_TTL, zstd);

            compressed.put(LAYER_KEY, JSON, compressed.epoch());
            compressed.putId(NAME_KEY, "layer-1", compressed.epoch());

            assertThat(compressed.lookup(LAYER_KEY)).isEqualTo(new Hit(JSON));
            assertThat(compressed.lookupId(NAME_KEY)).contains("layer-1");
        }
    }

    @Test
    void putAfterEvictIsDropped() {
        long epochSeen = cache.epoch();
        cache.evict(LAYER_KEY);

        cache.put(LAYER_KEY, JSON, epochSeen);

        assertThat(cache.lookup(LAYER_KEY)).isInstanceOf(Absent.class);
    }

    // each eviction moves the epoch before and after its removal
    @Test
    void evictionsMoveEpoch() {
        long before = cache.epoch();

        cache.evict(LAYER_KEY);
        cache.evictWhere(key -> true);
        cache.clear();

        assertThat(cache.epoch()).isEqualTo(before + 6);
    }

    // a reader may hit an entry while the eviction removes it: the epoch read by it must be out of date afterwards
    @Test
    void epochMovesAfterRemoval() {
        cache.put(LAYER_KEY, JSON, cache.epoch());
        AtomicLong readDuringRemoval = new AtomicLong();

        cache.evictWhere(key -> readEpoch(readDuringRemoval));

        assertThat(cache.epoch()).isGreaterThan(readDuringRemoval.get());
    }

    @Test
    void noMissBeforeInitialized() {
        cache.setInitialized(false);

        cache.putMiss(UNKNOWN_KEY, cache.epoch());

        assertThat(cache.lookup(UNKNOWN_KEY)).isInstanceOf(Absent.class);
    }

    @Test
    void noListingWriteThroughBeforeInitialized() {
        CacheKey otherKey = CacheKey.entity(LAYER, "layer-2");

        cache.putFromListing(LAYER_KEY, JSON, cache.epoch());
        cache.setInitialized(true);
        cache.putFromListing(otherKey, JSON, cache.epoch());

        assertThat(cache.lookup(LAYER_KEY)).isInstanceOf(Absent.class);
        assertThat(cache.lookup(otherKey)).isEqualTo(new Hit(JSON));
    }

    @Test
    void noListedIdBeforeInitialized() {
        CacheKey otherName = CacheKey.name(LAYER, "topp:rivers");

        cache.putIdFromListing(NAME_KEY, "layer-1", cache.epoch());
        cache.setInitialized(true);
        cache.putIdFromListing(otherName, "layer-2", cache.epoch());

        assertThat(cache.lookup(NAME_KEY)).isInstanceOf(Absent.class);
        assertThat(cache.lookupId(otherName)).contains("layer-2");
    }

    // a listing skips the objects held already, but replaces a miss
    @Test
    void containsHeldValuesOnly() {
        cache.setInitialized(true);
        cache.put(LAYER_KEY, JSON, cache.epoch());
        cache.putMiss(UNKNOWN_KEY, cache.epoch());

        assertThat(cache.contains(LAYER_KEY)).isTrue();
        assertThat(cache.contains(UNKNOWN_KEY)).isFalse();
        assertThat(cache.contains(NAME_KEY)).isFalse();
    }

    @Test
    void containsLeavesStatsAlone() {
        cache.put(LAYER_KEY, JSON, cache.epoch());

        cache.contains(LAYER_KEY);
        cache.contains(NAME_KEY);

        assertThat(cache.caffeine().stats().requestCount()).isZero();
    }

    // a coverage store looked up as a store probes the data store key first
    @Test
    void multiKeyRecordsOneGet() {
        CacheKey dataStore = CacheKey.entity(DATASTORE, "store-1");
        CacheKey coverageStore = CacheKey.entity(COVERAGESTORE, "store-1");
        cache.put(coverageStore, JSON, cache.epoch());

        Lookup found = cache.lookup(List.of(dataStore, coverageStore));

        assertThat(found).isEqualTo(new Hit(JSON));
        assertThat(cache.caffeine().stats().requestCount()).isEqualTo(1);
        assertThat(cache.caffeine().stats().hitCount()).isEqualTo(1);
    }

    @Test
    void multiKeyMissNeedsEachKey() {
        cache.setInitialized(true);
        CacheKey dataStore = CacheKey.entity(DATASTORE, "store-1");
        CacheKey coverageStore = CacheKey.entity(COVERAGESTORE, "store-1");
        cache.putMiss(dataStore, cache.epoch());

        Lookup oneMiss = cache.lookup(List.of(dataStore, coverageStore));
        cache.putMiss(coverageStore, cache.epoch());
        Lookup twoMisses = cache.lookup(List.of(dataStore, coverageStore));

        assertThat(oneMiss).isInstanceOf(Absent.class);
        assertThat(twoMisses).isInstanceOf(Miss.class);
    }

    // another thread finds a group as a layer: it stores the group, then a miss under the layer key
    @Test
    void raceKeepsAbsent() {
        CacheKey layer = CacheKey.entity(LAYER, "group-1");
        CacheKey group = CacheKey.entity(LAYERGROUP, "group-1");
        EntityCache racing = racingCache(other -> {
            other.put(group, JSON, other.epoch());
            other.putMiss(layer, other.epoch());
        });

        Lookup found = racing.lookup(List.of(layer, group));

        assertThat(found).isInstanceOf(Absent.class);
    }

    // the data store key was absent at the probe: a miss under the coverage store key alone cannot answer Miss
    @Test
    void replacedHitAnswersAbsent() {
        CacheKey dataStore = CacheKey.entity(DATASTORE, "store-1");
        CacheKey coverageStore = CacheKey.entity(COVERAGESTORE, "store-1");
        EntityCache racing = racingCache(other -> other.putMiss(coverageStore, other.epoch()));
        racing.put(coverageStore, JSON, racing.epoch());

        Lookup found = racing.lookup(List.of(dataStore, coverageStore));

        assertThat(found).isInstanceOf(Absent.class);
    }

    @Test
    void suspendedAnswersAbsent() {
        cache.setInitialized(true);
        cache.put(LAYER_KEY, JSON, cache.epoch());
        cache.putMiss(UNKNOWN_KEY, cache.epoch());

        cache.setSuspended(true);

        assertThat(cache.lookup(LAYER_KEY)).isInstanceOf(Absent.class);
        assertThat(cache.lookup(UNKNOWN_KEY)).isInstanceOf(Absent.class);
    }

    @Test
    void suspendedIgnoresPuts() {
        cache.setInitialized(true);
        cache.setSuspended(true);

        cache.put(LAYER_KEY, JSON, cache.epoch());
        cache.putId(NAME_KEY, "layer-1", cache.epoch());
        cache.putMiss(UNKNOWN_KEY, cache.epoch());
        cache.setSuspended(false);

        assertThat(cache.lookup(LAYER_KEY)).isInstanceOf(Absent.class);
        assertThat(cache.lookup(NAME_KEY)).isInstanceOf(Absent.class);
        assertThat(cache.lookup(UNKNOWN_KEY)).isInstanceOf(Absent.class);
    }

    @Test
    void suspendedStillEvicts() {
        cache.put(LAYER_KEY, JSON, cache.epoch());
        cache.setSuspended(true);

        cache.evict(LAYER_KEY);
        cache.setSuspended(false);

        assertThat(cache.lookup(LAYER_KEY)).isInstanceOf(Absent.class);
    }

    @Test
    void missStillExpires() {
        cache.setInitialized(true);
        cache.put(LAYER_KEY, JSON, cache.epoch());
        cache.putMiss(UNKNOWN_KEY, cache.epoch());

        advance(MISS_TTL.plusSeconds(1));

        assertThat(cache.lookup(UNKNOWN_KEY)).isInstanceOf(Absent.class);
        assertThat(cache.lookup(LAYER_KEY)).isEqualTo(new Hit(JSON));
    }

    @Test
    void entryNeverExpires() {
        cache.put(LAYER_KEY, JSON, cache.epoch());

        advance(A_DAY);

        assertThat(cache.lookup(LAYER_KEY)).isEqualTo(new Hit(JSON));
    }

    // an object added after its miss was stored must not expire with the miss
    @Test
    void putOverMissStays() {
        cache.setInitialized(true);
        cache.putMiss(LAYER_KEY, cache.epoch());

        cache.put(LAYER_KEY, JSON, cache.epoch());
        advance(A_DAY);

        assertThat(cache.lookup(LAYER_KEY)).isEqualTo(new Hit(JSON));
    }

    // 1 KB of JSON weighs 1088 with the estimate for its key: 4 KB hold three entries, and would hold four without it
    @Test
    void weightBoundsEntries() {
        EntityCache small = newCache(4 * 1024);
        List<CacheKey> keys = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            CacheKey key = CacheKey.entity(LAYER, "layer-" + i);
            small.put(key, new byte[1024], small.epoch());
            keys.add(key);
        }

        small.cleanUp();

        assertThat(countHits(small, keys)).isBetween(1L, 3L);
    }

    @Test
    void evictWhereRemovesMatchingKeys() {
        CacheKey styleName = CacheKey.name(STYLE, "topp:roads");
        cache.put(LAYER_KEY, JSON, cache.epoch());
        cache.putId(NAME_KEY, "layer-1", cache.epoch());
        cache.putId(styleName, "style-1", cache.epoch());

        cache.evictWhere(key -> key.kind() == Kind.NAME && key.type() == LAYER);

        assertThat(cache.lookup(NAME_KEY)).isInstanceOf(Absent.class);
        assertThat(cache.lookup(LAYER_KEY)).isEqualTo(new Hit(JSON));
        assertThat(cache.lookupId(styleName)).contains("style-1");
    }

    // the caller warns at the first mark of an object only
    @Test
    void markReportsFirstOnly() {
        boolean before = cache.hasUncacheableRows();

        boolean first = cache.markUncacheable(LAYER_KEY);
        boolean repeat = cache.markUncacheable(LAYER_KEY);

        assertThat(before).isFalse();
        assertThat(first).isTrue();
        assertThat(repeat).isFalse();
        assertThat(cache.isUncacheable(LAYER_KEY)).isTrue();
        assertThat(cache.hasUncacheableRows()).isTrue();
    }

    // a write evicts the keys of its object: the next read splits its row again
    @Test
    void uncacheableForgottenOnEvict() {
        CacheKey byPredicate = CacheKey.entity(LAYER, "layer-2");
        CacheKey kept = CacheKey.entity(LAYER, "layer-3");
        cache.markUncacheable(LAYER_KEY);
        cache.markUncacheable(byPredicate);
        cache.markUncacheable(kept);

        cache.evict(LAYER_KEY);
        cache.evictWhere(byPredicate::equals);

        assertThat(cache.isUncacheable(LAYER_KEY)).isFalse();
        assertThat(cache.isUncacheable(byPredicate)).isFalse();
        assertThat(cache.isUncacheable(kept)).isTrue();
    }

    @Test
    void uncacheableForgottenOnClear() {
        cache.markUncacheable(LAYER_KEY);

        cache.clear();

        assertThat(cache.isUncacheable(LAYER_KEY)).isFalse();
    }

    @Test
    void uncacheableMarkStays() {
        cache.markUncacheable(LAYER_KEY);

        advance(A_DAY);
        boolean aDayLater = cache.isUncacheable(LAYER_KEY);
        cache.evict(LAYER_KEY);

        assertThat(aDayLater).isTrue();
        assertThat(cache.isUncacheable(LAYER_KEY)).isFalse();
    }

    private EntityCache newCache(long maximumWeightBytes) {
        return newCache(maximumWeightBytes, new NoopCodec());
    }

    private EntityCache newCache(long maximumWeightBytes, EntityCodec codec) {
        return new EntityCache("gs-catalog", maximumWeightBytes, MISS_TTL, codec, nanos::get);
    }

    /** The bytes kept by the cache for a key, as encoded by its codec. */
    private static byte[] storedBytes(EntityCache cache, CacheKey key) {
        return cache.caffeine().policy().getIfPresentQuietly(key);
    }

    /** A cache running the writes of another thread between the probe of a lookup under several keys and its get. */
    private EntityCache racingCache(Consumer<EntityCache> otherThread) {
        EntityCache racing = new EntityCache("gs-catalog", ONE_MIB, MISS_TTL, new NoopCodec(), nanos::get) {
            @Override
            public Lookup lookup(CacheKey key) {
                otherThread.accept(this);
                return super.lookup(key);
            }
        };
        racing.setInitialized(true);
        return racing;
    }

    private void advance(Duration duration) {
        nanos.addAndGet(duration.toNanos());
    }

    /** Keeps the epoch read by an eviction predicate, and accepts the key. */
    private boolean readEpoch(AtomicLong epoch) {
        epoch.set(cache.epoch());
        return true;
    }

    private static long countHits(EntityCache cache, List<CacheKey> keys) {
        return keys.stream().filter(key -> cache.lookup(key) instanceof Hit).count();
    }
}
