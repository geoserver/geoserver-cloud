/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.catalog.backend.datadir;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.resolving.ProxyUtils;
import org.geoserver.catalog.plugin.resolving.ResolvingProxyResolver;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.springframework.lang.Nullable;

/**
 * Manages deferred catalog operations to ensure eventual consistency when distributed events arrive
 * out of order.
 *
 * <p>This component tracks catalog operations that cannot complete immediately because they
 * reference objects not yet present in the local catalog. It maintains a pending operations queue
 * indexed by the IDs of missing referenced objects, automatically resolving operations as their
 * dependencies arrive.
 *
 * <h2>Pending Operations</h2>
 *
 * <p>A pending operation is a catalog modification (add, update, remove, or set default) that has
 * been deferred because it contains unresolved {@link ResolvingProxy} references to objects not yet
 * available in the catalog. Each pending operation is represented by a {@link ConsistencyOp}
 * subclass that encapsulates the operation's logic and tracks which object IDs it's waiting for.
 *
 * <p>Pending operations are stored in a map keyed by the ID of the missing object they depend on.
 * Multiple operations can wait for the same missing object. When that object arrives, all operations
 * waiting for it are retried. Operations that successfully resolve are removed from the pending
 * queue, while those still missing other dependencies remain queued under their remaining unresolved
 * reference IDs.
 *
 * <p>Pending operations typically occur only under load, such as when populating the catalog through
 * the REST API or during service startup in a multi-node deployment. References usually remain
 * unresolved for only milliseconds (typically not more than 100 milliseconds even under high stress),
 * but this brief inconsistency window is sufficient to cause CREATE/READ workflows to fail if query
 * operations are not retried. The {@link EventuallyConsistentCatalogFacade} addresses this by
 * implementing retry logic for query operations while the catalog converges.
 *
 * <h2>Operation Processing</h2>
 *
 * <p>All mutating catalog operations (add, update, remove, set defaults) are intercepted. Each
 * operation is scanned for unresolved {@link ResolvingProxy} references. Operations with missing
 * dependencies are queued until those dependencies arrive. When an object arrives that other
 * operations are waiting for, cascading resolution is triggered. Removal operations either complete
 * or discard dependent operations.
 *
 * <p>Operation lifecycle: First, the operation checks for unresolved references. If references are
 * missing, the operation is queued by missing reference IDs. When a referenced object arrives,
 * operations waiting for it retry. Successfully resolved operations are removed from the pending
 * queue.
 *
 * <p>All operations are protected by a {@link ReentrantLock} to ensure safe concurrent access to
 * the pending operations map.
 *
 * <p>Example: A LayerInfo referencing a ResourceInfo with ID "resource-123" arrives before the
 * ResourceInfo. The LayerInfo add operation is queued under "resource-123". When the ResourceInfo
 * add operation executes, the enforcer detects pending operations waiting for "resource-123", adds
 * the ResourceInfo successfully, then retries the LayerInfo add operation which now succeeds.
 *
 * @since 1.9
 * @see EventuallyConsistentCatalogFacade
 * @see ResolvingProxy
 * @see ConsistencyOp
 */
@Slf4j(topic = "org.geoserver.cloud.catalog.backend.datadir")
public class EventualConsistencyEnforcer implements GeoServerLifecycleHandler {

    /**
     * Pending operations indexed by the IDs of missing objects they depend on.
     *
     * <p>Access to this map is protected by {@link #lock}. All public methods that read or modify
     * this map acquire the lock through {@link #execute(ConsistencyOp)}, {@link #forceResolve()},
     * {@link #forceClearPending()}, or {@link #isConverged()}. A regular {@code HashMap} is used
     * instead of {@code ConcurrentHashMap} since the lock provides all necessary synchronization.
     */
    private final Map<String, List<ConsistencyOp<?>>> pendingOperations = new HashMap<>();

    /**
     * Lock protecting access to {@link #pendingOperations} and ensuring atomic operation execution.
     */
    private final ReentrantLock lock = new ReentrantLock();

    @Setter
    private ExtendedCatalogFacade rawFacade;

    public EventualConsistencyEnforcer() {
        log.debug("raw catalog facade to be set using setter injection");
    }

