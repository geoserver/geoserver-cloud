/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.catalog.backend.datadir;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.forwarding.ForwardingExtendedCatalogFacade;
import org.geoserver.ows.util.OwsUtils;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Catalog facade decorator that ensures eventual consistency when processing distributed catalog
 * events that may arrive out of order.
 *
 * <p>In a distributed GeoServer Cloud deployment, catalog modification events are broadcast over a
 * message bus to synchronize all nodes. Network latency and message delivery guarantees mean that
 * events may arrive at each node in a different order than they were generated. This creates a
 * consistency problem when catalog objects reference other objects:
 *
 * <ul>
 *   <li>A LayerInfo references a ResourceInfo and a StyleInfo
 *   <li>A ResourceInfo references a StoreInfo and a NamespaceInfo
 *   <li>A StoreInfo references a WorkspaceInfo
 * </ul>
 *
 * <p>If an event to add a LayerInfo arrives before the event to add its referenced ResourceInfo,
 * attempting to add the LayerInfo with unresolved references would fail or corrupt the catalog.
 *
 * <p><b>Solution:</b> This facade delegates all mutating operations ({@code add}, {@code update},
 * {@code remove}) to an {@link EventualConsistencyEnforcer}, which defers operations with
 * unresolved references until the referenced objects arrive. Query operations ({@code get*}
 * methods) implement retry logic for REST API requests, allowing brief wait periods for pending
 * operations to converge.
 *
 * <p><b>Example scenario:</b>
 *
 * <ol>
 *   <li>Node A creates a Workspace, Store, Resource, and Layer
 *   <li>Events arrive at Node B in order: Layer → Resource → Store → Workspace
 *   <li>Layer add is deferred (missing Resource reference)
 *   <li>Resource add is deferred (missing Store reference)
 *   <li>Store add is deferred (missing Workspace reference)
 *   <li>Workspace add succeeds, triggers Store add
 *   <li>Store add succeeds, triggers Resource add
 *   <li>Resource add succeeds, triggers Layer add
 *   <li>Catalog converges to consistent state
 * </ol>
 *
 * <p><b>Query retry behavior:</b>
 *
 * <p>Query methods (e.g., {@code getWorkspaceByName()}) implement retry logic to handle queries
 * that occur while pending operations are resolving:
 *
 * <ul>
 *   <li><b>Web requests (REST API, OWS services):</b> Retry when pending operations exist
 *       ({@code !isConverged()}). Each retry invokes {@code forceResolve()} to attempt resolving
 *       pending operations. If the catalog has converged (no pending operations), queries return
 *       immediately as retrying cannot find anything new.
 *   <li><b>Internal operations:</b> Never retry. Internal catalog access during startup or event
 *       processing returns immediately to avoid blocking.
 * </ul>
 *
 * <p>Retry intervals are configured via
 * {@code geoserver.backend.data-directory.eventual-consistency.retries} (default: [25, 25, 50] ms).
 *
 * @since 1.9
 * @see EventualConsistencyEnforcer
 * @see RemoteEventDataDirectoryProcessor
 * @see org.geoserver.cloud.config.catalog.backend.datadirectory.DataDirectoryProperties.EventualConsistencyConfig
 */
@Slf4j(topic = "org.geoserver.cloud.catalog.backend.datadir")
public class EventuallyConsistentCatalogFacade extends ForwardingExtendedCatalogFacade {

    private final EventualConsistencyEnforcer enforcer;

    /**
     * number of retry attempts and milliseconds to wait between retries
     *
     * @see #retry
     */
    private final @NonNull int[] retryAttemptMillis;

    public EventuallyConsistentCatalogFacade(
            @NonNull ExtendedCatalogFacade facade,
            @NonNull EventualConsistencyEnforcer tracker,
            @NonNull int[] retryAttemptMillis) {
        super(facade);
        this.retryAttemptMillis = retryAttemptMillis;
        this.enforcer = tracker;
    }

    @Override
    public <T extends CatalogInfo> T add(@NonNull T info) {
        return enforcer.add(info);
    }

    @Override
    public WorkspaceInfo add(@NonNull WorkspaceInfo info) {
        return enforcer.add(info);
    }

    @Override
    public NamespaceInfo add(@NonNull NamespaceInfo info) {
        return enforcer.add(info);
    }

    @Override
    public LayerGroupInfo add(@NonNull LayerGroupInfo info) {
        return enforcer.add(info);
    }

    @Override
    public LayerInfo add(@NonNull LayerInfo info) {
        return enforcer.add(info);
    }

