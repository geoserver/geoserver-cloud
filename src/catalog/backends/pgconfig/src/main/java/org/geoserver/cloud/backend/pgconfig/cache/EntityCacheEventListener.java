/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import static org.geoserver.cloud.event.info.ConfigInfoType.CATALOG;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYER;
import static org.geoserver.cloud.event.info.ConfigInfoType.NAMESPACE;
import static org.geoserver.cloud.event.info.ConfigInfoType.SERVICE;
import static org.geoserver.cloud.event.info.ConfigInfoType.SETTINGS;
import static org.geoserver.cloud.event.info.ConfigInfoType.WORKSPACE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.cloud.event.UpdateSequenceEvent;
import org.geoserver.cloud.event.catalog.CatalogInfoAdded;
import org.geoserver.cloud.event.catalog.CatalogInfoModified;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.cloud.event.catalog.DefaultDataStoreSet;
import org.geoserver.cloud.event.catalog.DefaultNamespaceSet;
import org.geoserver.cloud.event.catalog.DefaultWorkspaceSet;
import org.geoserver.cloud.event.config.LoggingInfoModified;
import org.geoserver.cloud.event.config.LoggingInfoSet;
import org.geoserver.cloud.event.config.ServiceAdded;
import org.geoserver.cloud.event.config.ServiceModified;
import org.geoserver.cloud.event.config.ServiceRemoved;
import org.geoserver.cloud.event.config.SettingsAdded;
import org.geoserver.cloud.event.config.SettingsModified;
import org.geoserver.cloud.event.config.SettingsRemoved;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Evicts the cache entries made stale by the changes of other service instances, received as remote events.
 *
 * <p>The repositories evict the keys of the writes of this instance: local events are ignored, except the removal of a
 * workspace. The database deletes the settings and the services of a removed workspace with no write of the
 * configuration repository, and the listener evicts them for the removals of this instance too. The keys come from
 * {@link CatalogInfoKeys} and {@link ConfigInfoKeys}, as for the writes of the repositories. The listener evicts a key
 * from all its caches: the type of a key decides which cache holds it, and the eviction of a key from another cache
 * removes nothing.
 *
 * <p>The cached objects are no source of the state before a change: a lookup on this instance may have cached an object
 * as changed before the event arrives. When an event does not tell an old name or URI, the listener evicts a wider set
 * of keys.
 *
 * <p>If the evictions of an event fail, the listener logs a warning and clears the caches. The event still reaches the
 * other listeners, and an empty cache holds nothing stale.
 *
 * <p>The listener runs after the listeners of the highest precedence. One of them cleans the resource pool up after a
 * remote removal, and reads the removed object: the cache may still hold it. The cleanup of the resource pool after a
 * remote change runs after this listener and after {@link RequestCacheEventListener}.
 */
@Slf4j(topic = "org.geoserver.cloud.backend.pgconfig.cache")
public class EntityCacheEventListener {

    /**
     * Right after the listeners of the highest precedence, and before the other listeners: they may read the changes.
     */
    static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    private final List<EntityCache> caches;

    public EntityCacheEventListener(@NonNull List<EntityCache> caches) {
        this.caches = List.copyOf(caches);
    }

    /** Evicts the misses stored for the id and the names of the added object, and for the URI of a namespace. */
    @EventListener(CatalogInfoAdded.class)
    @Order(ORDER)
    public void onCatalogInfoAdded(CatalogInfoAdded event) {
        evictIfRemote(event, () -> evictAdded(event));
    }

    /**
     * Evicts the keys of the object under its old and new names, the lookups by URI of a namespace with a new URI, and
     * the names of the objects named after it.
     */
    @EventListener(CatalogInfoModified.class)
    @Order(ORDER)
    public void onCatalogInfoModified(CatalogInfoModified event) {
        // the default workspace, namespace and data store are changes of the catalog, with listeners of their own
        if (event.getObjectType() != CATALOG) {
            evictIfRemote(event, () -> evictModified(event));
        }
    }

    /**
     * Evicts the keys of the removed object. Other keys may hold its id, such as the key of a lookup by URI: a lookup
     * through them finds the object gone, and runs its query.
     *
     * <p>The removal of a workspace also evicts the settings and the services, local or remote: the database deletes
     * those of the workspace, and the event does not tell their ids. The catalog repositories evict the catalog keys of
     * a local removal.
     */
    @EventListener(CatalogInfoRemoved.class)
    @Order(ORDER)
    public void onCatalogInfoRemoved(CatalogInfoRemoved event) {
        evictIfRemote(event, () -> evict(keysOf(event)));
        if (event.getObjectType() == WORKSPACE) {
            evictOrClear(event, () -> evictWhere(ConfigInfoKeys.settingsAndServices()));
        }
    }