    EventualConsistencyEnforcer(@NonNull ExtendedCatalogFacade rawFacade) {
        this.rawFacade = rawFacade;
    }

    /**
     * Clears all pending operations when the catalog is disposed.
     *
     * <p>This lifecycle method ensures clean shutdown by discarding any operations that were
     * deferred due to missing dependencies. Called automatically by the GeoServer lifecycle
     * management system during application shutdown.
     *
     * @see #forceClearPending()
     */
    @Override
    public void onDispose() {
        forceClearPending();
    }

    /**
     * Forcibly clears all pending operations from the queue.
     *
     * <p>This method immediately discards all deferred operations regardless of their state,
     * without attempting to resolve them. Used during lifecycle transitions like catalog
     * disposal and reload to prevent stale operations from interfering with the new catalog
     * state.
     *
     * <p>This operation is thread-safe and acquires the internal lock to ensure consistency.
     *
     * @see #onDispose()
     * @see #beforeReload()
     */
    private void forceClearPending() {
        lock.lock();
        try {
            pendingOperations.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clears all pending operations before catalog reload.
     *
     * <p>This lifecycle method prevents stale deferred operations from interfering with the
     * reloaded catalog state. Called automatically by the GeoServer lifecycle management system
     * before the catalog is reloaded, ensuring a clean slate for the reload process.
     *
     * @see #forceClearPending()
     */
    @Override
    public void beforeReload() {
        forceClearPending();
    }

    /**
     * No-op, this is called after the catalog is reloaded. We've already cleared pending operations at {@link #beforeReload()}
     */
    @Override
    public void onReload() {
        // no-op
    }

    /**
     * No-op, called by the UI to reset caches.
     */
    @Override
    public void onReset() {
        // no-op
    }

    /**
     * Checks whether all deferred operations have been resolved.
     *
     * <p>Returns {@code true} when the catalog has reached a consistent state with no pending
     * operations waiting for missing dependencies. This indicates all distributed events received
     * so far have been successfully applied.
     *
     * @return {@code true} if there are no pending operations, {@code false} if operations are
     *     still waiting for dependencies
     */
    boolean isConverged() {
        lock.lock();
        try {
            return pendingOperations.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Called during {@link EventuallyConsistentCatalogFacade} retries, attempts to resolve all pending operations by checking if their dependencies have arrived.
     *
     * <p>This method scans the pending operations queue and retries each operation to see if
     * previously missing dependencies are now available in the catalog. Useful for forcing
     * convergence after a batch of events has been processed, or when testing to verify all
     * operations eventually complete.
     *
     * <p>Operations that successfully resolve are removed from the pending queue. Operations that
     * still have missing dependencies remain queued.
     */
    public void forceResolve() {
        lock.lock();
        try {
            resolvePending();
        } finally {
            lock.unlock();
        }
    }

    private void resolvePending() {
        if (pendingOperations.size() > 0) {
            for (String missingRef : Set.copyOf(pendingOperations.keySet())) {
                tryResolvePending(missingRef);
            }
        }
    }

    private void tryResolvePending(String missingRef) {
        List<ConsistencyOp<?>> pending = pendingOperations.get(missingRef);
        if (pending != null && pending.isEmpty()) {
            pendingOperations.remove(missingRef);
            return;
        }
        Optional<CatalogInfo> found = rawFacade.get(missingRef);
        if (found.isPresent()) {
            log.debug("previously missing ref {} found, resolving pending operations waiting for it", missingRef);
            tryResolvePending(found.orElseThrow());
        } else {
            log.debug(
                    "missing ref {} still not found, the following operations wait for it: {}",
                    missingRef,
                    pendingOperations.get(missingRef));
        }
    }

    private void tryResolvePending(CatalogInfo resolved) {
        List<ConsistencyOp<?>> pending = List.copyOf(pendingOperations.getOrDefault(resolved.getId(), List.of()));
        for (ConsistencyOp<?> op : pending) {
            log.debug("converging operation for resolved {}: {}", resolved.getId(), op);
            execute(op);
        }
    }

    private <T> T execute(ConsistencyOp<T> op) {
        lock.lock();
        try {
            T ret = op.call();
            if (!op.completedSuccessfully() && log.isDebugEnabled()) {
                log.debug("operation not converged {}, misses {}", op, op.unresolvedRefs);
            }
            return ret;
        } catch (Exception e) {
            log.error("Error executing {}", op, e);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds a catalog object with eventual consistency enforcement.
     *
     * <p>If the object contains unresolved {@link ResolvingProxy} references to other objects not
     * yet present in the catalog, the add operation is deferred until those dependencies arrive.
     * Once all dependencies are resolved, the object is added to the catalog and any operations
     * waiting for this object are triggered.
     *
     * <p>If the object has no missing dependencies, it is added immediately to the underlying
     * catalog facade. If an object with the same ID already exists (which can happen when a node
     * starts while events are being broadcast), the operation is silently ignored.
     *
     * @param <T> the type of catalog object
     * @param info the catalog object to add, may contain ResolvingProxy references
     * @return the added object (or the input object if the operation was deferred)
     */
    @NonNull
    public <T extends CatalogInfo> T add(@NonNull T info) {
        return execute(new AddOp<>(info));
    }

    /**
     * Removes a catalog object with eventual consistency enforcement.
     *
     * <p>If the object to remove is a {@link ResolvingProxy} (not yet resolved), the operation is
     * deferred until the object is found in the catalog. When executed, any pending operations
     * waiting for this object are either completed (if their other dependencies are met) or
     * discarded (if they still have unresolved dependencies).
     *
     * <p>This ensures cascading removal behavior: operations that depend on the removed object are
     * given a final chance to complete before the dependency is removed, preventing catalog
     * inconsistencies.
     *
     * @param info the catalog object to remove, may be a ResolvingProxy
     */
    public void remove(@NonNull CatalogInfo info) {
        execute(new RemoveOp(info));
    }

    /**
     * Updates a catalog object with eventual consistency enforcement.
     *
     * <p>If the object to update is a {@link ResolvingProxy} or if the patch contains ResolvingProxy
     * property values, the operation is deferred until all references are resolved. The patch is
     * scanned for unresolved references and the operation waits for them to arrive.
     *
     * <p>Once the object and all patch property references are resolved, the patch is applied to
     * the object in the catalog.
     *
     * @param <I> the type of catalog object
     * @param info the catalog object to update, may be a ResolvingProxy
     * @param patch the property changes to apply, may contain ResolvingProxy values
     * @return the updated object (or the input object if the operation was deferred)
     */
    @NonNull
    public <I extends CatalogInfo> I update(I info, Patch patch) {
        return execute(new UpdateOp<>(info, patch));
    }

    /**
     * Sets the default workspace with eventual consistency enforcement.
     *
     * <p>If the workspace is a {@link ResolvingProxy}, the operation is deferred until the
     * workspace is found in the catalog. Passing {@code null} clears the default workspace
     * immediately.
     *
     * @param workspace the workspace to set as default, may be a ResolvingProxy or {@code null}
     */
    public void setDefaultWorkspace(@Nullable WorkspaceInfo workspace) {
        execute(new SetDefaultWorkspace(workspace));
    }

    /**
     * Sets the default namespace with eventual consistency enforcement.
     *
     * <p>If the namespace is a {@link ResolvingProxy}, the operation is deferred until the
     * namespace is found in the catalog. Passing {@code null} clears the default namespace
     * immediately.
     *
     * @param namespace the namespace to set as default, may be a ResolvingProxy or {@code null}
     */
    public void setDefaultNamespace(@Nullable NamespaceInfo namespace) {
        execute(new SetDefaultNamespace(namespace));
    }

    /**
     * Sets the default data store for a workspace with eventual consistency enforcement.
     *
     * <p>If either the workspace or store are {@link ResolvingProxy} instances, the operation is
     * deferred until both are resolved. Passing {@code null} for the store clears the default
     * data store for the workspace once the workspace is resolved.
     *
     * @param workspace the workspace whose default store to set, may be a ResolvingProxy
     * @param store the data store to set as default, may be a ResolvingProxy or {@code null}
     */
    public void setDefaultDataStore(@NonNull WorkspaceInfo workspace, @Nullable DataStoreInfo store) {
        execute(new SetDefaultDataStore(workspace, store));
    }

    /**
     * Base class for all deferred catalog operations that support eventual consistency.
     *
     * <p>A {@code ConsistencyOp} represents a catalog modification operation that may need to be
     * deferred if it contains unresolved {@link ResolvingProxy} references. Each operation tracks
     * which object IDs it's waiting for and automatically retries when those dependencies become
     * available.
     *
     * <p>The operation lifecycle is managed by the {@link #call()} method, which checks for missing
     * references before and after attempting resolution. If all references are resolved, the
     * operation completes and is removed from the pending queue. If references are still missing,
     * the operation updates its pending state to wait only for the remaining unresolved references.
     *
     * <p>Each operation has a unique ID for tracking purposes and implements proper equality based
     * on this ID to ensure correct behavior when stored in collections.
     *
     * <h3>Subclasses</h3>
     * <ul>
     * <li>{@link AddOp} - Defers adding catalog objects until all referenced objects exist
     * <li>{@link UpdateOp} - Defers updates until the object and patch references are resolved
     * <li>{@link RemoveOp} - Defers removal and manages cascading dependent operation cleanup
     * <li>{@link SetDefaultWorkspace} - Defers setting default workspace until it exists
     * <li>{@link SetDefaultNamespace} - Defers setting default namespace until it exists
     * <li>{@link SetDefaultDataStore} - Defers setting default store until workspace and store exist
     * </ul>
     *
     * @param <T> the return type of the operation
     * @see #getMissingRefs()
     * @see #resolve()
     * @see #completedSuccessfully()
     */
    private abstract class ConsistencyOp<T> implements Callable<T> {

        private final UUID operationId = UUID.randomUUID();

        protected boolean success;

        /** Cached unresolved references after last call(), for debug logging without recomputation */
        private Set<String> unresolvedRefs = Set.of();

        /** Original missing references from first invocation, used to track all dependencies */
        private Set<String> originalMissingRefs = null;

        @Override
        public final boolean equals(Object o) {
            return o instanceof ConsistencyOp<?> op && operationId.equals(op.operationId);
        }

        @Override
        public final int hashCode() {
            return operationId.hashCode();
        }

        /**
         * Computes the IDs of catalog objects this operation is currently waiting for.
         *
         * <p>Called before and after {@link #resolve()} to track which dependencies remain unmet.
         * Operations register themselves in {@code pendingOperations} under these IDs.
         *
         * <p>May perform resolution attempts and update internal state as a side effect.
         */
        @NonNull
        abstract Set<String> computeMissingRefs();

        /**
         * Executes the operation, managing its registration in {@code pendingOperations}.
         *
         * <p>Checks missing references before and after calling {@link #resolve()}. If successful,
         * removes the operation from all pending queues. If still incomplete, updates registration
         * to wait only for remaining unresolved references.
         *
         * @return the unresolved references if operation didn't complete, empty set otherwise
         */
        @Override
        public final T call() {
            Set<String> pre = Set.copyOf(computeMissingRefs());

            // Cache original missing refs on first invocation
            if (originalMissingRefs == null) {
                originalMissingRefs = Set.copyOf(pre);
            }

            if (!pre.isEmpty()) {
                log.debug("{} is missing refs {}", this, pre);
            }
            T result = resolve();
            if (completedSuccessfully()) {
                if (pre.isEmpty()) {
                    purge(this);
                } else {
                    pre.forEach(ref -> unsetPending(ref, this));
                }
                afterSuccess();
                this.unresolvedRefs = Set.of();
            } else {
                Set<String> resolved;
                Set<String> unresolved = computeMissingRefs();
                if (unresolved.isEmpty()) {
                    resolved = pre;
                } else {
                    resolved = Sets.difference(pre, unresolved);
                }
                resolved.forEach(ref -> unsetPending(ref, this));
                unresolved.forEach(ref -> setPending(ref, this));
                this.unresolvedRefs = unresolved;
            }
            return result;
        }

        /** Called after successful completion. Override to trigger additional actions. */
        protected void afterSuccess() {
            // override as needed
        }

        /**
         * Attempts to execute the catalog operation.
         *
         * <p>Implementations should check if all dependencies are met and execute the operation if
         * possible, setting {@code success = true} on completion. Returns the operation result or
         * partial state if deferred.
         */
        protected abstract T resolve();

        /** Returns whether the operation completed successfully. */
        protected boolean completedSuccessfully() {
            return success;
        }

        /**
         * Returns the original set of missing references from the first invocation.
         *
         * <p>Used to determine if an operation should be discarded when a dependency is removed,
         * even if the operation has moved on to waiting for other dependencies.
         */
        protected Set<String> getOriginalMissingRefs() {
            return originalMissingRefs == null ? Set.of() : originalMissingRefs;
        }

        private void setPending(String missingRef, ConsistencyOp<?> deferredOp) {
            log.debug("missing ref {}, deferring execution of {}", missingRef, deferredOp);
            List<ConsistencyOp<?>> pending = pendingOperations.computeIfAbsent(missingRef, ref -> new ArrayList<>());
            if (!pending.contains(deferredOp)) {
                pending.add(deferredOp);
            }
        }

        private void unsetPending(String resolvedRef, ConsistencyOp<?> completedOp) {
            log.debug("missing ref {} resolved, completed {}", resolvedRef, completedOp);
            remove(resolvedRef, completedOp);
        }

        /**
         * Removes an operation from all pending queues, discarding it completely.
         *
         * <p>Used when an operation should no longer be retried, typically because a dependency it
         * references is being removed from the catalog.
         */
        protected void discard(ConsistencyOp<?> op) {
            for (String ref : Set.copyOf(pendingOperations.keySet())) {
                List<ConsistencyOp<?>> pending = pendingOperations.get(ref);
                boolean removed = pending.remove(op);
                if (removed) {
                    log.debug("discarded op waiting for ref {}: {}", ref, op);
                }
                if (pending.isEmpty()) {
                    pendingOperations.remove(ref);
                }
            }
        }

        /**
         * Removes a completed operation from all pending queues.
         *
         * <p>Called when an operation succeeded despite having no missing refs at the start (edge
         * case cleanup).
         */
        protected void purge(ConsistencyOp<?> op) {
            Set<String> removedFromRefs = new HashSet<>();
            for (String ref : Set.copyOf(pendingOperations.keySet())) {
                List<ConsistencyOp<?>> pending = pendingOperations.get(ref);
                boolean removed = pending.remove(op);
                if (removed) {
                    removedFromRefs.add(ref);
                }
                if (pending.isEmpty()) {
                    pendingOperations.remove(ref);
                }
            }
            if (!removedFromRefs.isEmpty()) {
                log.debug("purged op {} from pending refs {}", op, removedFromRefs);
            }
        }

        private void remove(String resolvedRef, ConsistencyOp<?> completedOp) {
            List<ConsistencyOp<?>> pendingForRef = pendingOperations.get(resolvedRef);
            if (null != pendingForRef) {
                pendingForRef.remove(completedOp);
                if (pendingForRef.isEmpty()) {
                    log.debug("there are no more consistency ops waiting for {}", resolvedRef);
                    pendingOperations.remove(resolvedRef);
                }
            }
        }
    }

    /**
     * Operation that adds a catalog object to the facade, deferring execution until all referenced
     * objects exist.
     *
     * <p>Scans the object for unresolved {@link ResolvingProxy} references using {@link
     * ResolvingProxyResolver}. If references are missing, the operation is queued. Once all
     * references resolve, the object is added to the catalog via {@link
     * ExtendedCatalogFacade#add(CatalogInfo)}.
     *
     * <p>If the object already exists when the operation finally executes (which can happen when a
     * node starts while events are being broadcast), the add is silently ignored to maintain
     * idempotency.
     *
     * <p>After successfully adding the object, triggers resolution of any pending operations that
     * were waiting for this object via {@link #afterSuccess()}.
     *
     * @param <T> the type of catalog object being added
     */
    @RequiredArgsConstructor
    private class AddOp<T extends CatalogInfo> extends ConsistencyOp<T> {

        private @NonNull T info;

        @Override
        Set<String> computeMissingRefs() {
            if (completedSuccessfully()) {
                return Set.of();
            }
            return findMissingRefs(info);
        }

        private <I extends CatalogInfo> Set<String> findMissingRefs(I info) {
            final Set<String> missing = new HashSet<>();
            ResolvingProxyResolver<CatalogInfo> lookup = ResolvingProxyResolver.<CatalogInfo>of(rawFacade.getCatalog());
            lookup.onNotFound((proxied, proxy) -> missing.add(proxy.getRef()));
            lookup.apply(info);
            return missing;
        }

        @Override
        protected T resolve() {
            Set<String> missing = computeMissingRefs();
            if (missing.isEmpty()) {
                // check if the object already exists, may happen on a node that started while the
                // events are being sent
                Class<T> type = ConfigInfoType.valueOf(info).type();
                rawFacade
                        .get(info.getId(), type)
                        .ifPresentOrElse(existing -> log.info("Ignoring add, object exists {}", info.getId()), () -> {
                            T added = rawFacade.add(info);
                            this.info = added;
                            success = true;
                        });
            }
            // return info to comply with the catalog facade add() contract
            // but delay insertion of info as it has unresolved references
            return info;
        }

        /**
         * Triggers resolution of any operations that were waiting for the object just added.
         *
         * <p>After successfully adding an object, attempts to execute any pending operations that
         * were blocked waiting for this object's ID.
         */
        @Override
        protected void afterSuccess() {
            tryResolvePending(this.info);
        }

        @Override
        public String toString() {
            return "%s(%s)".formatted(getClass().getSimpleName(), info.getId());
        }
    }

    /**
     * Operation that updates a catalog object with property changes, deferring execution until the
     * object and all patch property references are resolved.
     *
     * <p>Handles two types of unresolved references: the object being updated may itself be a
     * {@link ResolvingProxy}, and the {@link Patch} may contain property values that are
     * ResolvingProxy instances. Both are resolved using {@link ResolvingProxyResolver} before the
     * update can proceed.
     *
     * <p>Once the object is found and all patch properties are resolved, applies the patch via
     * {@link ExtendedCatalogFacade#update(CatalogInfo, Patch)}.
     *
     * @param <T> the type of catalog object being updated
     */
    @RequiredArgsConstructor
    private class UpdateOp<T extends CatalogInfo> extends ConsistencyOp<T> {
        /** Object to update, may be a ResolvingProxy itself */
        private @NonNull T toUpdate;

        /** Patch to apply, may contain ResolvingProxy properties */
        private @NonNull Patch patch;

        @Override
        public String toString() {
            return "%s(%s, patch: %s)"
                    .formatted(getClass().getSimpleName(), toUpdate.getId(), patch.getPropertyNames());
        }

        @Override
        @NonNull
        Set<String> computeMissingRefs() {
            if (completedSuccessfully()) {
                return Set.of();
            }
            HashSet<String> missing = new HashSet<>();
            if (ProxyUtils.isResolvingProxy(toUpdate)) {
                missing.add(toUpdate.getId());
            }
            patch = resolvePatch(patch, missing);
            return missing;
        }

        @Override
        protected T resolve() {
            Optional<T> found = objectToUpdate();
            if (found.isPresent()) {
                this.toUpdate = found.orElseThrow();
                Set<String> missingInfoIds = new HashSet<>();
                this.patch = resolvePatch(patch, missingInfoIds);
                if (missingInfoIds.isEmpty()) {
                    success = true;
                    return rawFacade.update(toUpdate, patch);
                }
            }
            return toUpdate;
        }

        @SuppressWarnings("unchecked")
        private Optional<T> objectToUpdate() {
            return rawFacade.get(toUpdate.getId()).map(found -> (T) found);
        }

        private Patch resolvePatch(Patch patch, Set<String> target) {
            ResolvingProxyResolver<CatalogInfo> lookup = ResolvingProxyResolver.<CatalogInfo>of(rawFacade.getCatalog());
            lookup.onNotFound((proxied, proxy) -> target.add(proxy.getRef()));
            return lookup.resolve(patch);
        }
    }

    /**
     * Operation that removes a catalog object, managing cascading cleanup of dependent operations.
     *
     * <p>If the object to remove is a {@link ResolvingProxy}, defers execution until the object is
     * found in the catalog. When the removal finally executes, handles operations that were waiting
     * for the removed object in a special way:
     *
     * <ul>
     * <li>Each dependent operation is given one final retry attempt
     * <li>If the dependent operation completes successfully (its other dependencies are met), it's
     *     removed from the pending queue
     * <li>If the dependent operation still has unresolved dependencies, it's forcibly discarded
     *     with a warning, as its dependency is about to be removed
     * </ul>
     *
     * <p>This cascading behavior prevents catalog inconsistencies where operations reference objects
     * that no longer exist, while giving operations a chance to complete if their other dependencies
     * have arrived.
     */
    @RequiredArgsConstructor
    private class RemoveOp extends ConsistencyOp<Void> {
        /**
         * Object to remove, may be a {@link ResolvingProxy} itself, in which case the operation is
         * deferred until the object is found. When executed, any pending operation waiting for this
         * object is discarded
         */
        private @NonNull CatalogInfo info;

        @Override
        public String toString() {
            return "%s(%s)".formatted(getClass().getSimpleName(), info.getId());
        }

        @Override
        Set<String> computeMissingRefs() {
            if (ProxyUtils.isResolvingProxy(info)) {
                return Set.of(info.getId());
            }
            return Set.of();
        }

        /**
         * Retrieves all operations that depend on the object being removed.
         *
         * <p>Scans all pending operations to find those whose original missing references included
         * the removed object. This handles cases where an operation has moved from waiting for the
         * removed object to waiting for other dependencies - such operations must still be
         * discarded to prevent dangling references.
         *
         * <p>These operations will be given a final chance to complete in {@link
         * #completeOrDiscard(ConsistencyOp)}.
         */
        private List<ConsistencyOp<?>> clearDependants() {
            Set<ConsistencyOp<?>> dependants = new HashSet<>();
            String removedId = info.getId();

            // Scan all pending operations to find those that originally depended on this object
            for (List<ConsistencyOp<?>> ops : pendingOperations.values()) {
                for (ConsistencyOp<?> op : ops) {
                    if (op.getOriginalMissingRefs().contains(removedId)) {
                        dependants.add(op);
                    }
                }
            }

            return List.copyOf(dependants);
        }

        @Override
        protected Void resolve() {
            if (ProxyUtils.isResolvingProxy(info)) {
                final String id = info.getId();
                Optional<CatalogInfo> found = rawFacade.get(id);
                if (found.isPresent()) {
                    this.info = found.orElseThrow();
                } else {
                    // this.info is still a ResolvingProxy, defer operation
                    return null;
                }
            }

            // Find all operations that depend on this object
            List<ConsistencyOp<?>> waitingForThis = clearDependants();

            // Remove the object FIRST, before attempting to complete/discard dependents
            // This ensures dependents cannot succeed with a reference to the removed object
            rawFacade.remove(ModificationProxy.unwrap(info));
            success = true;

            // Now try to complete or discard dependent operations
            for (ConsistencyOp<?> dependent : waitingForThis) {
                completeOrDiscard(dependent);
            }

            return null;
        }

        /**
         * Gives a dependent operation a final attempt to complete before the object is removed.
         *
         * <p>Executes the operation once more. If it succeeds (all other dependencies met), it's
         * removed from pending queues naturally. If it fails (still has unmet dependencies), the
         * operation is forcibly discarded since its dependency is about to be removed.
         *
         * <p>This prevents operations from lingering with references to removed objects while
         * allowing them to complete if only this object was blocking them.
         */
        private void completeOrDiscard(ConsistencyOp<?> dependent) {
            final String id = info.getId();
            execute(dependent);
            if (dependent.completedSuccessfully()) {
                log.debug("successfully executed {} depending on {} before removing it", dependent, id);
            } else {
                log.warn(
                        "operation dependent on {} didn't complete successfully before removing it. It will be discarded: {}",
                        id,
                        dependent);
                // discard the op in case its waiting for some other ref besides the object removed
                // by this op
                discard(dependent);
            }
        }
    }

    /**
     * Operation that sets the catalog's default workspace, deferring execution until the workspace
     * exists.
     *
     * <p>If the workspace is a {@link ResolvingProxy}, waits for the workspace to arrive before
     * setting it as default. Passing {@code null} clears the default workspace immediately without
     * deferral.
     */
    @RequiredArgsConstructor
    private class SetDefaultWorkspace extends ConsistencyOp<Void> {
        @Nullable
        private final WorkspaceInfo workspace;

        @Override
        Set<String> computeMissingRefs() {
            if (ProxyUtils.isResolvingProxy(workspace)) {
                return Set.of(workspace.getId());
            }
            return Set.of();
        }

        @Override
        protected Void resolve() {
            if (null == workspace) {
                success = true;
                rawFacade.setDefaultWorkspace(null);
            } else {
                String id = workspace.getId();
                Optional<WorkspaceInfo> found = rawFacade.get(id, WorkspaceInfo.class);
                if (found.isPresent()) {
                    success = true;
                    rawFacade.setDefaultWorkspace(found.orElseThrow());
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return "%s(workspace: %s)"
                    .formatted(getClass().getSimpleName(), (workspace == null ? null : workspace.getId()));
        }
    }

    /**
     * Operation that sets the catalog's default namespace, deferring execution until the namespace
     * exists.
     *
     * <p>If the namespace is a {@link ResolvingProxy}, waits for the namespace to arrive before
     * setting it as default. Passing {@code null} clears the default namespace immediately without
     * deferral.
     */
    @RequiredArgsConstructor
    private class SetDefaultNamespace extends ConsistencyOp<Void> {
        @Nullable
        private final NamespaceInfo namespace;

        @Override
        Set<String> computeMissingRefs() {
            if (ProxyUtils.isResolvingProxy(namespace)) {
                return Set.of(namespace.getId());
            }
            return Set.of();
        }

        @Override
        protected Void resolve() {
            if (null == namespace) {
                success = true;
                rawFacade.setDefaultNamespace(null);
            } else {
                String id = namespace.getId();
                Optional<NamespaceInfo> found = rawFacade.get(id, NamespaceInfo.class);
                if (found.isPresent()) {
                    success = true;
                    rawFacade.setDefaultNamespace(found.orElseThrow());
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return "%s(namespace: %s)"
                    .formatted(getClass().getSimpleName(), (namespace == null ? null : namespace.getId()));
        }
    }

    /**
     * Operation that sets the default data store for a workspace, deferring execution until both
     * the workspace and store exist.
     *
     * <p>Handles two potential {@link ResolvingProxy} references: the workspace and the data store.
     * Both must be resolved before the operation can complete. Passing {@code null} for the store
     * clears the default data store for the workspace once the workspace is resolved.
     */
    @AllArgsConstructor
    private class SetDefaultDataStore extends ConsistencyOp<Void> {
        @NonNull
        private WorkspaceInfo workspace;

        @Nullable
        private DataStoreInfo store;

        @Override
        Set<String> computeMissingRefs() {
            if (completedSuccessfully()) {
                return Set.of();
            }
            HashSet<String> missing = new HashSet<>();
            if (ProxyUtils.isResolvingProxy(workspace)) {
                missing.add(workspace.getId());
            }
            if (ProxyUtils.isResolvingProxy(store)) {
                missing.add(store.getId());
            }
            return missing;
        }

        @Override
        protected Void resolve() {
            WorkspaceInfo ws = rawFacade.getWorkspace(workspace.getId());
            if (null == ws) {
                return null;
            }
            this.workspace = ws;
            if (null == store) {
                success = true;
                rawFacade.setDefaultDataStore(ws, null);
            } else {
                String storeId = store.getId();
                Optional<DataStoreInfo> found = rawFacade.get(storeId, DataStoreInfo.class);
                if (found.isPresent()) {
                    success = true;
                    this.store = found.orElseThrow();
                    rawFacade.setDefaultDataStore(ws, store);
                }
            }

            return null;
        }

        @Override
        public String toString() {
            return "%s(workspace: %s, store: %s)"
                    .formatted(getClass().getSimpleName(), workspace.getId(), (store == null ? null : store.getId()));
        }
    }
}
