/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.geoserver.platform.resource.Resource.Type.DIRECTORY;
import static org.geoserver.platform.resource.Resource.Type.RESOURCE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.ResourceNotification.Kind;
import org.junit.jupiter.api.Test;

class ResourceStoreCacheTest {

    private static final long ONE_MIB = 1024 * 1024;
    private static final long FILE_SIZE = 100;

    private final ResourceRow root = row(0, 0, DIRECTORY, "");
    private final ResourceRow a = row(1, 0, DIRECTORY, "a");
    private final ResourceRow ab = row(2, 1, DIRECTORY, "a/b");
    private final ResourceRow abc = row(3, 2, RESOURCE, "a/b/c");
    private final ResourceRow abSibling = row(4, 1, RESOURCE, "a/bc");
    private final ResourceRow d = row(5, 0, RESOURCE, "d");

    private final List<ResourceRow> rows = new ArrayList<>(List.of(root, a, ab, abc, abSibling, d));
    private final AtomicInteger loads = new AtomicInteger();

    private final ResourceStoreCache cache = newCache(this::load);

    @Test
    void loadsOnce() {
        assertThat(cache.find("a")).contains(a);
        assertThat(cache.find("a/b/c")).contains(abc);

        assertThat(loads.get()).isEqualTo(1);
    }

    // the index holds the whole store: a path missing from it is missing from the store
    @Test
    void absentPathNoLoad() {
        cache.find("a");

        assertThat(cache.find("missing")).isEmpty();
        assertThat(loads.get()).isEqualTo(1);
    }

    @Test
    void childrenByParent() {
        ResourceRow foundRoot = cache.find("").orElseThrow();

        assertThat(foundRoot).isEqualTo(root);
        assertThat(cache.children(a)).containsExactly(ab, abSibling);
        assertThat(cache.children(foundRoot)).containsExactly(a, d);
    }

    @Test
    void descendantsByPrefix() {
        assertThat(cache.descendants("a/b")).containsExactly(abc);
        assertThat(cache.descendants("a")).containsExactly(ab, abc, abSibling);
    }

    @Test
    void rootDescendantsAreAllRows() {
        assertThat(cache.descendants("")).containsExactly(a, ab, abc, abSibling, d);
    }

    @Test
    void removeDropsSubtree() {
        cache.find("a");

        cache.remove("a/b");

        assertThat(cache.find("a/b")).isEmpty();
        assertThat(cache.find("a/b/c")).isEmpty();
        assertThat(cache.find("a/bc")).contains(abSibling);
        assertThat(cache.size()).isEqualTo(4);
    }

    @Test
    void movedRepathsSubtree() {
        cache.find("a");
        List<ResourceRow> moved = List.of(
                new ResourceRow(2, 7, DIRECTORY, "e/f", 2_000, 0),
                new ResourceRow(3, 2, RESOURCE, "e/f/c", 2_000, FILE_SIZE));

        cache.moved("a/b", "e/f", moved);

        assertThat(cache.find("a/b")).isEmpty();
        assertThat(cache.find("a/b/c")).isEmpty();
        assertThat(cache.find("e/f")).contains(moved.get(0));
        assertThat(cache.find("e/f/c")).contains(moved.get(1));
        assertThat(cache.find("a/bc")).contains(abSibling);
    }

    // the snapshot of the reload holds the move, and a directory created at the source path afterwards
    @Test
    void moveReplayKeepsNewSource() throws Exception {
        BlockingLoader loader = new BlockingLoader(2);
        ResourceStoreCache blocking = newCache(loader);
        blocking.find("a");
        List<ResourceRow> moved = List.of(
                new ResourceRow(1, 0, DIRECTORY, "b", 2_000, 0),
                new ResourceRow(2, 1, DIRECTORY, "b/b", 2_000, 0),
                new ResourceRow(3, 2, RESOURCE, "b/b/c", 2_000, FILE_SIZE),
                new ResourceRow(4, 1, RESOURCE, "b/bc", 2_000, FILE_SIZE));
        ResourceRow newSource = new ResourceRow(9, 0, DIRECTORY, "a", 3_000, 0);
        rows.clear();
        rows.add(root);
        rows.addAll(moved);
        rows.add(newSource);
        CompletableFuture<Optional<ResourceRow>> reload = reloadInBackground(blocking, loader);

        blocking.moved("a", "b", moved);
        loader.release();
        reload.get(10, TimeUnit.SECONDS);

        assertThat(blocking.find("b")).contains(moved.get(0));
        assertThat(blocking.find("b/b/c")).contains(moved.get(2));
        assertThat(blocking.find("a")).contains(newSource);
    }