    @EventListener(DefaultWorkspaceSet.class)
    @Order(ORDER)
    public void onDefaultWorkspaceSet(DefaultWorkspaceSet event) {
        evictIfRemote(event, () -> evict(List.of(CatalogInfoKeys.defaultWorkspace())));
    }

    @EventListener(DefaultNamespaceSet.class)
    @Order(ORDER)
    public void onDefaultNamespaceSet(DefaultNamespaceSet event) {
        evictIfRemote(event, () -> evict(List.of(CatalogInfoKeys.defaultNamespace())));
    }

    @EventListener(DefaultDataStoreSet.class)
    @Order(ORDER)
    public void onDefaultDataStoreSet(DefaultDataStoreSet event) {
        evictIfRemote(event, () -> evictDefaultDataStore(event.getWorkspaceId()));
    }

    /**
     * Evicts the global settings upon the changes of other service instances, to the catalog as to the configuration. A
     * change increments the update sequence, held by the global settings.
     */
    @EventListener(UpdateSequenceEvent.class)
    @Order(ORDER)
    public void onUpdateSequence(UpdateSequenceEvent event) {
        evictIfRemote(event, () -> evict(List.of(ConfigInfoKeys.global())));
    }

    @EventListener({LoggingInfoSet.class, LoggingInfoModified.class})
    @Order(ORDER)
    public void onLoggingChange(InfoEvent event) {
        evictIfRemote(event, () -> evict(List.of(ConfigInfoKeys.logging())));
    }

    @EventListener(SettingsAdded.class)
    @Order(ORDER)
    public void onSettingsAdded(SettingsAdded event) {
        evictIfRemote(event, () -> evictAddedSettings(event));
    }

    @EventListener(SettingsModified.class)
    @Order(ORDER)
    public void onSettingsModified(SettingsModified event) {
        evictIfRemote(event, () -> evictModifiedSettings(event));
    }

    @EventListener(SettingsRemoved.class)
    @Order(ORDER)
    public void onSettingsRemoved(SettingsRemoved event) {
        evictIfRemote(event, () -> evictSettings(event.getObjectId(), event.getWorkspaceId()));
    }

    /**
     * Evicts the service and the lookups of services. The key of a lookup by class holds the class asked by the caller,
     * such as {@code WMSInfo}, and an event tells the class of the implementation at most. A modification does not tell
     * the old name either.
     */
    @EventListener({ServiceAdded.class, ServiceModified.class, ServiceRemoved.class})
    @Order(ORDER)
    public void onServiceChange(InfoEvent event) {
        evictIfRemote(event, () -> evictService(event.getObjectId()));
    }

    /** Runs the evictions of a remote event, and ignores a local one. */
    private void evictIfRemote(GeoServerEvent event, Runnable evictions) {
        if (event.isRemote()) {
            evictOrClear(event, evictions);
        }
    }

    /** Runs the evictions of an event, and clears the caches if they fail. */
    private void evictOrClear(GeoServerEvent event, Runnable evictions) {
        try {
            evictions.run();
        } catch (RuntimeException e) {
            List<String> names = caches.stream().map(EntityCache::name).toList();
            log.warn("Clearing the caches {}, the evictions upon {} failed", names, event.toShortString(), e);
            caches.forEach(EntityCache::clear);
        }
    }

    private void evictAdded(CatalogInfoAdded event) {
        List<CacheKey> keys = new ArrayList<>(keysOf(event));
        if (event.getObject() instanceof NamespaceInfo namespace && null != namespace.getURI()) {
            keys.add(CatalogInfoKeys.namespaceByUri(namespace.getURI()));
        }
        evict(keys);
    }

    private void evictModified(CatalogInfoModified event) {
        evictOldName(event);
        evictChangedUri(event);
        evict(keysOf(event));
        CatalogInfoKeys.dependentKeys(event.getObjectType(), event.getPatch()).ifPresent(this::evictWhere);
    }