    @Override
    public ResourceInfo add(@NonNull ResourceInfo info) {
        return enforcer.add(info);
    }

    @Override
    public StoreInfo add(@NonNull StoreInfo info) {
        return enforcer.add(info);
    }

    @Override
    public StyleInfo add(@NonNull StyleInfo info) {
        return enforcer.add(info);
    }

    @Override
    public <I extends CatalogInfo> I update(@NonNull I info, @NonNull Patch patch) {
        return enforcer.update(info, patch);
    }

    @Override
    public void remove(@NonNull CatalogInfo info) {
        enforcer.remove(info);
    }

    @Override
    public void remove(@NonNull WorkspaceInfo info) {
        enforcer.remove(info);
    }

    @Override
    public void remove(@NonNull NamespaceInfo info) {
        enforcer.remove(info);
    }

    @Override
    public void remove(@NonNull StoreInfo info) {
        enforcer.remove(info);
    }

    @Override
    public void remove(@NonNull ResourceInfo info) {
        enforcer.remove(info);
    }

    @Override
    public void remove(@NonNull LayerInfo info) {
        enforcer.remove(info);
    }

    @Override
    public void remove(@NonNull LayerGroupInfo info) {
        enforcer.remove(info);
    }

    @Override
    public void remove(@NonNull StyleInfo info) {
        enforcer.remove(info);
    }

    @Override
    public void remove(@NonNull MapInfo info) {
        enforcer.remove(info);
    }

