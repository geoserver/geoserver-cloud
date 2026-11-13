/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.requestcache;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

/**
 * Binds a {@link RequestCache} to the threads of a request, with a static thread-local.
 *
 * <p>A servlet filter opens the scope when a request starts and closes it when the request ends. A forward or an
 * include runs within the scope of its request. If the filter also runs for them, {@link #open(int)} returns a closer
 * doing nothing to the nested call, and the filter leaves the scope to its owner. Worker threads of the request, such
 * as the WMS rendering threads, receive the cache through a thread-local transfer: {@link #apply(RequestCache)} binds
 * it to the worker, and {@link #cleanup()} removes it when the worker's task ends.
 *
 * <p>Closing the scope kills the cache. A worker still holding it then finds nothing in it and reads through to the
 * facade below: it decodes objects again, but never reads stale ones. The listeners added with
 * {@link #onClose(Consumer)} receive the cache when its scope closes, once per cache.
 *
 * <p>Work outside a request, such as a catalog listing, opens a scope of its own with {@link #openIfAbsent(int)} and
 * closes it when it ends. The cache of such a scope also dies by itself {@link #OPERATION_MAX_AGE} after it opened.
 *
 * <p>The caches of the scopes share the budget of entries of the JVM, see {@link RequestCacheBudget#shared()}.
 */
@Slf4j(topic = "org.geoserver.catalog.plugin.requestcache")
public final class RequestCacheScope {

    /** The size of the cache of a scope opened by code without a configured size. */
    public static final int DEFAULT_MAX_ENTRIES = 8_000;

    /**
     * The maximum age of the cache of work outside a request. A listing closes its cache with its stream. A stream left
     * open would leave a live cache on a pooled thread, such as a thread of the event bus, and later work on that
     * thread would read the objects of the listing without seeing their changes. Work running longer reads through to
     * the facade below once its cache dies. The cache of a request has no maximum age: the filter closes it when the
     * request ends, and a long request keeps it.
     */
    static final Duration OPERATION_MAX_AGE = Duration.ofMinutes(5);

    private static final ThreadLocal<RequestCache> CURRENT = new ThreadLocal<>();

    private static final List<Consumer<RequestCache>> CLOSE_LISTENERS = new CopyOnWriteArrayList<>();

    private static final Runnable NOTHING_TO_CLOSE = () -> {};

    private RequestCacheScope() {
        // static methods only
    }

    /**
     * Opens a scope on this thread, with a new cache of at most {@code maxEntries} entries, and returns the action
     * closing the scope opened by this call. The action kills the opened cache, even if another cache was bound to the
     * thread in the meantime. It does nothing if this thread holds the live cache of a request already, from an open
     * scope or from a thread-local transfer: that cache stays, and its owner closes it.
     *
     * <p>A dead cache is replaced. Its scope is closed, but the cache may still be bound: on a worker running after the
     * end of its request, or after its closer ran on another thread.
     *
     * <p>The cache of work outside a request is replaced too, see {@link #openIfAbsent(int)}. It is bound only if that
     * work left its scope open, and the request must not read the objects of that work. The replaced cache is killed:
     * the closer of that work may never run, and the entries would keep their share of the budget. The closer leaves
     * the cache of the request bound.
     */
    public static Runnable open(int maxEntries) {
        if (holdsRequestCache()) {
            return NOTHING_TO_CLOSE;
        }
        closeBoundCache();
        RequestCache opened = new RequestCache(maxEntries);
        CURRENT.set(opened);
        return () -> close(opened);
    }

    /** Whether this thread holds the live cache of a request: a cache without a maximum age. */
    private static boolean holdsRequestCache() {
        RequestCache bound = CURRENT.get();
        return bound != null && !bound.isDead() && !bound.hasMaximumAge();
    }

    private static boolean holdsLiveCache() {
        RequestCache bound = CURRENT.get();
        return bound != null && !bound.isDead();
    }

    /** Closes the cache of this thread before the cache of a request replaces it: a dead cache, or an operation's. */
    private static void closeBoundCache() {
        RequestCache bound = CURRENT.get();
        if (bound != null) {
            close(bound);
        }
    }

