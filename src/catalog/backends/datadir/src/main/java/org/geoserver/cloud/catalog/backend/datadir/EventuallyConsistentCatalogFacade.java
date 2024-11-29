/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
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
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.forwarding.ForwardingExtendedCatalogFacade;
import org.geoserver.ows.util.OwsUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 *
 *
 * <ul>
 *   <li>All {@code add(@NonNull )} methods check if the {@link CatalogInfo} being added have
 *       unresolved ({@link ResolvingProxy}) references
 *   <li>If so, the object is put in a pending list and not added
 *   <li>Conversely, if during {@code add(@NonNull )}, there's a pending add waiting for this new
 *       object, the {@code add(@NonNull )} proceeds and then the pending object is added
 * </ul>
 *
 * @since 1.9
 * @see EventualConsistencyEnforcer
 * @see RemoteEventDataDirectoryProcessor
 */
@Slf4j(topic = "org.geoserver.cloud.catalog.backend.datadir")
public class EventuallyConsistentCatalogFacade extends ForwardingExtendedCatalogFacade {

    private final EventualConsistencyEnforcer enforcer;

    /**
     * number of retry attemps and milliseconds to wait between retries
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

    ///////// point queries, apply retry if

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
        if (maybeUri) return super.getNamespace(prefix);
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
        if (predicate.test(ret)) return ret;

        // do retry if it's a REST API request or there're pending updates
        if (isWebRequest() && (isRestRequest() || !enforcer.isConverged())) {
            return doRetry(supplier, predicate, op, ret);
        }
        return ret;
    }

    private boolean isRestRequest() {
        RequestAttributes atts = RequestContextHolder.getRequestAttributes();
        if (atts instanceof ServletRequestAttributes webreq) {
            String uri = webreq.getRequest().getRequestURI();
            return uri.contains("/rest/");
        }
        return false;
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
                log.debug("retry #{} after {}ms found, call: {}", attempt, opDesc);
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
        if (null == info) return null;
        if (info instanceof StoreInfo s)
            return "%s:%s".formatted(s.getWorkspace().getName(), s.getName());
        if (info instanceof PublishedInfo p) return p.prefixedName();
        return (String) OwsUtils.get(info, "name");
    }
}
