/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
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
 * Aids {@link EventuallyConsistentCatalogFacade} in ensuring the catalog stays consistent and
 * eventually converges while remote events may arrive out of order and {@link CatalogInfo}s
 * reference objects not yet added to the local catalog.
 *
 * <p>
 *
 * <ul>
 *   <li>All {@code add()} methods check if the {@link CatalogInfo} being added have unresolved
 *       ({@link ResolvingProxy}) references
 *   <li>If so, the object is put in a pending list and not added
 *   <li>Conversely, if during {@code add()}, there's a pending add waiting for this new object, the
 *       {@code add()} proceeds and then the pending object is added
 * </ul>
 *
 * @since 1.9
 */
@Slf4j(topic = "org.geoserver.cloud.catalog.backend.datadir")
public class EventualConsistencyEnforcer implements GeoServerLifecycleHandler {

    private final Map<String, List<ConsistencyOp<?>>> pendingOperations = new HashMap<>();

    @Setter
    private ExtendedCatalogFacade rawFacade;

    private final ReentrantLock lock = new ReentrantLock();

    public EventualConsistencyEnforcer() {
        log.debug("raw catalog facade to be set using setter injection");
    }

    EventualConsistencyEnforcer(@NonNull ExtendedCatalogFacade rawFacade) {
        this.rawFacade = rawFacade;
    }