    @Test
    void reloadReportsChanges() {
        List<IndexChanges> reported = new ArrayList<>();
        cache.onChangesFound(reported::add);
        cache.find("a");
        ResourceRow touched = new ResourceRow(d.id(), 0, RESOURCE, "d", 2_000, FILE_SIZE);
        ResourceRow added = row(6, 0, RESOURCE, "g");
        rows.set(rows.indexOf(d), touched);
        rows.remove(abSibling);
        rows.add(added);

        cache.clear();
        cache.find("a");

        IndexChanges expected = new IndexChanges(List.of(added), List.of(touched), List.of(abSibling));
        assertThat(reported).containsExactly(expected);
    }

    // the events lost while the event bus was away empty the cache
    @Test
    void reloadAfterClearReports() {
        List<IndexChanges> reported = new ArrayList<>();
        cache.onChangesFound(reported::add);
        cache.find("a");
        rows.remove(abSibling);

        cache.clear();
        cache.find("a");

        assertThat(reported).containsExactly(new IndexChanges(List.of(), List.of(), List.of(abSibling)));
    }

    @Test
    void firstLoadReportsNothing() {
        List<IndexChanges> reported = new ArrayList<>();
        cache.onChangesFound(reported::add);

        cache.find("a");

        assertThat(reported).isEmpty();
    }

    @Test
    void reloadEvictsChangedContent() {
        CountingReader reader = new CountingReader("content of c");
        cache.find("a");
        cache.content(abc.id(), reader);
        rows.set(rows.indexOf(abc), new ResourceRow(abc.id(), ab.id(), RESOURCE, "a/b/c", 2_000, FILE_SIZE));

        cache.clear();
        cache.find("a");
        cache.content(abc.id(), reader);

        assertThat(reader.reads.get()).isEqualTo(2);
    }

    @Test
    void clearForcesReload() {
        cache.find("a");

        cache.clear();

        assertThat(cache.size()).isZero();
        cache.find("a");
        assertThat(loads.get()).isEqualTo(2);
    }

    @Test
    void putKeepsNewerMtime() {
        cache.find("a");
        ResourceRow newer = new ResourceRow(1, 0, DIRECTORY, "a", 2_000, 0);
        ResourceRow older = new ResourceRow(1, 0, DIRECTORY, "a", 1_500, 0);
        ResourceRow sameTime = new ResourceRow(7, 0, DIRECTORY, "a", 2_000, 0);

        cache.put(newer);
        cache.put(older);
        assertThat(cache.find("a")).contains(newer);

        cache.put(sameTime);
        assertThat(cache.find("a")).contains(sameTime);
    }

    @Test
    void contentCachedById() {
        CountingReader reader = new CountingReader("content of c");

        byte[] first = cache.content(abc.id(), reader);
        byte[] second = cache.content(abc.id(), reader);

        assertThat(second).isEqualTo(first).isEqualTo("content of c".getBytes(UTF_8));
        assertThat(reader.reads.get()).isEqualTo(1);
    }

    @Test
    void removeEvictsContent() {
        CountingReader reader = new CountingReader("content of c");
        ResourceRow found = cache.find("a/b/c").orElseThrow();
        cache.content(found.id(), reader);

        cache.remove("a/b");
        cache.content(abc.id(), reader);

        assertThat(reader.reads.get()).isEqualTo(2);
    }

    @Test
    void writeDuringReloadKept() throws Exception {
        BlockingLoader loader = new BlockingLoader(2);
        ResourceStoreCache blocking = newCache(loader);
        blocking.find("a");
        CompletableFuture<Optional<ResourceRow>> reload = reloadInBackground(blocking, loader);

        ResourceRow written = row(6, 1, RESOURCE, "a/w");
        blocking.put(written);
        loader.release();
        reload.get(10, TimeUnit.SECONDS);

        assertThat(blocking.find("a/w")).contains(written);
        assertThat(loads.get()).isEqualTo(2);
    }

    // a listener of the dispatcher reads the store while the dispatcher holds its lock, as upstream's FileWatcher does
    @Test
    void reportOutsideLoadLock() throws Exception {
        BlockingLoader loader = new BlockingLoader(2);
        ResourceStoreCache blocking = newCache(loader);
        SynchronizedResourceNotificationDispatcher dispatcher = new SynchronizedResourceNotificationDispatcher();
        dispatcher.addListener("d", notification -> blocking.find("d"));
        blocking.onChangesFound(changes -> dispatcher.changed(notificationOf("d")));
        blocking.find("a");
        rows.remove(abSibling);
        blocking.clear();

        CompletableFuture<Optional<ResourceRow>> load = CompletableFuture.supplyAsync(() -> blocking.find("a"));
        loader.awaitLoading();
        Thread notifier = new Thread(() -> dispatcher.changed(notificationOf("d")));
        notifier.setDaemon(true);
        notifier.start();
        awaitBlocked(notifier);
        loader.release();

        assertThat(load.get(5, TimeUnit.SECONDS)).contains(a);
        notifier.join(5_000);
        assertThat(notifier.isAlive()).isFalse();
    }