    @Override
    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        enforcer.setDefaultWorkspace(workspace);
    }

    @Override
    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        enforcer.setDefaultNamespace(defaultNamespace);
    }

    @Override
    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
        enforcer.setDefaultDataStore(workspace, store);
    }

    // point queries, apply retry if

    @Override
    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return retryOnNull(
                () -> super.getStore(id, clazz), //
                () -> "getStore(%s, %s)".formatted(id, clazz.getSimpleName()));
    }

    @Override
    public <T extends StoreInfo> T getStoreByName(WorkspaceInfo workspace, String name, Class<T> clazz) {
        return retryOnNull(
                () -> super.getStoreByName(workspace, name, clazz), //
                () -> "getStoreByName(%s, %s, %s)".formatted(nameof(workspace), name, clazz.getSimpleName()));
    }

    @Override
    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return retryOnNull(
                () -> super.getResource(id, clazz), //
                () -> "getResource(%s, %s)".formatted(id, clazz.getSimpleName()));
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(NamespaceInfo namespace, String name, Class<T> clazz) {
        return retryOnNull(
                () -> super.getResourceByName(namespace, name, clazz), //
                () -> "getResourceByName(%s, %s, %s)".formatted(nameof(namespace), name, clazz.getSimpleName()));
    }

    @Override
    public <T extends ResourceInfo> T getResourceByStore(StoreInfo store, String name, Class<T> clazz) {
        return retryOnNull(
                () -> super.getResourceByStore(store, name, clazz), //
                () -> "getResourceByStore(%s, %s, %s)".formatted(nameof(store), name, clazz.getSimpleName()));
    }

    @Override
    public LayerInfo getLayer(String id) {
        return retryOnNull(
                () -> super.getLayer(id), //
                () -> "getLayer(%s)".formatted(id));
    }

    @Override
    public LayerInfo getLayerByName(String name) {
        return retryOnNull(
                () -> super.getLayerByName(name), //
                () -> "getLayerByName(%s)".formatted(name));
    }

    @Override
    public LayerGroupInfo getLayerGroup(String id) {
        return retryOnNull(
                () -> super.getLayerGroup(id), //
                () -> "getLayerGroup(%s)".formatted(id));
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {
        return retryOnNull(
                () -> super.getLayerGroupByName(name), //
                () -> "getLayerGroupByName(%s)".formatted(name));
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        return retryOnNull(
                () -> super.getLayerGroupByName(workspace, name), //
                () -> "getLayerGroupByName(%s,%s)".formatted(nameof(workspace), name));
    }

    @Override
    public NamespaceInfo getNamespace(String id) {
        return retryOnNull(
                () -> super.getNamespace(id), //
                () -> "getNamespace(%s)".formatted(id));
    }

    @Override
    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        // hack, CatalogImpl.getResourceByName(String ns, String name, Class<T> clazz)
        // wil try uri and prefix, if it looks like a uri don't bother retrying
        boolean maybeUri = prefix != null && prefix.indexOf(':') > -1;
        if (maybeUri) {
            return super.getNamespace(prefix);
        }
        return retryOnNull(
                () -> super.getNamespaceByPrefix(prefix), //
                () -> "getNamespaceByPrefix(%s)".formatted(prefix));
    }

    @Override
    public NamespaceInfo getNamespaceByURI(String uri) {
        return retryOnNull(
                () -> super.getNamespaceByURI(uri), //
                () -> "getNamespaceByURI(%s)".formatted(uri));
    }

    @Override
    public WorkspaceInfo getWorkspace(String id) {
        return retryOnNull(
                () -> super.getWorkspace(id), //
                () -> "getWorkspace(%s)".formatted(id));
    }

    @Override
    public WorkspaceInfo getWorkspaceByName(String name) {
        return retryOnNull(
                () -> super.getWorkspaceByName(name), //
                () -> "getWorkspaceByName(%s)".formatted(name));
    }

    @Override
    public StyleInfo getStyle(String id) {
        return retryOnNull(
                () -> super.getStyle(id), //
                () -> "getStyle(%s)".formatted(id));
    }

    @Override
    public StyleInfo getStyleByName(String name) {
        return retryOnNull(
                () -> super.getStyleByName(name), //
                () -> "getStyleByName(%s)".formatted(name));
    }

    @Override
    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        return retryOnNull(
                () -> super.getStyleByName(workspace, name), //
                () -> "getStyleByName(%s, %s)".formatted(nameof(workspace), name));
    }

    private <T> T retryOnNull(Supplier<T> supplier, Supplier<String> op) {
        return retry(supplier, Objects::nonNull, op);
    }

    private <T> T retry(Supplier<T> supplier, Predicate<T> predicate, Supplier<String> op) {
        T ret = supplier.get();
        if (predicate.test(ret)) {
            return ret;
        }

        // Retry logic when catalog has pending operations.
        //
        // Problem: Distributed catalog events may arrive out of order. An add event for a Layer
        // might arrive before its referenced Resource. The add is deferred until the Resource
        // arrives. Meanwhile, a client query for the Layer returns null even though it will
        // exist momentarily. Retrying gives pending operations a chance to resolve.
        //
        // Retry strategy:
        //
        // 1. Web requests (REST API, OWS services):
        //    - Retry when catalog has pending operations (!enforcer.isConverged())
        //    - During retry, forceResolve() attempts to resolve pending operations
        //    - If no pending operations exist, retrying is pointless (object genuinely doesn't exist)
        //
        // 2. Internal operations (catalog loading at startup, event processing):
        //    - Never retry (filtered by isWebRequest())
        //    - Would block initialization and event processing
        //
        // Note: The previous implementation (commit b60c8a2b9ec) had REST requests retry even
        // when converged, but this was wasteful - if there are no pending operations, retrying
        // can't find anything new.
        if (isWebRequest() && !enforcer.isConverged()) {
            return doRetry(supplier, predicate, op, ret);
        }
        return ret;
    }

    private <T> T doRetry(Supplier<T> supplier, Predicate<T> predicate, Supplier<String> op, T ret) {
        // poor man's Retry implementation
        final int maxAttempts = retryAttemptMillis.length;
        final String opDesc = op.get();
        log.debug("{} not found, retrying up to {} times", opDesc, maxAttempts);
        for (int i = 0; i < maxAttempts; i++) {
            int waitMillis = retryAttemptMillis[i];
            waitBeforeRetry(waitMillis);
            enforcer.forceResolve();

            int attempt = i + 1;
            ret = supplier.get();
            if (predicate.test(ret)) {
                log.debug("retry #{} after {}ms found, call: {}", attempt, waitMillis, opDesc);
                return ret;
            } else {
                log.debug("retry #{} after {}ms not found, call: {}", attempt, waitMillis, opDesc);
            }
        }
        log.debug("failing out after retry #{}, call: {}", maxAttempts, opDesc);
        return ret;
    }

    private boolean isWebRequest() {
        return null != RequestContextHolder.getRequestAttributes();
    }

    @SneakyThrows(InterruptedException.class)
    private void waitBeforeRetry(int waitMillis) {
        TimeUnit.MILLISECONDS.sleep(waitMillis);
    }

    private String nameof(CatalogInfo info) {
        if (null == info) {
            return null;
        } else if (info instanceof StoreInfo s) {
            return "%s:%s".formatted(s.getWorkspace().getName(), s.getName());
        } else if (info instanceof PublishedInfo p) {
            return p.prefixedName();
        }
        return (String) OwsUtils.get(info, "name");
    }
}
