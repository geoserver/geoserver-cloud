/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.backend.pgconfig;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.gwc.GWC;

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
 * {@link NamePair (old, new)} prefixed-name pairs while the OLD names are still queryable, and in
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
 * </ul>
 */
@Slf4j(topic = "org.geoserver.cloud.gwc.backend.pgconfig")
public class PgconfigGwcCatalogRenameListener implements CatalogListener {

    private record NamePair(String oldPrefixed, String newPrefixed) {}

    private static final ThreadLocal<List<NamePair>> PENDING_RENAMES = new ThreadLocal<>();

    @NonNull
    private final Catalog catalog;

    public PgconfigGwcCatalogRenameListener(@NonNull Catalog catalog) {
        this.catalog = catalog;
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

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        // no-op, blob/quota removal on tile-layer delete is handled by PgconfigTileLayerCatalog
    }

    @Override
    public void reloaded() {
        // no-op
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        List<NamePair> pairs = collectRenames(event);
        if (!pairs.isEmpty()) {
            PENDING_RENAMES.set(pairs);
        }
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        List<NamePair> pairs = PENDING_RENAMES.get();
        if (pairs == null) {
            return;
        }
        try {
            replayRenames(pairs);
        } finally {
            PENDING_RENAMES.remove();
        }
    }

    private List<NamePair> collectRenames(CatalogModifyEvent event) {
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
        return List.of();
    }

    private List<NamePair> collectWorkspaceRenames(CatalogModifyEvent event) {
        String oldWsName = stringPropertyOldValue(event, "name");
        String newWsName = stringPropertyNewValue(event, "name");
        if (oldWsName == null || newWsName == null || oldWsName.equals(newWsName)) {
            return List.of();
        }
        List<NamePair> pairs = new ArrayList<>();
        addLayerRenamesForWorkspace(oldWsName, newWsName, pairs);
        addLayerGroupRenamesForWorkspace(oldWsName, newWsName, pairs);
        return pairs;
    }

    private void addLayerRenamesForWorkspace(String oldWsName, String newWsName, List<NamePair> pairs) {
        try (CloseableIterator<org.geoserver.catalog.LayerInfo> layers = catalog.list(
                org.geoserver.catalog.LayerInfo.class, Predicates.equal("resource.store.workspace.name", oldWsName))) {
            while (layers.hasNext()) {
                String localName = layers.next().getName();
                pairs.add(new NamePair(prefixed(oldWsName, localName), prefixed(newWsName, localName)));
            }
        }
    }

    private void addLayerGroupRenamesForWorkspace(String oldWsName, String newWsName, List<NamePair> pairs) {
        try (CloseableIterator<LayerGroupInfo> groups =
                catalog.list(LayerGroupInfo.class, Predicates.equal("workspace.name", oldWsName))) {
            while (groups.hasNext()) {
                String localName = groups.next().getName();
                pairs.add(new NamePair(prefixed(oldWsName, localName), prefixed(newWsName, localName)));
            }
        }
    }

    private List<NamePair> collectResourceRename(ResourceInfo resource, CatalogModifyEvent event) {
        List<String> changedProperties = event.getPropertyNames();
        int nameIndex = changedProperties.indexOf("name");
        int namespaceIndex = changedProperties.indexOf("namespace");

        NamespaceInfo currentNamespace = resource.getNamespace();
        NamespaceInfo oldNamespace =
                namespaceIndex > -1 ? (NamespaceInfo) event.getOldValues().get(namespaceIndex) : currentNamespace;
        if (oldNamespace == null || currentNamespace == null) {
            return List.of();
        }

        String oldLocalName = nameIndex > -1 ? (String) event.getOldValues().get(nameIndex) : resource.getName();
        String newLocalName = nameIndex > -1 ? (String) event.getNewValues().get(nameIndex) : resource.getName();

        String oldPrefixed = prefixed(oldNamespace.getPrefix(), oldLocalName);
        String newPrefixed = prefixed(currentNamespace.getPrefix(), newLocalName);
        if (oldPrefixed.equals(newPrefixed)) {
            return List.of();
        }
        return List.of(new NamePair(oldPrefixed, newPrefixed));
    }

    private List<NamePair> collectLayerGroupRename(LayerGroupInfo group, CatalogModifyEvent event) {
        String oldName = stringPropertyOldValue(event, "name");
        String newName = stringPropertyNewValue(event, "name");
        if (oldName == null || newName == null || oldName.equals(newName)) {
            return List.of();
        }
        WorkspaceInfo workspace = group.getWorkspace();
        String prefix = workspace == null ? null : workspace.getName();
        return List.of(new NamePair(prefixed(prefix, oldName), prefixed(prefix, newName)));
    }

    private void replayRenames(List<NamePair> pairs) {
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
        for (NamePair pair : pairs) {
            try {
                mediator.layerRenamed(pair.oldPrefixed(), pair.newPrefixed());
            } catch (RuntimeException e) {
                log.warn("Failed to rename tile layer cache '{}' -> '{}'", pair.oldPrefixed(), pair.newPrefixed(), e);
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
