/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.backend.pgconfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.geoserver.gwc.GWC;
import org.jspecify.annotations.Nullable;

/**
 * Catalog listener that propagates name changes affecting a tile-layer's prefixed name to the GeoWebCache storage
 * broker, so the file blob store directory and disk-quota tables follow.
 *
 * <p>Pgconfig derives tile-layer names from the underlying {@code PublishedInfo} on every lookup, so by the time
 * upstream's {@link org.geoserver.gwc.layer.CatalogLayerEventListener CatalogLayerEventListener} runs in post-modify,
 * the SQL trigger has already refreshed {@code publishedinfos_mat} and a lookup by old prefixed name returns empty.
 * Upstream catches the resulting {@code IllegalArgumentException} and silently skips the rename, leaving stale
 * directories under {@code /geowebcache_data/} and stale rows in {@code pgconfig.tileset}.
 *
 * <p>This listener bridges that gap: in {@link #handleModifyEvent(CatalogModifyEvent) pre-modify} it captures
 * {@link TileLayerRename (old, new)} prefixed-name pairs while the OLD names are still queryable, and in
 * {@link #handlePostModifyEvent(CatalogPostModifyEvent) post-modify} it replays them through
 * {@link GWC#layerRenamed(String, String)} - which calls {@code storageBroker.rename(...)} directly without going
 * through {@code TileLayerDispatcher.rename}, so the broken old-name lookup is bypassed entirely.
 *
 * <p>Handled cases:
 *
 * <ul>
 *   <li>{@link WorkspaceInfo} {@code name} change - all layers and layer-groups in the workspace.
 *   <li>{@link ResourceInfo} {@code name} or {@code namespace} change - the single layer.
 *   <li>{@link LayerGroupInfo} {@code name} change - the single layer-group.
 *   <li>{@link StoreInfo} {@code workspace} change - all the store's layers, whose namespaces follow the new workspace.
 * </ul>
 */
@Slf4j(topic = "org.geoserver.cloud.gwc.backend.pgconfig")
public class PgconfigGwcCatalogRenameListener implements CatalogListener {

    private record TileLayerRename(@Nullable String publishedId, String oldPrefixed, String newPrefixed) {}

    private static final ThreadLocal<List<TileLayerRename>> PENDING_RENAMES = new ThreadLocal<>();

    @NonNull
    private final Catalog catalog;

    @NonNull
    private final Consumer<TileLayerEvent> eventPublisher;

    public PgconfigGwcCatalogRenameListener(
            @NonNull Catalog catalog, @NonNull Consumer<TileLayerEvent> eventPublisher) {
        this.catalog = catalog;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    void register() {
        catalog.addListener(this);
    }

    @PreDestroy
    void unregister() {
        catalog.removeListener(this);
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        // no-op, only renames matter
    }

    /**
     * Publishes the deleted {@link TileLayerEvent} when a layer or layer-group is removed.
     *
     * <p>On pgconfig the tile layer is stored on the same row as its {@code PublishedInfo}: by the time
     * {@code GWC#removeTileLayers} runs, the row is gone, the tile-layer lookup returns empty and
     * {@code GeoServerTileLayerConfiguration} publishes no deleted event. Publishing it here, from the removed object's
     * in-memory state, evicts the name from every node's tile-layer cache. Blob and quota removal on tile-layer delete
     * is handled by {@code PgconfigTileLayerCatalog}.
     */
    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        if (event.getSource() instanceof PublishedInfo published) {
            eventPublisher.accept(TileLayerEvent.deleted(this, published.getId(), published.prefixedName()));
        }
    }

    @Override
    public void reloaded() {
        // no-op
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        List<TileLayerRename> pairs = collectRenames(event);
        if (!pairs.isEmpty()) {
            PENDING_RENAMES.set(pairs);
        }
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        List<TileLayerRename> pairs = PENDING_RENAMES.get();
        if (pairs == null) {
            return;
        }
        try {
            replayRenames(pairs);
        } finally {
            PENDING_RENAMES.remove();
        }
    }