    // the load answers the read that started it
    @Test
    void clearDuringLoadDiscardsIt() throws Exception {
        BlockingLoader loader = new BlockingLoader(1);
        ResourceStoreCache blocking = newCache(loader);
        CompletableFuture<Optional<ResourceRow>> load = CompletableFuture.supplyAsync(() -> blocking.find("a"));
        loader.awaitLoading();

        blocking.clear();
        loader.release();

        assertThat(load.get(10, TimeUnit.SECONDS)).contains(a);
        assertThat(blocking.size()).isZero();
        blocking.find("a");
        assertThat(loads.get()).isEqualTo(2);
    }

    // the retry runs on another thread: the lock is reentrant
    @Test
    void loaderFailureFreesLock() throws Exception {
        AtomicBoolean failing = new AtomicBoolean(true);
        ResourceStoreCache failingOnce = newCache(failingLoader(failing));

        assertThatThrownBy(() -> failingOnce.find("a")).isInstanceOf(IllegalStateException.class);
        failing.set(false);
        CompletableFuture<Optional<ResourceRow>> retry = CompletableFuture.supplyAsync(() -> failingOnce.find("a"));

        assertThat(retry.get(10, TimeUnit.SECONDS)).contains(a);
        assertThat(loads.get()).isEqualTo(2);
    }

    private Supplier<List<ResourceRow>> failingLoader(AtomicBoolean failing) {
        return () -> {
            loads.incrementAndGet();
            if (failing.get()) {
                throw new IllegalStateException("the database is not available");
            }
            return List.copyOf(rows);
        };
    }

    private CompletableFuture<Optional<ResourceRow>> reloadInBackground(
            ResourceStoreCache blocking, BlockingLoader loader) throws InterruptedException {
        blocking.clear();
        CompletableFuture<Optional<ResourceRow>> reload = CompletableFuture.supplyAsync(() -> blocking.find("a"));
        loader.awaitLoading();
        return reload;
    }

    private ResourceStoreCache newCache(Supplier<List<ResourceRow>> loader) {
        return new ResourceStoreCache("gs-resources", loader, ONE_MIB);
    }

    private List<ResourceRow> load() {
        loads.incrementAndGet();
        return List.copyOf(rows);
    }

    private static ResourceRow row(long id, long parentId, Type type, String path) {
        long size = type == RESOURCE ? FILE_SIZE : 0;
        return new ResourceRow(id, parentId, type, path, 1_000, size);
    }

    private static ResourceNotification notificationOf(String path) {
        List<ResourceNotification.Event> events = List.of(new ResourceNotification.Event(path, Kind.ENTRY_MODIFY));
        return new ResourceNotification(path, Kind.ENTRY_MODIFY, 2_000, events);
    }

    /** Waits until the thread waits for a lock: the listener read the store, and waits for the load. */
    private static void awaitBlocked(Thread thread) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (thread.getState() != Thread.State.WAITING && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(thread.getState()).isEqualTo(Thread.State.WAITING);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** Returns the rows of the test, and blocks from a given load until released. */
    private class BlockingLoader implements Supplier<List<ResourceRow>> {

        private final int firstBlockingLoad;
        private final CountDownLatch loading = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        BlockingLoader(int firstBlockingLoad) {
            this.firstBlockingLoad = firstBlockingLoad;
        }

        @Override
        public List<ResourceRow> get() {
            if (loads.incrementAndGet() >= firstBlockingLoad) {
                loading.countDown();
                await(release);
            }
            return List.copyOf(rows);
        }

        void awaitLoading() throws InterruptedException {
            assertThat(loading.await(10, TimeUnit.SECONDS)).isTrue();
        }

        void release() {
            release.countDown();
        }
    }

    private static class CountingReader implements Supplier<byte[]> {

        private final AtomicInteger reads = new AtomicInteger();
        private final byte[] content;

        CountingReader(String content) {
            this.content = content.getBytes(UTF_8);
        }

        @Override
        public byte[] get() {
            reads.incrementAndGet();
            return content;
        }
    }
}