    /**
     * Closes the dead cache of this thread before a new cache replaces it. The listeners of the scope receive a cache
     * dead at its maximum age: its closer may never run.
     */
    private static void closeDeadCache() {
        RequestCache bound = CURRENT.get();
        if (bound != null && bound.isDead()) {
            close(bound);
        }
    }

    /**
     * Kills the cache of this thread and removes it. Call it only where the scope was opened: on a worker thread, it
     * would kill the cache of the whole request.
     */
    public static void close() {
        RequestCache cache = CURRENT.get();
        if (cache != null) {
            close(cache);
        }
    }

    /**
     * Kills the cache, and removes it from this thread if this thread still holds it. The action returned by
     * {@link #openIfAbsent(int)} may run late, or on another thread: it must not remove a cache bound in the meantime.
     * The listeners receive the cache only from the first close: a scope may be closed by its thread and by its closer.
     */
    private static void close(RequestCache cache) {
        boolean firstClose = cache.kill();
        if (CURRENT.get() == cache) {
            CURRENT.remove();
        }
        if (firstClose) {
            notifyClosed(cache);
        }
    }

    private static void notifyClosed(RequestCache cache) {
        for (Consumer<RequestCache> listener : CLOSE_LISTENERS) {
            notifyListener(listener, cache);
        }
    }

    /**
     * Calls a listener, and logs its exception. The filter closes the scope in a {@code finally} block: the exception
     * would replace the outcome of the request, and skip the other listeners.
     */
    private static void notifyListener(Consumer<RequestCache> listener, RequestCache cache) {
        try {
            listener.accept(cache);
        } catch (RuntimeException e) {
            log.warn("A close listener of a request cache failed", e);
        }
    }

    /** Returns the cache bound to this thread, or null outside a scope. The cache is dead if its scope closed. */
    public static @Nullable RequestCache current() {
        return CURRENT.get();
    }

    /**
     * Opens a scope on this thread unless it holds a live cache already, and returns the action closing the scope
     * opened by this call. The action does nothing if this call opened no scope: the owner of the cache in place closes
     * it. Work outside a request, such as a catalog listing, uses it to look each object up once during the operation.
     * The cache dies by itself {@link #OPERATION_MAX_AGE} after it opened, if the action did not run before.
     */
    public static Runnable openIfAbsent(int maxEntries) {
        if (holdsLiveCache()) {
            return NOTHING_TO_CLOSE;
        }
        closeDeadCache();
        RequestCache opened = new RequestCache(maxEntries, OPERATION_MAX_AGE);
        CURRENT.set(opened);
        return () -> close(opened);
    }

    /**
     * Calls the listener when the scope of a cache closes, once per cache, on the thread closing the scope. The counts
     * of the cache, such as {@link RequestCache#hits()}, survive its end. An exception of the listener is logged, and
     * the other listeners are still called. Returns the action removing the listener.
     */
    public static Runnable onClose(Consumer<RequestCache> listener) {
        CLOSE_LISTENERS.add(listener);
        return () -> CLOSE_LISTENERS.remove(listener);
    }

    /**
     * Runs an action with no cache bound to this thread, then binds the cache of this thread again, even if the action
     * fails. The lookups of the action return objects of their own, as outside a request. An object kept beyond the
     * request, for example by a cache shared by the requests, must be such an object: a save in the request commits its
     * changes into the objects held by the cache of the request.
     */
    public static <T> T withoutCache(Supplier<T> action) {
        RequestCache bound = CURRENT.get();
        CURRENT.remove();
        try {
            return action.get();
        } finally {
            apply(bound);
        }
    }

    /**
     * Binds a cache opened by another thread to this thread, for a thread-local transfer to a worker thread. A null
     * cache removes the cache of this thread: the task of a thread without a scope gets none.
     */
    static void apply(@Nullable RequestCache cache) {
        CURRENT.set(cache);
    }

    /** Removes the cache from this thread without killing it: the thread owning the scope still uses it. */
    static void cleanup() {
        CURRENT.remove();
    }
}