    private List<TileLayerRename> collectRenames(CatalogModifyEvent event) {
        CatalogInfo source = event.getSource();
        List<String> changedProperties = event.getPropertyNames();
        if (source instanceof WorkspaceInfo && changedProperties.contains("name")) {
            return collectWorkspaceRenames(event);
        }
        if (source instanceof ResourceInfo resource
                && (changedProperties.contains("name") || changedProperties.contains("namespace"))) {
            return collectResourceRename(resource, event);
        }
        if (source instanceof LayerGroupInfo group && changedProperties.contains("name")) {
            return collectLayerGroupRename(group, event);
        }
        if (source instanceof StoreInfo store && changedProperties.contains("workspace")) {
            return collectStoreMove(store, event);
        }
        return List.of();
    }

    /**
     * A store moved to another workspace renames all its layers: the resources' namespaces follow the new workspace
     * (see {@code CatalogPlugin#save(StoreInfo)}), but the modify events fired for those resources already expose the
     * new namespace as both the old and the new value, leaving no usable old name. The store's own modify event does
     * provide the old workspace: collect the renames here, while the resources still expose the old namespace.
     */
    private List<TileLayerRename> collectStoreMove(StoreInfo store, CatalogModifyEvent event) {
        int workspaceIndex = event.getPropertyNames().indexOf("workspace");
        WorkspaceInfo oldWorkspace = (WorkspaceInfo) event.getOldValues().get(workspaceIndex);
        WorkspaceInfo newWorkspace = (WorkspaceInfo) event.getNewValues().get(workspaceIndex);
        if (oldWorkspace == null
                || newWorkspace == null
                || oldWorkspace.getName().equals(newWorkspace.getName())) {
            return List.of();
        }
        String newPrefix = newWorkspace.getName();
        List<TileLayerRename> pairs = new ArrayList<>();
        for (ResourceInfo resource : catalog.getResourcesByStore(store, ResourceInfo.class)) {
            NamespaceInfo oldNamespace = resource.getNamespace();
            if (oldNamespace == null) {
                continue;
            }
            String oldPrefixed = prefixed(oldNamespace.getPrefix(), resource.getName());
            String newPrefixed = prefixed(newPrefix, resource.getName());
            if (oldPrefixed.equals(newPrefixed)) {
                continue;
            }
            String layerId = catalog.getLayers(resource).stream()
                    .map(LayerInfo::getId)
                    .findFirst()
                    .orElse(null);
            pairs.add(new TileLayerRename(layerId, oldPrefixed, newPrefixed));
        }
        return pairs;
    }

    private List<TileLayerRename> collectWorkspaceRenames(CatalogModifyEvent event) {
        String oldWsName = stringPropertyOldValue(event, "name");
        String newWsName = stringPropertyNewValue(event, "name");
        if (oldWsName == null || newWsName == null || oldWsName.equals(newWsName)) {
            return List.of();
        }
        List<TileLayerRename> pairs = new ArrayList<>();
        addLayerRenamesForWorkspace(oldWsName, newWsName, pairs);
        addLayerGroupRenamesForWorkspace(oldWsName, newWsName, pairs);
        return pairs;
    }

    private void addLayerRenamesForWorkspace(String oldWsName, String newWsName, List<TileLayerRename> pairs) {
        try (CloseableIterator<org.geoserver.catalog.LayerInfo> layers =
                catalog.list(LayerInfo.class, Predicates.equal("resource.store.workspace.name", oldWsName))) {
            while (layers.hasNext()) {
                LayerInfo layer = layers.next();
                String localName = layer.getName();
                pairs.add(new TileLayerRename(
                        layer.getId(), prefixed(oldWsName, localName), prefixed(newWsName, localName)));
            }
        }
    }

    private void addLayerGroupRenamesForWorkspace(String oldWsName, String newWsName, List<TileLayerRename> pairs) {
        try (CloseableIterator<LayerGroupInfo> groups =
                catalog.list(LayerGroupInfo.class, Predicates.equal("workspace.name", oldWsName))) {
            while (groups.hasNext()) {
                LayerGroupInfo group = groups.next();
                String localName = group.getName();
                pairs.add(new TileLayerRename(
                        group.getId(), prefixed(oldWsName, localName), prefixed(newWsName, localName)));
            }
        }
    }

