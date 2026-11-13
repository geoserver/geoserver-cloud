/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.requestcache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class RequestCacheScopeTest {

    private static final String SCOPE_LOGGER = "org.geoserver.catalog.plugin.requestcache";

    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private final List<RequestCache> closed = new CopyOnWriteArrayList<>();

    private Runnable removeListener = () -> {};

    @AfterEach
    void closeScope() {
        RequestCacheScope.close();
        removeListener.run();
        worker.shutdownNow();
    }

    // the nested call closes nothing: the owner of the scope closes it
    @Test
    void openOnceOnly() {
        RequestCacheScope.open(10);
        RequestCache opened = RequestCacheScope.current();

        Runnable nestedCloser = RequestCacheScope.open(10);
        nestedCloser.run();

        assertThat(RequestCacheScope.current()).isSameAs(opened);
        assertThat(opened.isDead()).isFalse();
    }

    @Test
    void openReplacesDeadCache() {
        RequestCache dead = bindDeadCache();

        RequestCacheScope.open(10);

        RequestCache current = RequestCacheScope.current();
        assertThat(current).isNotSameAs(dead);
        assertThat(current.isDead()).isFalse();
    }

    // a thread-local cleanup on the thread of the request removed the cache before the request ended
    @Test
    void openCloserKillsItsCache() {
        Runnable closer = RequestCacheScope.open(10);
        RequestCache opened = RequestCacheScope.current();
        RequestCache other = new RequestCache(10);
        RequestCacheScope.apply(other);

        closer.run();

        assertThat(opened.isDead()).isTrue();
        assertThat(other.isDead()).isFalse();
        assertThat(RequestCacheScope.current()).isSameAs(other);
    }

    @Test
    void closeRemovesCache() {
        RequestCacheScope.open(10);
        RequestCache cache = RequestCacheScope.current();

        RequestCacheScope.close();

        assertThat(RequestCacheScope.current()).isNull();
        assertThat(cache.isDead()).isTrue();
    }

    @Test
    void openIfAbsentReusesOpen() {
        RequestCacheScope.open(10);
        RequestCache outer = RequestCacheScope.current();

        Runnable closer = RequestCacheScope.openIfAbsent(10);
        assertThat(RequestCacheScope.current()).isSameAs(outer);

        closer.run();
        assertThat(RequestCacheScope.current()).isSameAs(outer);
        assertThat(outer.isDead()).isFalse();
    }

    @Test
    void openIfAbsentOpensAndCloses() {
        Runnable closer = RequestCacheScope.openIfAbsent(10);
        RequestCache opened = RequestCacheScope.current();
        assertThat(opened).isNotNull();

        closer.run();

        assertThat(RequestCacheScope.current()).isNull();
        assertThat(opened.isDead()).isTrue();
    }

    @Test
    void openIfAbsentReplacesDead() {
        RequestCache dead = bindDeadCache();

        Runnable closer = RequestCacheScope.openIfAbsent(10);
        RequestCache opened = RequestCacheScope.current();
        assertThat(opened).isNotSameAs(dead);
        assertThat(opened.isDead()).isFalse();

        closer.run();

        assertThat(opened.isDead()).isTrue();
        assertThat(RequestCacheScope.current()).isNull();
    }

    // the closer may run after the thread received another cache: it must leave that cache alone
    @Test
    void closerKillsWhatItOpened() {
        Runnable closer = RequestCacheScope.openIfAbsent(10);
        RequestCache opened = RequestCacheScope.current();
        RequestCache other = new RequestCache(10);
        RequestCacheScope.apply(other);

        closer.run();

        assertThat(opened.isDead()).isTrue();
        assertThat(other.isDead()).isFalse();
        assertThat(RequestCacheScope.current()).isSameAs(other);
    }

    // objects kept beyond the request must not come from its cache
    @Test
    void withoutCacheRestoresCache() {
        RequestCacheScope.open(10);
        RequestCache cache = RequestCacheScope.current();

        RequestCache duringAction = RequestCacheScope.withoutCache(RequestCacheScope::current);

        assertThat(duringAction).isNull();
        assertThat(RequestCacheScope.current()).isSameAs(cache);
        assertThat(cache.isDead()).isFalse();
    }

    @Test
    void withoutCacheRestoresOnError() {
        RequestCacheScope.open(10);
        RequestCache cache = RequestCacheScope.current();

        assertThatIllegalStateException()
                .isThrownBy(() -> RequestCacheScope.withoutCache(RequestCacheScopeTest::failInAction));

        assertThat(RequestCacheScope.current()).isSameAs(cache);
    }

    @Test
    void putPastCapIsRejected() {
        RequestCache cache = new RequestCache(2);
        LayerInfo roads = layer("roads");
        LayerInfo rivers = layer("rivers");
        LayerInfo lakes = layer("lakes");
        IdKey roadsId = idOf(roads);
        IdKey riversId = idOf(rivers);
        IdKey lakesId = idOf(lakes);

        cache.put(roadsId, roads);
        cache.put(riversId, rivers);
        cache.put(lakesId, lakes);

        assertThat(cache.found(roadsId)).containsSame(roads);
        assertThat(cache.found(riversId)).containsSame(rivers);
        assertThat(cache.found(lakesId)).isEmpty();
        assertThat(cache.rejected()).isEqualTo(1);
    }

    @Test
    void missIsRemembered() {
        RequestCache cache = new RequestCache(10);
        NameKey missing = new NameKey(ClassMappings.LAYER, "topp:missing");

        cache.put(missing, null);

        assertThat(cache.found(missing)).containsSame(RequestCache.MISS);
        LayerInfo layer = cache.get(missing);
        assertThat(layer).isNull();
    }

    // a read answered with a cached miss is a hit: the cache spared the lookup below
    @Test
    void countsHitsAndMisses() {
        RequestCache cache = new RequestCache(10);
        LayerInfo roads = layer("roads");
        IdKey roadsId = idOf(roads);
        IdKey riversId = new IdKey(ClassMappings.LAYER, "rivers");
        NameKey missing = new NameKey(ClassMappings.LAYER, "topp:missing");
        cache.put(roadsId, roads);
        cache.put(missing, null);

        cache.found(roadsId);
        cache.get(missing);
        cache.found(riversId);

        assertThat(cache.hits()).isEqualTo(2);
        assertThat(cache.misses()).isEqualTo(1);
    }

    @Test
    void evictForgetsKeys() {
        RequestCache cache = new RequestCache(10);
        LayerInfo roads = layer("roads");
        IdKey roadsId = idOf(roads);
        NameKey roadsName = new NameKey(ClassMappings.LAYER, "topp:roads");
        cache.put(roadsId, roads);
        cache.put(roadsName, roads);

        cache.evict(roadsId, roadsName);

        assertThat(cache.found(roadsId)).isEmpty();
        assertThat(cache.found(roadsName)).isEmpty();
    }

    @Test
    void evictIfForgetsMatches() {
        RequestCache cache = new RequestCache(10);
        LayerInfo roads = layer("roads");
        IdKey roadsId = idOf(roads);
        NameKey roadsName = new NameKey(ClassMappings.LAYER, "topp:roads");
        cache.put(roadsId, roads);
        cache.put(roadsName, roads);

        cache.evictIf(NameKey.class::isInstance);

        assertThat(cache.found(roadsId)).containsSame(roads);
        assertThat(cache.found(roadsName)).isEmpty();
    }

    // unlike kill, a clear leaves the cache alive
    @Test
    void clearForgetsEntries() {
        RequestCache cache = new RequestCache(10);
        LayerInfo roads = layer("roads");
        IdKey roadsId = idOf(roads);
        LookupKey defaultWorkspace = new LookupKey("default-workspace");
        cache.put(roadsId, roads);
        cache.put(defaultWorkspace, null);

        cache.clear();

        assertThat(cache.found(roadsId)).isEmpty();
        assertThat(cache.found(defaultWorkspace)).isEmpty();
        assertThat(cache.isDead()).isFalse();
    }

    @Test
    void deadCacheIgnoresPuts() {
        RequestCache cache = new RequestCache(10);
        LayerInfo roads = layer("roads");
        IdKey roadsId = idOf(roads);
        cache.kill();

        cache.put(roadsId, roads);

        assertThat(cache.found(roadsId)).isEmpty();
        assertThat(cache.rejected()).isEqualTo(1);
    }

    // each cache has room for the fourth put, the budget shared by both caches has none
    @Test
    void budgetSharedByCaches() {
        RequestCacheBudget budget = new RequestCacheBudget(3);
        RequestCache first = new RequestCache(10, budget);
        RequestCache second = new RequestCache(10, budget);
        LayerInfo lakes = layer("lakes");
        IdKey lakesId = idOf(lakes);
        IdKey coastId = new IdKey(ClassMappings.LAYER, "coast");

        first.put(new IdKey(ClassMappings.LAYER, "roads"), layer("roads"));
        first.put(new IdKey(ClassMappings.LAYER, "rivers"), layer("rivers"));
        second.put(lakesId, lakes);
        second.put(coastId, layer("coast"));

        assertThat(second.found(lakesId)).containsSame(lakes);
        assertThat(second.found(coastId)).isEmpty();
        assertThat(second.rejected()).isEqualTo(1);
        assertThat(budget.used()).isEqualTo(3);
    }

    @Test
    void killFreesBudgetForOthers() {
        RequestCacheBudget budget = new RequestCacheBudget(3);
        RequestCache first = new RequestCache(10, budget);
        RequestCache second = new RequestCache(10, budget);
        first.put(new IdKey(ClassMappings.LAYER, "roads"), layer("roads"));
        first.put(new IdKey(ClassMappings.LAYER, "rivers"), layer("rivers"));
        second.put(new IdKey(ClassMappings.LAYER, "lakes"), layer("lakes"));
        LayerInfo coast = layer("coast");
        IdKey coastId = idOf(coast);

        first.kill();
        second.put(coastId, coast);

        assertThat(second.found(coastId)).containsSame(coast);
        assertThat(second.rejected()).isZero();
        assertThat(budget.used()).isEqualTo(2);
    }

    @Test
    void exhaustedBudgetTakesNoEntries() {
        RequestCacheBudget budget = new RequestCacheBudget(1);
        RequestCache first = new RequestCache(10, budget);
        RequestCache second = new RequestCache(10, budget);
        first.put(new IdKey(ClassMappings.LAYER, "roads"), layer("roads"));

        boolean whileExhausted = second.takesEntries();
        first.kill();

        assertThat(whileExhausted).isFalse();
        assertThat(second.takesEntries()).isTrue();
    }

    @Test
    void evictionsFreeBudget() {
        RequestCacheBudget budget = new RequestCacheBudget(10);
        RequestCache cache = new RequestCache(10, budget);
        LayerInfo roads = layer("roads");
        IdKey roadsId = idOf(roads);
        cache.put(roadsId, roads);
        cache.put(new NameKey(ClassMappings.LAYER, "topp:roads"), roads);
        cache.put(new IdKey(ClassMappings.LAYER, "rivers"), layer("rivers"));
        cache.put(new LookupKey("default-workspace"), null);

        cache.evict(roadsId);
        int afterEvict = budget.used();
        cache.evictIf(NameKey.class::isInstance);
        int afterEvictIf = budget.used();
        cache.clear();

        assertThat(afterEvict).isEqualTo(3);
        assertThat(afterEvictIf).isEqualTo(2);
        assertThat(budget.used()).isZero();
    }

    // the next lookup after a change made through another instance reads the facade below
    @Test
    void remoteChangeClearsCache() {
        RequestCacheBudget budget = new RequestCacheBudget(10);
        RemoteChangeCounter remoteChanges = new RemoteChangeCounter();
        RequestCache cache = new RequestCache(10, budget, remoteChanges);
        LayerInfo roads = layer("roads");
        IdKey roadsId = idOf(roads);
        IdKey riversId = new IdKey(ClassMappings.LAYER, "rivers");
        cache.put(roadsId, roads);
        cache.put(riversId, layer("rivers"));

        remoteChanges.increment();
        Optional<Object> afterChange = cache.found(roadsId);
        int usedAfterChange = budget.used();
        cache.put(roadsId, roads);

        assertThat(afterChange).isEmpty();
        assertThat(usedAfterChange).isZero();
        assertThat(cache.found(roadsId)).containsSame(roads);
        assertThat(cache.found(riversId)).isEmpty();
        assertThat(budget.used()).isOne();
    }

    // the object put may have been read before the change
    @Test
    void putAfterRemoteChangeIgnored() {
        RequestCacheBudget budget = new RequestCacheBudget(10);
        RemoteChangeCounter remoteChanges = new RemoteChangeCounter();
        RequestCache cache = new RequestCache(10, budget, remoteChanges);
        LayerInfo roads = layer("roads");
        IdKey roadsId = idOf(roads);

        Optional<Object> beforeChange = cache.found(roadsId);
        remoteChanges.increment();
        cache.put(roadsId, roads);

        assertThat(beforeChange).isEmpty();
        assertThat(cache.found(roadsId)).isEmpty();
        assertThat(budget.used()).isZero();
    }

    // the key holds one entry, before and after the second put
    @Test
    void replacingPutTakesNoBudget() {
        RequestCacheBudget budget = new RequestCacheBudget(10);
        RequestCache cache = new RequestCache(10, budget);
        LayerInfo roads = layer("roads");
        IdKey roadsId = idOf(roads);

        cache.put(roadsId, null);
        cache.put(roadsId, roads);

        assertThat(cache.found(roadsId)).containsSame(roads);
        assertThat(cache.rejected()).isZero();
        assertThat(budget.used()).isOne();
    }

    // a listing left its stream open: its cache died at its maximum age, and kept its entries
    @Test
    void openClosesExpiredCache() throws InterruptedException {
        RequestCacheBudget budget = new RequestCacheBudget(10);
        RequestCache expiring = new RequestCache(10, Duration.ofMillis(200), budget);
        expiring.put(new IdKey(ClassMappings.LAYER, "roads"), layer("roads"));
        int usedBeforeExpiry = budget.used();
        RequestCacheScope.apply(expiring);
        awaitExpiry(expiring);

        RequestCacheScope.open(10);

        assertThat(usedBeforeExpiry).isOne();
        assertThat(budget.used()).isZero();
    }

    @Test
    void deadScopeFallsThrough() throws Exception {
        RequestCacheScope.open(10);
        RequestCache cache = RequestCacheScope.current();
        LayerInfo roads = layer("roads");
        IdKey roadsId = idOf(roads);
        cache.put(roadsId, roads);
        Optional<Object> beforeClose = onWorker(() -> applyThenFind(cache, roadsId));

        RequestCacheScope.close();

        Optional<Object> afterClose = onWorker(() -> findInCurrent(roadsId));
        assertThat(beforeClose).containsSame(roads);
        assertThat(afterClose).isEmpty();
    }

    @Test
    void applyOnWorkerThenCleanup() throws Exception {
        RequestCacheScope.open(10);
        RequestCache cache = RequestCacheScope.current();

        RequestCache applied = onWorker(() -> applyThenCurrent(cache));
        RequestCache afterCleanup = onWorker(RequestCacheScopeTest::cleanupThenCurrent);

        assertThat(applied).isSameAs(cache);
        assertThat(afterCleanup).isNull();
        assertThat(cache.isDead()).isFalse();
        assertThat(RequestCacheScope.current()).isSameAs(cache);
    }

    @Test
    void closeNotifiesListener() {
        listenToClosedCaches();
        RequestCacheScope.open(10);
        RequestCache cache = RequestCacheScope.current();
        cache.found(new LookupKey("default-workspace"));

        RequestCacheScope.close();

        assertThat(closed).containsExactly(cache);
        // the counters survive the end of the cache
        assertThat(cache.misses()).isEqualTo(1);
    }

    // the thread of the scope may close it, and the closer of openIfAbsent run later
    @Test
    void closeNotifiesOncePerCache() {
        listenToClosedCaches();
        Runnable closer = RequestCacheScope.openIfAbsent(10);
        RequestCache cache = RequestCacheScope.current();

        RequestCacheScope.close();
        closer.run();

        assertThat(closed).containsExactly(cache);
    }

    @Test
    void reusedScopeNotifiesNothing() {
        RequestCacheScope.open(10);
        listenToClosedCaches();

        Runnable closer = RequestCacheScope.openIfAbsent(10);
        closer.run();

        assertThat(closed).isEmpty();
    }

    @Test
    void removedListenerNotCalled() {
        Runnable removal = RequestCacheScope.onClose(closed::add);
        removal.run();
        RequestCacheScope.open(10);

        RequestCacheScope.close();

        assertThat(closed).isEmpty();
    }

    @Test
    void killEndsCacheOnce() {
        RequestCache cache = new RequestCache(10);

        boolean first = cache.kill();
        boolean second = cache.kill();

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    // a listing stream left open would otherwise leave its cache alive on a pooled thread
    @Test
    void openIfAbsentCacheExpires() {
        RequestCacheScope.openIfAbsent(10);
        RequestCache opened = RequestCacheScope.current();
        long afterMaxAge = System.nanoTime() + RequestCacheScope.OPERATION_MAX_AGE.toNanos();

        assertThat(opened.hasExpired(afterMaxAge)).isTrue();
    }

    @Test
    void openCacheNeverExpires() {
        RequestCacheScope.open(10);
        RequestCache opened = RequestCacheScope.current();
        long inOneYear = System.nanoTime() + Duration.ofDays(365).toNanos();

        assertThat(opened.hasExpired(inOneYear)).isFalse();
    }

    // the scope of an expired cache still reports when it closes
    @Test
    void expiredCacheIsDead() {
        RequestCache expired = new RequestCache(10, Duration.ZERO);
        IdKey roadsId = new IdKey(ClassMappings.LAYER, "roads");

        expired.put(roadsId, layer("roads"));

        assertThat(expired.isDead()).isTrue();
        assertThat(expired.found(roadsId)).isEmpty();
        assertThat(expired.rejected()).isEqualTo(1);
        assertThat(expired.kill()).isTrue();
    }

    @Test
    void openReplacesExpiredCache() {
        RequestCache expired = new RequestCache(10, Duration.ZERO);
        RequestCacheScope.apply(expired);

        RequestCacheScope.open(10);

        assertThat(RequestCacheScope.current()).isNotSameAs(expired);
        assertThat(RequestCacheScope.current().isDead()).isFalse();
    }

    // a listing stream left open outside a request leaves its cache bound, and the next request on the thread
    // must not read the objects of that listing
    @Test
    void openReplacesOperationCache() {
        Runnable closer = RequestCacheScope.openIfAbsent(10);
        RequestCache operation = RequestCacheScope.current();

        RequestCacheScope.open(10);
        RequestCache request = RequestCacheScope.current();
        closer.run();

        assertThat(request).isNotSameAs(operation);
        assertThat(operation.isDead()).isTrue();
        assertThat(request.isDead()).isFalse();
        assertThat(RequestCacheScope.current()).isSameAs(request);
    }

    // the closer of the operation may never run: the replaced cache must return its shares of the budget at once
    @Test
    void replacedOperationIsKilled() {
        RequestCacheScope.openIfAbsent(10);
        RequestCache operation = RequestCacheScope.current();
        operation.put(new LookupKey("default-workspace"), null);
        int usedBefore = RequestCacheBudget.shared().used();

        RequestCacheScope.open(10);

        assertThat(operation.isDead()).isTrue();
        assertThat(RequestCacheBudget.shared().used()).isEqualTo(usedBefore - 1);
    }

    // a cache left bound past its maximum age may never be closed
    @Test
    void expiredCacheDropsEntries() throws InterruptedException {
        RequestCacheBudget budget = new RequestCacheBudget(10);
        RequestCache expiring = new RequestCache(10, Duration.ofMillis(100), budget);
        LookupKey key = new LookupKey("default-workspace");
        expiring.put(key, null);

        Thread.sleep(150);
        Optional<Object> found = expiring.found(key);

        assertThat(found).isEmpty();
        assertThat(budget.used()).isZero();
    }

    // the filter closes the scope in a finally block: a listener exception would replace the outcome of the request
    @Test
    void failingListenerSkipsNoOther() {
        Runnable removeFailing = RequestCacheScope.onClose(RequestCacheScopeTest::failToListen);
        listenToClosedCaches();
        RequestCacheScope.open(10);
        RequestCache cache = RequestCacheScope.current();

        List<String> warnings;
        try (LogCapture log = LogCapture.of(SCOPE_LOGGER)) {
            RequestCacheScope.close();
            warnings = log.warnings();
        } finally {
            removeFailing.run();
        }

        assertThat(closed).containsExactly(cache);
        assertThat(warnings).singleElement().asString().contains("listener failed");
    }

    @Test
    void openIfAbsentReplacesExpired() {
        RequestCache expired = new RequestCache(10, Duration.ZERO);
        RequestCacheScope.apply(expired);

        Runnable closer = RequestCacheScope.openIfAbsent(10);
        RequestCache opened = RequestCacheScope.current();
        closer.run();

        assertThat(opened).isNotSameAs(expired);
        assertThat(opened.isDead()).isTrue();
        assertThat(RequestCacheScope.current()).isNull();
    }

    @Test
    void keysMatchByValue() {
        RequestCache cache = new RequestCache(10);
        LayerInfo roads = layer("roads");
        WorkspaceInfo topp = workspace("topp");
        cache.put(new IdKey(ClassMappings.LAYER, "roads"), roads);
        cache.put(new NameKey(ClassMappings.LAYER, "topp:roads"), roads);
        cache.put(new LookupKey("default-workspace"), topp);

        assertThat(cache.found(new IdKey(ClassMappings.LAYER, "roads"))).containsSame(roads);
        assertThat(cache.found(new NameKey(ClassMappings.LAYER, "topp:roads"))).containsSame(roads);
        assertThat(cache.found(new LookupKey("default-workspace"))).containsSame(topp);
        assertThat(cache.found(new IdKey(ClassMappings.LAYERGROUP, "roads"))).isEmpty();
        assertThat(cache.found(new NameKey(ClassMappings.LAYER, "roads"))).isEmpty();
    }

    private void listenToClosedCaches() {
        removeListener = RequestCacheScope.onClose(closed::add);
    }

    private static void failToListen(RequestCache cache) {
        throw new IllegalStateException("listener failed");
    }

    private static Object failInAction() {
        throw new IllegalStateException("action failed");
    }

    /** Keeps the events of a logger in memory during a test, and keeps them off the console. */
    private static final class LogCapture implements AutoCloseable {

        private final Logger logger;
        private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

        private LogCapture(Logger logger) {
            this.logger = logger;
        }

        static LogCapture of(String loggerName) {
            LogCapture capture = new LogCapture((Logger) LoggerFactory.getLogger(loggerName));
            capture.appender.start();
            capture.logger.addAppender(capture.appender);
            capture.logger.setAdditive(false);
            return capture;
        }

        /** The warnings logged, each as its message followed by the message of its exception. */
        List<String> warnings() {
            return appender.list.stream()
                    .filter(event -> event.getLevel() == Level.WARN)
                    .map(LogCapture::messageWithCause)
                    .toList();
        }

        private static String messageWithCause(ILoggingEvent event) {
            IThrowableProxy cause = event.getThrowableProxy();
            if (cause == null) {
                return event.getFormattedMessage();
            }
            return event.getFormattedMessage() + ": " + cause.getMessage();
        }

        @Override
        public void close() {
            logger.setAdditive(true);
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private <T> T onWorker(Callable<T> task) throws Exception {
        return worker.submit(task).get();
    }

    private static Optional<Object> applyThenFind(RequestCache cache, Object key) {
        RequestCacheScope.apply(cache);
        return findInCurrent(key);
    }

    private static Optional<Object> findInCurrent(Object key) {
        RequestCache current = RequestCacheScope.current();
        return current.found(key);
    }

    private static @Nullable RequestCache applyThenCurrent(RequestCache cache) {
        RequestCacheScope.apply(cache);
        return RequestCacheScope.current();
    }

    private static @Nullable RequestCache cleanupThenCurrent() {
        RequestCacheScope.cleanup();
        return RequestCacheScope.current();
    }

    // a dead cache stays bound on a worker after its request ended, until the worker's cleanup
    private static RequestCache bindDeadCache() {
        RequestCache dead = new RequestCache(10);
        RequestCacheScope.apply(dead);
        dead.kill();
        return dead;
    }

    private static void awaitExpiry(RequestCache cache) throws InterruptedException {
        while (!cache.isDead()) {
            Thread.sleep(10);
        }
    }

    private static IdKey idOf(LayerInfo layer) {
        return new IdKey(ClassMappings.LAYER, layer.getId());
    }

    private static LayerInfo layer(String id) {
        LayerInfoImpl layer = new LayerInfoImpl();
        layer.setId(id);
        return layer;
    }

    private static WorkspaceInfo workspace(String name) {
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId(name);
        workspace.setName(name);
        return workspace;
    }
}