    /**
     * Evicts the names held by a renamed or moved object before the change.
     *
     * <p>A rename keeps the workspace or namespace of the object: its old name is the old local name with the prefix of
     * the new name. A move to another workspace or namespace changes the prefix, and the event does not tell the old
     * one: the names of the type with the local name of the object are evicted, whatever their prefix. A layer takes
     * the name of its resource, and the event does not tell the old resource: a layer moved to another resource evicts
     * the names of the layers.
     */
    private void evictOldName(CatalogInfoModified event) {
        ConfigInfoType type = event.getObjectType();
        Patch patch = event.getPatch();
        if (type == LAYER && patch.get("resource").isPresent()) {
            evictWhere(CatalogInfoKeys.namesOf(LAYER));
        } else if (moves(type, patch)) {
            evictWhere(CatalogInfoKeys.namesWithLocalName(type, oldLocalName(event)));
        } else if (renames(type, patch)) {
            Optional<String> prefix = InfoEvent.prefix(event.getObjectName());
            String oldName = InfoEvent.prefixedName(prefix, event.getOldName());
            evict(CatalogInfoKeys.nameKeys(type, oldName));
        }
    }

    /** Whether a change moves an object to another workspace or namespace. */
    private static boolean moves(ConfigInfoType type, Patch patch) {
        return switch (type) {
            case DATASTORE, COVERAGESTORE, WMSSTORE, WMTSSTORE, STYLE, LAYERGROUP ->
                patch.get("workspace").isPresent();
            case FEATURETYPE, COVERAGE, WMSLAYER, WMTSLAYER ->
                patch.get("namespace").isPresent();
            default -> false;
        };
    }

    /** Whether a change renames an object. The prefix of a namespace is its name. */
    private static boolean renames(ConfigInfoType type, Patch patch) {
        boolean prefixChanges = type == NAMESPACE && patch.get("prefix").isPresent();
        return prefixChanges || patch.get("name").isPresent();
    }

    /** The local name of a renamed or moved object before the change. The event holds it if the name changes. */
    private static String oldLocalName(CatalogInfoModified event) {
        if (renames(event.getObjectType(), event.getPatch())) {
            return event.getOldName();
        }
        return InfoEvent.localName(event.getObjectName());
    }

    /**
     * Evicts the lookups by URI of a namespace with a new URI. The key of the new URI may hold a miss or another
     * namespace, and the key of the old URI this namespace: a lookup key holds no name to check. The event does not
     * tell the old URI: the lookups by URI are evicted.
     */
    private void evictChangedUri(CatalogInfoModified event) {
        if (event.getObjectType() == NAMESPACE && changesUri(event.getPatch())) {
            evictWhere(CatalogInfoKeys.namespaceUriLookups());
        }
    }

    // A ModificationProxy names the property of setURI as uRI: it lowers the first letter of URI
    private static boolean changesUri(Patch patch) {
        return patch.getPropertyNames().stream().anyMatch("uri"::equalsIgnoreCase);
    }

    private void evictDefaultDataStore(String workspaceId) {
        evict(List.of(CatalogInfoKeys.defaultDataStore(workspaceId)));
    }

    private void evictAddedSettings(SettingsAdded event) {
        WorkspaceInfo workspace = event.getObject().getWorkspace();
        evictSettings(event.getObjectId(), workspace.getId());
    }

    /**
     * Evicts the settings and the lookup of their workspace. Settings moved to another workspace evict the lookups of
     * settings: the event tells the new workspace only. GeoServer offers no such move.
     */
    private void evictModifiedSettings(SettingsModified event) {
        evictSettings(event.getObjectId(), event.getWorkspaceId());
        if (event.getPatch().get("workspace").isPresent()) {
            evictWhere(CatalogInfoKeys.lookupsOf(SETTINGS));
        }
    }

    private void evictSettings(String settingsId, String workspaceId) {
        evict(List.of(ConfigInfoKeys.settings(settingsId), ConfigInfoKeys.settingsByWorkspace(workspaceId)));
    }

    private void evictService(String serviceId) {
        evict(List.of(ConfigInfoKeys.service(serviceId)));
        evictWhere(CatalogInfoKeys.lookupsOf(SERVICE));
    }

    private static List<CacheKey> keysOf(InfoEvent event) {
        return CatalogInfoKeys.keysOf(event.getObjectType(), event.getObjectId(), event.getObjectName());
    }

    private void evict(List<CacheKey> keys) {
        CacheKey[] evicted = keys.toArray(CacheKey[]::new);
        for (EntityCache cache : caches) {
            cache.evict(evicted);
        }
    }

    private void evictWhere(Predicate<CacheKey> which) {
        for (EntityCache cache : caches) {
            cache.evictWhere(which);
        }
    }
}