    private List<TileLayerRename> collectResourceRename(ResourceInfo resource, CatalogModifyEvent event) {
        List<String> changedProperties = event.getPropertyNames();
        int nameIndex = changedProperties.indexOf("name");
        int namespaceIndex = changedProperties.indexOf("namespace");

        NamespaceInfo oldNamespace = namespaceIndex > -1
                ? (NamespaceInfo) event.getOldValues().get(namespaceIndex)
                : resource.getNamespace();
        NamespaceInfo newNamespace = namespaceIndex > -1
                ? (NamespaceInfo) event.getNewValues().get(namespaceIndex)
                : resource.getNamespace();
        if (oldNamespace == null || newNamespace == null) {
            return List.of();
        }

        String oldLocalName = nameIndex > -1 ? (String) event.getOldValues().get(nameIndex) : resource.getName();
        String newLocalName = nameIndex > -1 ? (String) event.getNewValues().get(nameIndex) : resource.getName();

        String oldPrefixed = prefixed(oldNamespace.getPrefix(), oldLocalName);
        String newPrefixed = prefixed(newNamespace.getPrefix(), newLocalName);
        if (oldPrefixed.equals(newPrefixed)) {
            return List.of();
        }
        String layerId = catalog.getLayers(resource).stream()
                .map(LayerInfo::getId)
                .findFirst()
                .orElse(null);
        return List.of(new TileLayerRename(layerId, oldPrefixed, newPrefixed));
    }

    private List<TileLayerRename> collectLayerGroupRename(LayerGroupInfo group, CatalogModifyEvent event) {
        String oldName = stringPropertyOldValue(event, "name");
        String newName = stringPropertyNewValue(event, "name");
        if (oldName == null || newName == null || oldName.equals(newName)) {
            return List.of();
        }
        WorkspaceInfo workspace = group.getWorkspace();
        String prefix = workspace == null ? null : workspace.getName();
        return List.of(new TileLayerRename(group.getId(), prefixed(prefix, oldName), prefixed(prefix, newName)));
    }

    private void replayRenames(List<TileLayerRename> pairs) {
        publishTileLayerEvents(pairs);
        GWC mediator;
        try {
            mediator = GWC.get();
        } catch (NullPointerException npe) {
            log.debug("Skipping {} tile layer rename(s); GWC singleton not available", pairs.size());
            return;
        }
        if (mediator == null) {
            log.debug("Skipping {} tile layer rename(s); GWC singleton not available", pairs.size());
            return;
        }
        for (TileLayerRename pair : pairs) {
            try {
                mediator.layerRenamed(pair.oldPrefixed(), pair.newPrefixed());
            } catch (RuntimeException e) {
                log.warn("Failed to rename tile layer cache '{}' -> '{}'", pair.oldPrefixed(), pair.newPrefixed(), e);
            }
        }
    }

    /**
     * Notifies every service of the rename through {@link TileLayerEvent}s, keyed by the old prefixed name.
     *
     * <p>Upstream {@code CatalogLayerEventListener} only publishes its rename when the old prefixed name still
     * resolves, which on pgconfig depends on the caching tile-layer repository holding a warm entry at the service
     * handling the rename. These events make cache eviction independent of that: each node's
     * {@link CachingTileLayerInfoRepository} drops the old-name entry and the name listing memo.
     */
    private void publishTileLayerEvents(List<TileLayerRename> pairs) {
        for (TileLayerRename pair : pairs) {
            if (pair.publishedId() != null) {
                eventPublisher.accept(
                        TileLayerEvent.modified(this, pair.publishedId(), pair.newPrefixed(), pair.oldPrefixed()));
            }
        }
    }

    private static String prefixed(String workspaceOrPrefix, String localName) {
        return workspaceOrPrefix == null ? localName : workspaceOrPrefix + ":" + localName;
    }

    private static String stringPropertyOldValue(CatalogModifyEvent event, String property) {
        int index = event.getPropertyNames().indexOf(property);
        return index > -1 ? (String) event.getOldValues().get(index) : null;
    }

    private static String stringPropertyNewValue(CatalogModifyEvent event, String property) {
        int index = event.getPropertyNames().indexOf(property);
        return index > -1 ? (String) event.getNewValues().get(index) : null;
    }
}