    @Override
    public void onDispose() {
        lock.lock();
        try {
            pendingOperations.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void beforeReload() {
        onDispose();
    }

    @Override
    public void onReset() {
        // no-op
    }

    @Override
    public void onReload() {
        // no-op
    }

    boolean isConverged() {
        lock.lock();
        try {
            return pendingOperations.isEmpty();
        } finally {
            lock.unlock();
        }
    }

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
        var pending = pendingOperations.get(missingRef);
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
                    "missing ref {} still not found, the follwing operations wait for it: {}",
                    missingRef,
                    pendingOperations.get(missingRef));
        }
    }

    private void tryResolvePending(CatalogInfo resolved) {
        var pending = List.copyOf(pendingOperations.getOrDefault(resolved.getId(), List.of()));
        for (var op : pending) {
            log.debug("converging operation for resolved {}: {}", resolved.getId(), op);
            execute(op);
        }
    }

    private <T> T execute(ConsistencyOp<T> op) {
        lock.lock();
        try {
            T ret = op.call();
            if (!op.completedSuccessfully() && log.isDebugEnabled()) {
                log.debug("operation not converged {}, misses {}", op, op.getMissingRefs());
            }
            return ret;
        } catch (Exception e) {
            log.error("Error executing {}", op, e);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @NonNull
    public <T extends CatalogInfo> T add(@NonNull T info) {
        return execute(new AddOp<>(info));
    }

    public void remove(@NonNull CatalogInfo info) {
        execute(new RemoveOp(info));
    }

    @NonNull
    public <I extends CatalogInfo> I update(I info, Patch patch) {
        return execute(new UpdateOp<>(info, patch));
    }

    public void setDefaultWorkspace(@Nullable WorkspaceInfo workspace) {
        execute(new SetDefaultWorkspace(workspace));
    }

    public void setDefaultNamespace(@Nullable NamespaceInfo namespace) {
        execute(new SetDefaultNamespace(namespace));
    }

    public void setDefaultDataStore(@NonNull WorkspaceInfo workspace, @Nullable DataStoreInfo store) {
        execute(new SetDefaultDataStore(workspace, store));
    }

    private abstract class ConsistencyOp<T> implements Callable<T> {

        private final UUID operationId = UUID.randomUUID();

        protected boolean success;

        @Override
        public final boolean equals(Object o) {
            return o instanceof ConsistencyOp<?> op && operationId.equals(op.operationId);
        }

        @Override
        public final int hashCode() {
            return operationId.hashCode();
        }

        @NonNull
        abstract Set<String> getMissingRefs();

        @Override
        public final T call() {
            Set<String> pre = Set.copyOf(getMissingRefs());
            if (!pre.isEmpty()) log.debug("{} is missing refs {}", this, pre);
            T result = resolve();
            if (completedSuccessfully()) {
                if (pre.isEmpty()) {
                    purge(this);
                } else {
                    pre.forEach(ref -> unsetPending(ref, this));
                }
                afterSuccess();
                return result;
            }

            Set<String> resolved;
            Set<String> unresolved = getMissingRefs();
            if (unresolved.isEmpty()) {
                resolved = pre;
            } else {
                resolved = Sets.difference(pre, unresolved);
            }
            resolved.forEach(ref -> unsetPending(ref, this));
            unresolved.forEach(ref -> setPending(ref, this));
            return result;
        }

        protected void afterSuccess() {
            // override as needed
        }

        protected abstract T resolve();

        protected boolean completedSuccessfully() {
            return success;
        }

        private void setPending(String missingRef, ConsistencyOp<?> deferredOp) {
            log.debug("missing ref {}, deferring execution of {}", missingRef, deferredOp);
            pendingOperations
                    .computeIfAbsent(missingRef, ref -> new ArrayList<>())
                    .add(deferredOp);
        }

        private void unsetPending(String resolvedRef, ConsistencyOp<?> completedOp) {
            log.debug("missing ref {} resolved, completed {}", resolvedRef, completedOp);
            remove(resolvedRef, completedOp);
        }

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

    @RequiredArgsConstructor
    private class AddOp<T extends CatalogInfo> extends ConsistencyOp<T> {

        private @NonNull T info;

        @Override
        Set<String> getMissingRefs() {
            if (completedSuccessfully()) {
                return Set.of();
            }
            return findMissingRefs(info);
        }

        private <I extends CatalogInfo> Set<String> findMissingRefs(I info) {
            final Set<String> missing = new HashSet<>();
            var lookup = ResolvingProxyResolver.<CatalogInfo>of(rawFacade.getCatalog());
            lookup.onNotFound((proxied, proxy) -> missing.add(proxy.getRef()));
            lookup.apply(info);
            return missing;
        }

        @Override
        protected T resolve() {
            var missing = getMissingRefs();
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

        // resolve any pending ops waiting for the object just inserted
        @Override
        protected void afterSuccess() {
            tryResolvePending(this.info);
        }

        @Override
        public String toString() {
            return "%s(%s)".formatted(getClass().getSimpleName(), info.getId());
        }
    }

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
        Set<String> getMissingRefs() {
            if (completedSuccessfully()) return Set.of();
            var missing = new HashSet<String>();
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
                Set<String> missing = new HashSet<>();
                this.patch = resolvePatch(patch, missing);
                if (missing.isEmpty()) {
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
            var lookup = ResolvingProxyResolver.<CatalogInfo>of(rawFacade.getCatalog());
            lookup.onNotFound((proxied, proxy) -> target.add(proxy.getRef()));
            return lookup.resolve(patch);
        }
    }

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
        Set<String> getMissingRefs() {
            if (ProxyUtils.isResolvingProxy(info)) {
                return Set.of(info.getId());
            }
            return Set.of();
        }

        private List<ConsistencyOp<?>> clearDependants() {
            List<ConsistencyOp<?>> dependants = pendingOperations.remove(info.getId());
            return dependants == null ? List.of() : List.copyOf(dependants);
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

            // complete or discard any pending op waiting for this object
            var waitingForThis = clearDependants();
            for (var dependant : waitingForThis) {
                completeOrDiscard(dependant);
            }

            rawFacade.remove(ModificationProxy.unwrap(info));
            success = true;
            return null;
        }

        private void completeOrDiscard(ConsistencyOp<?> dependant) {
            final String id = info.getId();
            execute(dependant);
            if (dependant.completedSuccessfully()) {
                log.debug("successfully executed {} depending on {} before removing it", dependant, id);
            } else {
                log.warn(
                        "operation dependant on {} didn't complete successfully before removing it. It will be discarded: {}",
                        id,
                        dependant);
                // discard the op in case its waiting for some other ref besides the object removed
                // by this op
                discard(dependant);
            }
        }
    }

    @RequiredArgsConstructor
    private class SetDefaultWorkspace extends ConsistencyOp<Void> {
        @Nullable
        private final WorkspaceInfo workspace;

        @Override
        Set<String> getMissingRefs() {
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

    @RequiredArgsConstructor
    private class SetDefaultNamespace extends ConsistencyOp<Void> {
        @Nullable
        private final NamespaceInfo namespace;

        @Override
        Set<String> getMissingRefs() {
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

    @AllArgsConstructor
    private class SetDefaultDataStore extends ConsistencyOp<Void> {
        @NonNull
        private WorkspaceInfo workspace;

        @Nullable
        private DataStoreInfo store;

        @Override
        Set<String> getMissingRefs() {
            if (completedSuccessfully()) {
                return Set.of();
            }
            var missing = new HashSet<String>();
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
