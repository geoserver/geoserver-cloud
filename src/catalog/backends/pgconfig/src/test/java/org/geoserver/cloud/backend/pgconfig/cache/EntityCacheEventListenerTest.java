/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.geoserver.cloud.event.info.ConfigInfoType.DATASTORE;
import static org.geoserver.cloud.event.info.ConfigInfoType.FEATURETYPE;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYER;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYERGROUP;
import static org.geoserver.cloud.event.info.ConfigInfoType.STYLE;
import static org.geoserver.cloud.event.info.ConfigInfoType.WORKSPACE;

import ch.qos.logback.classic.Level;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup.Absent;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup.Hit;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup.Miss;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigObjectMapper;
import org.geoserver.cloud.backend.pgconfig.support.LogCapture;
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
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.LoggingInfoImpl;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class EntityCacheEventListenerTest {

    private static final long ONE_MIB = 1024 * 1024;
    private static final byte[] JSON = "{}".getBytes(UTF_8);
    private static final String CACHE_LOGGER = "org.geoserver.cloud.backend.pgconfig.cache";

    // the JSON of the objects as the repositories write it to the database, and the cache holds it
    private static final ObjectMapper INFO_MAPPER = PgconfigObjectMapper.newObjectMapper();

    private final EntityCache catalogCache = newCache("gs-catalog");
    private final EntityCache configCache = newCache("gs-config");
    private final EntityCacheEventListener listener = new EntityCacheEventListener(List.of(catalogCache, configCache));

    // typed by their interfaces, as the proxies of the changes are
    private final WorkspaceInfo topp = workspace("ws-1", "topp");
    private final WorkspaceInfo other = workspace("ws-2", "other");
    private final DataStoreInfo roads = store("store-1", "roads", topp);

    @Test
    void addedEvictsNameMiss() {
        CacheKey name = CacheKey.name(DATASTORE, "topp:roads");
        catalogCache.putMiss(name, catalogCache.epoch());

        listener.onCatalogInfoAdded(remote(CatalogInfoAdded.createLocal(1L, roads)));

        assertThat(catalogCache.lookup(name)).isInstanceOf(Absent.class);
    }

    @Test
    void addedEvictsIdMiss() {
        CacheKey entity = CacheKey.entity(DATASTORE, "store-1");
        catalogCache.putMiss(entity, catalogCache.epoch());

        listener.onCatalogInfoAdded(remote(CatalogInfoAdded.createLocal(1L, roads)));

        assertThat(catalogCache.lookup(entity)).isInstanceOf(Absent.class);
    }

    @Test
    void addedNamespaceEvictsUriMiss() {
        NamespaceInfo namespace = namespace("ns-1", "topp", "http://topp.example");
        CacheKey uri = CatalogInfoKeys.namespaceByUri("http://topp.example");
        catalogCache.putMiss(uri, catalogCache.epoch());

        listener.onCatalogInfoAdded(remote(CatalogInfoAdded.createLocal(1L, namespace)));

        assertThat(catalogCache.lookup(uri)).isInstanceOf(Absent.class);
    }

    // a lookup without workspace of topp:basemap remembered that no group without workspace had the name
    @Test
    void addedGlobalGroupEvictsMiss() {
        CacheKey miss = CatalogInfoKeys.globalNameMiss(LAYERGROUP, "topp:basemap");
        catalogCache.putMiss(miss, catalogCache.epoch());
        LayerGroupInfo global = group("group-1", "topp:basemap", null);

        listener.onCatalogInfoAdded(remote(CatalogInfoAdded.createLocal(1L, global)));

        assertThat(catalogCache.lookup(miss)).isInstanceOf(Absent.class);
    }

    // the event of the group basemap of topp has the name of a group without workspace named topp:basemap
    @Test
    void workspaceGroupChangeEvictsMiss() {
        CacheKey miss = CatalogInfoKeys.globalNameMiss(LAYERGROUP, "topp:basemap");
        catalogCache.putMiss(miss, catalogCache.epoch());
        LayerGroupInfo basemap = group("group-1", "basemap", topp);

        listener.onCatalogInfoModified(remote(modified(basemap, group -> group.setTitle("Base map"))));

        assertThat(catalogCache.lookup(miss)).isInstanceOf(Absent.class);
    }

    // the repositories evict the keys of the writes of this service instance
    @Test
    void localEventsIgnored() {
        CacheKey name = CacheKey.name(DATASTORE, "topp:roads");
        CacheKey entity = CacheKey.entity(DATASTORE, "store-1");
        catalogCache.putMiss(name, catalogCache.epoch());
        catalogCache.put(entity, JSON, catalogCache.epoch());
        catalogCache.putId(CatalogInfoKeys.defaultWorkspace(), "ws-2", catalogCache.epoch());
        configCache.put(ConfigInfoKeys.global(), JSON, configCache.epoch());
        configCache.put(ConfigInfoKeys.logging(), JSON, configCache.epoch());

        listener.onCatalogInfoAdded(CatalogInfoAdded.createLocal(1L, roads));
        listener.onCatalogInfoModified(modified(roads, store -> store.setName("rivers")));
        listener.onCatalogInfoRemoved(CatalogInfoRemoved.createLocal(1L, roads));
        listener.onDefaultWorkspaceSet(DefaultWorkspaceSet.createLocal(1L, topp));
        listener.onUpdateSequence(UpdateSequenceEvent.createLocal(2L));
        listener.onLoggingChange(LoggingInfoSet.createLocal(1L, new LoggingInfoImpl()));

        assertThat(catalogCache.lookup(name)).isInstanceOf(Miss.class);
        assertThat(catalogCache.lookup(entity)).isInstanceOf(Hit.class);
        assertThat(catalogCache.lookup(CatalogInfoKeys.defaultWorkspace())).isInstanceOf(Hit.class);
        assertThat(configCache.lookup(ConfigInfoKeys.global())).isInstanceOf(Hit.class);
        assertThat(configCache.lookup(ConfigInfoKeys.logging())).isInstanceOf(Hit.class);
    }

    // a rename keeps the workspace: the old name is exact, and a store of another workspace keeps the old local name
    @Test
    void modifiedEvictsOldAndNewName() {
        CacheKey oldName = CacheKey.name(DATASTORE, "topp:roads");
        CacheKey newName = CacheKey.name(DATASTORE, "topp:rivers");
        CacheKey sameLocalName = CacheKey.name(DATASTORE, "third:roads");
        catalogCache.putId(oldName, "store-1", catalogCache.epoch());
        catalogCache.putMiss(newName, catalogCache.epoch());
        catalogCache.putId(sameLocalName, "store-3", catalogCache.epoch());

        listener.onCatalogInfoModified(remote(modified(roads, store -> store.setName("rivers"))));

        assertThat(catalogCache.lookup(oldName)).isInstanceOf(Absent.class);
        assertThat(catalogCache.lookup(newName)).isInstanceOf(Absent.class);
        assertThat(catalogCache.lookup(sameLocalName)).isInstanceOf(Hit.class);
    }

    // the event does not tell the old workspace: the names with the local name are evicted, whatever their workspace
    @Test
    void moveEvictsSameLocalNames() {
        CacheKey oldName = CacheKey.name(DATASTORE, "topp:roads");
        CacheKey sameLocalName = CacheKey.name(DATASTORE, "third:roads");
        CacheKey otherName = CacheKey.name(DATASTORE, "topp:rivers");
        catalogCache.putId(oldName, "store-1", catalogCache.epoch());
        catalogCache.putId(sameLocalName, "store-3", catalogCache.epoch());
        catalogCache.putId(otherName, "store-4", catalogCache.epoch());

        listener.onCatalogInfoModified(remote(modified(roads, store -> store.setWorkspace(other))));

        assertThat(catalogCache.lookup(oldName)).isInstanceOf(Absent.class);
        assertThat(catalogCache.lookup(sameLocalName)).isInstanceOf(Absent.class);
        assertThat(catalogCache.lookup(otherName)).isInstanceOf(Hit.class);
    }

    // a lookup on this instance may cache the store in its new workspace before the event arrives
    @Test
    void moveAfterRefreshEvictsOldName() {
        putJson(other);
        putJson(store("store-1", "roads", other));
        CacheKey oldName = CacheKey.name(DATASTORE, "topp:roads");
        catalogCache.putId(oldName, "store-1", catalogCache.epoch());

        listener.onCatalogInfoModified(remote(modified(roads, store -> store.setWorkspace(other))));

        assertThat(catalogCache.lookup(oldName)).isInstanceOf(Absent.class);
    }

    // the layers take the names of their resources
    @Test
    void resourceMoveEvictsOldName() {
        NamespaceInfo otherNamespace = namespace("ns-2", "other", "http://other.example");
        FeatureTypeInfo featureType = featureType("ft-1", "roads", namespace("ns-1", "topp", "http://topp.example"));
        CacheKey oldName = CacheKey.name(FEATURETYPE, "topp:roads");
        CacheKey otherName = CacheKey.name(FEATURETYPE, "topp:lakes");
        CacheKey layerName = CacheKey.name(LAYER, "topp:roads");
        catalogCache.putId(oldName, "ft-1", catalogCache.epoch());
        catalogCache.putId(otherName, "ft-2", catalogCache.epoch());
        catalogCache.putId(layerName, "layer-1", catalogCache.epoch());

        listener.onCatalogInfoModified(remote(modified(featureType, moved -> moved.setNamespace(otherNamespace))));

        assertThat(catalogCache.lookup(oldName)).isInstanceOf(Absent.class);
        assertThat(catalogCache.lookup(otherName)).isInstanceOf(Hit.class);
        assertThat(catalogCache.lookup(layerName)).isInstanceOf(Absent.class);
    }

    // a style without a workspace has a bare name
    @Test
    void globalStyleMoveEvictsBareName() {
        StyleInfo line = style("style-1", "line");
        CacheKey oldName = CacheKey.name(STYLE, "line");
        CacheKey otherName = CacheKey.name(STYLE, "point");
        catalogCache.putId(oldName, "style-1", catalogCache.epoch());
        catalogCache.putId(otherName, "style-2", catalogCache.epoch());

        listener.onCatalogInfoModified(remote(modified(line, style -> style.setWorkspace(topp))));

        assertThat(catalogCache.lookup(oldName)).isInstanceOf(Absent.class);
        assertThat(catalogCache.lookup(otherName)).isInstanceOf(Hit.class);
    }

    // a lookup by URI holds no name to check: a stale id would be returned
    @Test
    void uriChangeEvictsUriLookups() {
        NamespaceInfo namespace = namespace("ns-1", "topp", "http://topp.example");
        CacheKey oldUri = CatalogInfoKeys.namespaceByUri("http://topp.example");
        CacheKey newUri = CatalogInfoKeys.namespaceByUri("http://topp.example/v2");
        catalogCache.putId(oldUri, "ns-1", catalogCache.epoch());
        catalogCache.putMiss(newUri, catalogCache.epoch());
        catalogCache.putId(CatalogInfoKeys.defaultNamespace(), "ns-1", catalogCache.epoch());

        listener.onCatalogInfoModified(remote(modified(namespace, ns -> ns.setURI("http://topp.example/v2"))));

        assertThat(catalogCache.lookup(oldUri)).isInstanceOf(Absent.class);
        assertThat(catalogCache.lookup(newUri)).isInstanceOf(Absent.class);
        assertThat(catalogCache.lookup(CatalogInfoKeys.defaultNamespace())).isInstanceOf(Hit.class);
    }

    // a lookup on this instance may cache the namespace with its new URI before the event arrives
    @Test
    void uriChangeAfterRefreshEvictsOld() {
        NamespaceInfo namespace = namespace("ns-1", "topp", "http://topp.example");
        putJson(namespace("ns-1", "topp", "http://topp.example/v2"));
        CacheKey oldUri = CatalogInfoKeys.namespaceByUri("http://topp.example");
        catalogCache.putId(oldUri, "ns-1", catalogCache.epoch());

        listener.onCatalogInfoModified(remote(modified(namespace, ns -> ns.setURI("http://topp.example/v2"))));

        assertThat(catalogCache.lookup(oldUri)).isInstanceOf(Absent.class);
    }

    @Test
    void renameEvictsDependentNames() {
        CacheKey oldName = CacheKey.name(WORKSPACE, "topp");
        CacheKey storeName = CacheKey.name(DATASTORE, "topp:roads");
        catalogCache.putId(oldName, "ws-1", catalogCache.epoch());
        catalogCache.putId(storeName, "store-1", catalogCache.epoch());

        listener.onCatalogInfoModified(remote(modified(topp, workspace -> workspace.setName("topp2"))));

        assertThat(catalogCache.lookup(oldName)).isInstanceOf(Absent.class);
        assertThat(catalogCache.lookup(storeName)).isInstanceOf(Absent.class);
    }

    // a layer takes the name of its resource, and the event does not tell the old resource: here it had the same name
    @Test
    void layerResourceChangeEvictsNames() {
        NamespaceInfo toppNamespace = namespace("ns-1", "topp", "http://topp.example");
        NamespaceInfo otherNamespace = namespace("ns-2", "other", "http://other.example");
        LayerInfo layer = layer("layer-1", featureType("ft-1", "roads", toppNamespace));
        FeatureTypeInfo otherRoads = featureType("ft-2", "roads", otherNamespace);
        CacheKey oldName = CacheKey.name(LAYER, "topp:roads");
        CacheKey otherName = CacheKey.name(LAYER, "topp:lakes");
        catalogCache.putId(oldName, "layer-1", catalogCache.epoch());
        catalogCache.putId(otherName, "layer-2", catalogCache.epoch());

        listener.onCatalogInfoModified(remote(modified(layer, changed -> changed.setResource(otherRoads))));

        assertThat(catalogCache.lookup(oldName)).isInstanceOf(Absent.class);
        assertThat(catalogCache.lookup(otherName)).isInstanceOf(Absent.class);
    }

    // a remote change lets the cache try the row of an uncacheable object again
    @Test
    void modifiedForgetsUncacheable() {
        CacheKey entity = CacheKey.entity(DATASTORE, "store-1");
        catalogCache.markUncacheable(entity);

        listener.onCatalogInfoModified(remote(modified(roads, store -> store.setDescription("Roads"))));

        assertThat(catalogCache.isUncacheable(entity)).isFalse();
    }

    @Test
    void removedEvictsEntityAndName() {
        CacheKey entity = CacheKey.entity(DATASTORE, "store-1");
        CacheKey name = CacheKey.name(DATASTORE, "topp:roads");
        catalogCache.put(entity, JSON, catalogCache.epoch());
        catalogCache.putId(name, "store-1", catalogCache.epoch());

        listener.onCatalogInfoRemoved(remote(CatalogInfoRemoved.createLocal(1L, roads)));

        assertThat(catalogCache.lookup(entity)).isInstanceOf(Absent.class);
        assertThat(catalogCache.lookup(name)).isInstanceOf(Absent.class);
    }

    // the event must still reach the other listeners, and an empty cache holds nothing stale
    @Test
    void failureClearsCaches() {
        EntityCache failing = new FailingEvictions();
        EntityCacheEventListener failingListener = new EntityCacheEventListener(List.of(failing, configCache));
        CacheKey entity = CacheKey.entity(DATASTORE, "store-1");
        failing.put(entity, JSON, failing.epoch());
        configCache.put(ConfigInfoKeys.global(), JSON, configCache.epoch());

        try (LogCapture log = LogCapture.of(CACHE_LOGGER)) {
            failingListener.onCatalogInfoRemoved(remote(CatalogInfoRemoved.createLocal(1L, roads)));

            assertThat(log.messages(Level.WARN)).singleElement().asString().contains("CatalogInfoRemoved");
        }
        assertThat(failing.lookup(entity)).isInstanceOf(Absent.class);
        assertThat(configCache.lookup(ConfigInfoKeys.global())).isInstanceOf(Absent.class);
    }

    @Test
    void defaultWorkspaceSetEvictsLookup() {
        catalogCache.putId(CatalogInfoKeys.defaultWorkspace(), "ws-2", catalogCache.epoch());

        listener.onDefaultWorkspaceSet(remote(DefaultWorkspaceSet.createLocal(1L, topp)));

        assertThat(catalogCache.lookup(CatalogInfoKeys.defaultWorkspace())).isInstanceOf(Absent.class);
    }

    @Test
    void defaultNamespaceSetEvictsLookup() {
        NamespaceInfo namespace = namespace("ns-1", "topp", "http://topp.example");
        catalogCache.putId(CatalogInfoKeys.defaultNamespace(), "ns-2", catalogCache.epoch());

        listener.onDefaultNamespaceSet(remote(DefaultNamespaceSet.createLocal(1L, namespace)));

        assertThat(catalogCache.lookup(CatalogInfoKeys.defaultNamespace())).isInstanceOf(Absent.class);
    }

    @Test
    void defaultDataStoreSetEvictsLookup() {
        CacheKey defaultStore = CatalogInfoKeys.defaultDataStore("ws-1");
        catalogCache.putId(defaultStore, "store-2", catalogCache.epoch());

        listener.onDefaultDataStoreSet(remote(DefaultDataStoreSet.createLocal(1L, topp, roads)));

        assertThat(catalogCache.lookup(defaultStore)).isInstanceOf(Absent.class);
    }

    // each change increments the update sequence, held by the global settings
    @Test
    void updateSequenceEvictsGlobal() {
        configCache.put(ConfigInfoKeys.global(), JSON, configCache.epoch());

        listener.onUpdateSequence(remote(UpdateSequenceEvent.createLocal(2L)));

        assertThat(configCache.lookup(ConfigInfoKeys.global())).isInstanceOf(Absent.class);
    }

    @Test
    void loggingModifiedEvictsLogging() {
        configCache.put(ConfigInfoKeys.logging(), JSON, configCache.epoch());
        Patch patch = new Patch().with("level", "VERBOSE_LOGGING");

        listener.onLoggingChange(remote(LoggingInfoModified.createLocal(1L, new LoggingInfoImpl(), patch)));

        assertThat(configCache.lookup(ConfigInfoKeys.logging())).isInstanceOf(Absent.class);
    }

    @Test
    void loggingSetEvictsLogging() {
        configCache.put(ConfigInfoKeys.logging(), JSON, configCache.epoch());

        listener.onLoggingChange(remote(LoggingInfoSet.createLocal(1L, new LoggingInfoImpl())));

        assertThat(configCache.lookup(ConfigInfoKeys.logging())).isInstanceOf(Absent.class);
    }

    @Test
    void settingsAddedEvictsLookupMiss() {
        configCache.putMiss(ConfigInfoKeys.settings("settings-1"), configCache.epoch());
        configCache.putMiss(ConfigInfoKeys.settingsByWorkspace("ws-1"), configCache.epoch());

        listener.onSettingsAdded(remote(SettingsAdded.createLocal(1L, settings("settings-1", topp))));

        assertThat(configCache.lookup(ConfigInfoKeys.settings("settings-1"))).isInstanceOf(Absent.class);
        assertThat(configCache.lookup(ConfigInfoKeys.settingsByWorkspace("ws-1")))
                .isInstanceOf(Absent.class);
    }

    @Test
    void settingsModifiedEvictsLookup() {
        configCache.put(ConfigInfoKeys.settings("settings-1"), JSON, configCache.epoch());
        configCache.putId(ConfigInfoKeys.settingsByWorkspace("ws-1"), "settings-1", configCache.epoch());
        Patch patch = new Patch().with("title", "Topp");

        listener.onSettingsModified(remote(SettingsModified.createLocal(1L, settings("settings-1", topp), patch)));

        assertThat(configCache.lookup(ConfigInfoKeys.settings("settings-1"))).isInstanceOf(Absent.class);
        assertThat(configCache.lookup(ConfigInfoKeys.settingsByWorkspace("ws-1")))
                .isInstanceOf(Absent.class);
    }

    // the event tells the new workspace only. GeoServer offers no such move.
    @Test
    void settingsMoveEvictsLookups() {
        configCache.putId(ConfigInfoKeys.settingsByWorkspace("ws-1"), "settings-1", configCache.epoch());
        Patch patch = new Patch().with("workspace", other);

        listener.onSettingsModified(remote(SettingsModified.createLocal(1L, settings("settings-1", other), patch)));

        assertThat(configCache.lookup(ConfigInfoKeys.settingsByWorkspace("ws-1")))
                .isInstanceOf(Absent.class);
    }

    @Test
    void settingsRemovedEvictsLookup() {
        configCache.put(ConfigInfoKeys.settings("settings-1"), JSON, configCache.epoch());
        configCache.putId(ConfigInfoKeys.settingsByWorkspace("ws-1"), "settings-1", configCache.epoch());

        listener.onSettingsRemoved(remote(SettingsRemoved.createLocal(1L, settings("settings-1", topp))));

        assertThat(configCache.lookup(ConfigInfoKeys.settings("settings-1"))).isInstanceOf(Absent.class);
        assertThat(configCache.lookup(ConfigInfoKeys.settingsByWorkspace("ws-1")))
                .isInstanceOf(Absent.class);
    }

    @Test
    void serviceAddedEvictsLookups() {
        CacheKey byClass = ConfigInfoKeys.serviceByWorkspaceAndClass(null, ServiceInfo.class);
        configCache.putMiss(ConfigInfoKeys.service("wms-1"), configCache.epoch());
        configCache.putMiss(byClass, configCache.epoch());

        listener.onServiceChange(remote(ServiceAdded.createLocal(1L, service("wms-1", "WMS"))));

        assertThat(configCache.lookup(ConfigInfoKeys.service("wms-1"))).isInstanceOf(Absent.class);
        assertThat(configCache.lookup(byClass)).isInstanceOf(Absent.class);
    }

    // the event tells neither the class used by the lookups of the service nor its old name
    @Test
    void serviceModifiedEvictsConfigEntry() {
        CacheKey byClass = ConfigInfoKeys.serviceByWorkspaceAndClass(null, ServiceInfo.class);
        configCache.put(ConfigInfoKeys.service("wms-1"), JSON, configCache.epoch());
        configCache.putId(byClass, "wms-1", configCache.epoch());
        configCache.putId(ConfigInfoKeys.serviceByName("WMS"), "wms-1", configCache.epoch());
        configCache.putId(ConfigInfoKeys.settingsByWorkspace("ws-1"), "settings-1", configCache.epoch());
        Patch patch = new Patch().with("name", "WMS2");

        listener.onServiceChange(remote(ServiceModified.createLocal(1L, service("wms-1", "WMS"), patch)));

        assertThat(configCache.lookup(ConfigInfoKeys.service("wms-1"))).isInstanceOf(Absent.class);
        assertThat(configCache.lookup(byClass)).isInstanceOf(Absent.class);
        assertThat(configCache.lookup(ConfigInfoKeys.serviceByName("WMS"))).isInstanceOf(Absent.class);
        assertThat(configCache.lookup(ConfigInfoKeys.settingsByWorkspace("ws-1")))
                .isInstanceOf(Hit.class);
    }

    // the database deletes the settings and the services of a removed workspace, with no write of the repository
    @Test
    void workspaceRemovalEvictsConfig() {
        putSettingsAndService();

        listener.onCatalogInfoRemoved(remote(CatalogInfoRemoved.createLocal(1L, topp)));

        assertSettingsAndServiceEvicted();
    }

    // the catalog repositories evict the catalog keys of a removal made on this service instance
    @Test
    void localWorkspaceRemovalEvicts() {
        CacheKey workspaceKey = CacheKey.entity(WORKSPACE, "ws-1");
        catalogCache.put(workspaceKey, JSON, catalogCache.epoch());
        putSettingsAndService();

        listener.onCatalogInfoRemoved(CatalogInfoRemoved.createLocal(1L, topp));

        assertSettingsAndServiceEvicted();
        assertThat(catalogCache.lookup(workspaceKey)).isInstanceOf(Hit.class);
    }

    @Test
    void serviceRemovedEvictsLookups() {
        CacheKey byClass = ConfigInfoKeys.serviceByWorkspaceAndClass(null, ServiceInfo.class);
        configCache.put(ConfigInfoKeys.service("wms-1"), JSON, configCache.epoch());
        configCache.putId(byClass, "wms-1", configCache.epoch());
        configCache.putId(ConfigInfoKeys.serviceByName("WMS"), "wms-1", configCache.epoch());

        listener.onServiceChange(remote(ServiceRemoved.createLocal(1L, service("wms-1", "WMS"))));

        assertThat(configCache.lookup(ConfigInfoKeys.service("wms-1"))).isInstanceOf(Absent.class);
        assertThat(configCache.lookup(byClass)).isInstanceOf(Absent.class);
        assertThat(configCache.lookup(ConfigInfoKeys.serviceByName("WMS"))).isInstanceOf(Absent.class);
    }

    /** Puts the settings and a service of the workspace topp, and the global settings. */
    private void putSettingsAndService() {
        long epoch = configCache.epoch();
        configCache.put(ConfigInfoKeys.settings("settings-1"), JSON, epoch);
        configCache.putId(ConfigInfoKeys.settingsByWorkspace("ws-1"), "settings-1", epoch);
        configCache.put(ConfigInfoKeys.service("wms-1"), JSON, epoch);
        configCache.putId(ConfigInfoKeys.serviceByWorkspaceAndClass("ws-1", ServiceInfo.class), "wms-1", epoch);
        configCache.put(ConfigInfoKeys.global(), JSON, epoch);
    }

    private void assertSettingsAndServiceEvicted() {
        CacheKey byClass = ConfigInfoKeys.serviceByWorkspaceAndClass("ws-1", ServiceInfo.class);
        assertThat(configCache.lookup(ConfigInfoKeys.settings("settings-1"))).isInstanceOf(Absent.class);
        assertThat(configCache.lookup(ConfigInfoKeys.settingsByWorkspace("ws-1")))
                .isInstanceOf(Absent.class);
        assertThat(configCache.lookup(ConfigInfoKeys.service("wms-1"))).isInstanceOf(Absent.class);
        assertThat(configCache.lookup(byClass)).isInstanceOf(Absent.class);
        assertThat(configCache.lookup(ConfigInfoKeys.global())).isInstanceOf(Hit.class);
    }

    private static EntityCache newCache(String name) {
        EntityCache cache = new EntityCache(name, ONE_MIB, Duration.ofSeconds(30), new NoopCodec());
        cache.setInitialized(true);
        return cache;
    }

    private static <E extends GeoServerEvent> E remote(E event) {
        event.setRemote(true);
        return event;
    }

    /** The event of a change made through a {@link ModificationProxy}, as the catalog makes it. */
    private static <T extends CatalogInfo> CatalogInfoModified modified(T info, Consumer<T> change) {
        Class<T> type = ConfigInfoType.valueOf(info).type();
        T proxy = ModificationProxy.create(info, type);
        change.accept(proxy);
        return CatalogInfoModified.createLocal(1L, proxy);
    }

    private void putJson(CatalogInfo info) {
        CacheKey key = CacheKey.entity(ConfigInfoType.valueOf(info), info.getId());
        catalogCache.put(key, INFO_MAPPER.writeValueAsBytes(info), catalogCache.epoch());
    }

    private static WorkspaceInfo workspace(String id, String name) {
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId(id);
        workspace.setName(name);
        return workspace;
    }

    private static NamespaceInfo namespace(String id, String prefix, String uri) {
        NamespaceInfoImpl namespace = new NamespaceInfoImpl();
        namespace.setId(id);
        namespace.setPrefix(prefix);
        namespace.setURI(uri);
        return namespace;
    }

    private static DataStoreInfo store(String id, String name, WorkspaceInfo workspace) {
        DataStoreInfoImpl store = new DataStoreInfoImpl(null, id);
        store.setName(name);
        store.setWorkspace(workspace);
        return store;
    }

    private static LayerGroupInfo group(String id, String name, @Nullable WorkspaceInfo workspace) {
        LayerGroupInfoImpl group = new LayerGroupInfoImpl();
        group.setId(id);
        group.setName(name);
        group.setWorkspace(workspace);
        return group;
    }

    /** A style without a workspace. */
    private static StyleInfo style(String id, String name) {
        StyleInfoImpl style = new StyleInfoImpl(null);
        style.setId(id);
        style.setName(name);
        return style;
    }

    private static LayerInfo layer(String id, FeatureTypeInfo resource) {
        LayerInfoImpl layer = new LayerInfoImpl();
        layer.setId(id);
        layer.setResource(resource);
        return layer;
    }

    private static FeatureTypeInfo featureType(String id, String name, NamespaceInfo namespace) {
        FeatureTypeInfoImpl featureType = new FeatureTypeInfoImpl(null, id);
        featureType.setName(name);
        featureType.setNamespace(namespace);
        return featureType;
    }

    private static SettingsInfo settings(String id, WorkspaceInfo workspace) {
        SettingsInfoImpl settings = new SettingsInfoImpl();
        settings.setId(id);
        settings.setWorkspace(workspace);
        return settings;
    }

    /** A global service. */
    private static ServiceInfo service(String id, String name) {
        ServiceInfoImpl service = new ServiceInfoImpl();
        service.setId(id);
        service.setName(name);
        return service;
    }

    /** A cache failing on each eviction by key. */
    private static final class FailingEvictions extends EntityCache {

        FailingEvictions() {
            super("gs-catalog", ONE_MIB, Duration.ofSeconds(30), new NoopCodec());
        }

        @Override
        public void evict(CacheKey... keys) {
            throw new IllegalStateException("evict failed");
        }
    }
}
