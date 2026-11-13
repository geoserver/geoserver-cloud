/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

import com.google.common.base.Stopwatch;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.impl.LayerGroupStyleListener;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.requestcache.RequestCacheScope;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.cloud.event.bus.RemoteEventsResumed;
import org.geoserver.cloud.event.catalog.CatalogInfoAdded;
import org.geoserver.cloud.event.catalog.CatalogInfoModified;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geotools.api.filter.Filter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.WebApplicationContext;

/**
 * Alternative to {@link LayerGroupContainmentCache}
 *
 * <ul>
 *   <li>Avoids building the cache twice during startup, once at the class constructor and again at
 *       {@link #onApplicationEvent(ContextRefreshedEvent)}
 *   <li>{@link #onApplicationEvent(ContextRefreshedEvent)} ignores the event if it's not for a
 *       {@link WebApplicationContext} (e.g. the spring boot actuator's context)
 *   <li>Makes a single pass over the {@link LayerGroupInfo}s in the catalog at {@link #buildLayerGroupCaches()}
 *   <li>Traverses the layer groups in a streaming fashion, avoiding loading them all in memory through
 *       {@link Catalog#getLayerGroups()}
 * </ul>
 *
 * With this, it takes 1 minute to build the cache with the pgconfig catalog backend, on a catalog with 70k layer
 * groups, whereas previously it would go out of memory after several minutes.
 *
 * <p>Further improvements may involve making the cache being build lazily as required by calls to
 * {@link #getContainerGroupsFor(LayerGroupInfo)} and/or {@link #getContainerGroupsFor(ResourceInfo)}. For the later,
 * only global and same-workspace layer groups may be queried.
 *
 * <p>The catalog notifies its listeners of the changes made on this service instance only. The changes of other
 * instances arrive as remote events, and the cache reads the changed group from the raw catalog. The listeners updating
 * the backend's view of the catalog run first: the cache reads the group after them. On the data directory backend, a
 * remote add or change deferred until its references arrive reaches the cache later, on a lookup of the group, or not
 * at all.
 *
 * @since 1.8.2
 */
@Slf4j
@SuppressWarnings({"java:S2177", "java:S6201", "java:S3776", "java:S3398"})
public class GsCloudLayerGroupContainmentCache extends LayerGroupContainmentCache implements ApplicationContextAware {

    /**
     * The precedence of the remote event listeners, lower than the precedence of the listeners updating the backend's
     * view of the catalog.
     */
    static final int REMOTE_EVENTS_ORDER = Ordered.LOWEST_PRECEDENCE - 100;

    /** The properties of a layer group read by the cache: its name, workspace and mode, and its members */
    private static final Set<String> SUMMARY_PROPERTIES = Set.of("name", "workspace", "layers", "mode");

    private Catalog rawCatalog;
    private ApplicationContext applicationContext;

    private LayerGroupWarmUp warmUp = LayerGroupWarmUp.NONE;

    /** The last build submitted by {@link #buildLayerGroupCaches()}, running after the builds submitted before it */
    CompletableFuture<Void> buildTask = CompletableFuture.completedFuture(null);

    /** The cancellation of the last build, set when another build is requested */
    private AtomicBoolean lastBuildCancelled = new AtomicBoolean();

    /**
     * Whether a build lists the groups. A change made during a build reaches the maps in use, not the maps of the
     * build: the build applies it again once it installs its maps.
     */
    private volatile boolean building;

    /** The changes made during the build, applied again to the maps of the build */
    private final Queue<Runnable> changesDuringBuild = new ConcurrentLinkedQueue<>();

    public GsCloudLayerGroupContainmentCache(Catalog rawCatalog) {
        /*
         * fix: constructor calls buildLayerGroupCaches, give it an empty catalog,
         * we override everything here, and avoid building the cache twice,
         * on the constructor and on the app context event
         */
        super(new CatalogPlugin());
        this.rawCatalog = rawCatalog;
        rawCatalog.addListener(new CatalogChangeListener());
        rawCatalog.addListener(new LayerGroupStyleListener());
    }

    public void setWarmUp(@NonNull LayerGroupWarmUp warmUp) {
        this.warmUp = warmUp;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext() != this.applicationContext) {
            log.debug("Ignoring non web application context refresh event");
            return;
        }
        log.info("Application context refreshed, building layer group containment cache");
        buildLayerGroupCaches();
    }

    /**
     * Builds the layer group cache asynchronously. A new build cancels the build in progress and starts after it ends:
     * two builds never write the caches together. Each build has a cancellation of its own.
     */
    private synchronized void buildLayerGroupCaches() {
        lastBuildCancelled.set(true);
        AtomicBoolean cancelled = new AtomicBoolean();
        lastBuildCancelled = cancelled;
        // the next build starts whatever the outcome of the previous one
        CompletableFuture<Void> previousBuild = buildTask.exceptionally(failure -> null);
        buildTask = previousBuild.thenRunAsync(() -> doBuildLayerGroupCaches(cancelled));
    }

    /**
     * Rebuilds the caches from a listing of the groups, into new maps. The access checks read the maps in use until the
     * build installs its own. A cancelled build installs nothing: it leaves the caches to the build after it.
     */
    private void doBuildLayerGroupCaches(AtomicBoolean cancelled) {
        if (cancelled.get()) {
            log.debug("Layer group containment cache build cancelled before it started");
            return;
        }
        Stopwatch sw = Stopwatch.createStarted();
        building = true;
        try {
            runWarmUp(cancelled);
            Containment built = listInItsOwnScope(cancelled);
            sw.stop();
            if (cancelled.get()) {
                log.info("Layer group containment cache build cancelled after {}", sw);
            } else {
                install(built);
                logBuilt(sw);
            }
        } finally {
            stopRecording();
        }
    }

    /** A failed warm-up only costs speed: the listing loads the groups one by one. */
    private void runWarmUp(AtomicBoolean cancelled) {
        try {
            warmUp.warmUp(cancelled::get);
        } catch (RuntimeException e) {
            log.warn("Layer group warm-up failed, listing the groups without it", e);
        }
    }

    private void logBuilt(Stopwatch sw) {
        log.info(
                "Built layer group containment cache in {}. Group cache size: {}, resource containment cache size: {} with {} layergroups",
                sw,
                groupCache.size(),
                resourceContainmentCache.size(),
                resourceContainmentCache.values().stream().flatMap(Set::stream).count());
    }

    /**
     * Stops recording the changes. A cancelled or failed build leaves the maps in use: the changes made during the
     * build reached them.
     */
    private void stopRecording() {
        building = false;
        changesDuringBuild.clear();
    }

    /**
     * Lists the groups within a scope of {@link RequestCacheScope}, opened on the thread of the build. On a backend
     * with a request cache, the scope keeps the members looked up to resolve the listed groups: a group listed later
     * finds the members of the earlier ones there. The scope closes before the install: the changes applied again after
     * the install must read the groups saved meanwhile, not the copies kept by the scope.
     */
    private Containment listInItsOwnScope(AtomicBoolean cancelled) {
        Runnable closeScope = RequestCacheScope.openIfAbsent(RequestCacheScope.DEFAULT_MAX_ENTRIES);
        try {
            return listGroups(cancelled);
        } finally {
            closeScope.run();
        }
    }

    /** Registers the groups in new maps, in a single pass over a streamed listing. */
    private Containment listGroups(AtomicBoolean cancelled) {
        Containment built = Containment.empty();
        try (CloseableIterator<LayerGroupInfo> groups = rawCatalog.list(LayerGroupInfo.class, Filter.INCLUDE)) {
            while (!cancelled.get() && groups.hasNext()) {
                built.register(groups.next());
            }
        }
        return built;
    }

    /**
     * Installs the maps of a build, then applies again the changes made during the build. These changes reached the
     * previous maps, and the listing may have read the groups before them.
     *
     * <p>The two maps are plain fields of the upstream class. The volatile write of {@link #building} after them orders
     * their writes before it. A request thread reading the fields does not read {@code building}, and may see the
     * previous maps for a moment, or the new groups with the previous resources: both are complete maps.
     */
    private void install(Containment built) {
        groupCache = built.groups();
        resourceContainmentCache = built.resources();
        building = false;
        Runnable change = changesDuringBuild.poll();
        while (change != null) {
            applyAgainOrWarn(change);
            change = changesDuringBuild.poll();
        }
    }

    private void applyAgainOrWarn(Runnable change) {
        try {
            change.run();
        } catch (RuntimeException e) {
            log.warn("Could not apply a layer group change made during the build of the containment cache", e);
        }
    }

    /** Records the change of a group made during a build: the build reads the group again once it installs its maps. */
    private void recordGroupChange(String groupId) {
        if (building) {
            changesDuringBuild.add(() -> updateGroup(groupId));
        }
    }

    private void recordWorkspaceRename(String oldName, String newName) {
        if (building) {
            changesDuringBuild.add(() -> updateWorkspaceNames(oldName, newName));
        }
    }

    /** Returns the maps in use. */
    private Containment live() {
        return new Containment(groupCache, resourceContainmentCache);
    }

    private LayerGroupSummary getGroupData(LayerGroupInfo lg) {
        return live().register(lg);
    }

    /**
     * The two maps of the cache: the summaries of the groups by id, and the groups containing each resource. A build
     * fills maps of its own. A change of a group applies all its steps to the maps in use when it starts: a change
     * running across an install must not mix the summaries of the two builds. The build applies it again to its maps.
     */
    private record Containment(Map<String, LayerGroupSummary> groups, Map<String, Set<LayerGroupSummary>> resources) {

        static Containment empty() {
            return new Containment(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
        }

        /**
         * Returns the summary of the group, registering the group and its members when it is unknown. A group listed
         * after its containers is found registered.
         */
        LayerGroupSummary register(LayerGroupInfo lg) {
            LayerGroupSummary groupData = groups.get(lg.getId());
            if (null == groupData) {
                groupData = groups.computeIfAbsent(lg.getId(), id -> new LayerGroupSummary(lg));
                registerMembers(lg, groupData);
            }
            return groupData;
        }

        /** Adds the group to the containers of its layers' resources and of its member groups. */
        void registerMembers(LayerGroupInfo group, LayerGroupSummary groupData) {
            group.getLayers().stream()
                    .filter(IS_LAYER)
                    .map(LayerInfo.class::cast)
                    .forEach(layer -> addResourceContainer(layer.getResource().getId(), groupData));
            group.getLayers().stream()
                    .filter(IS_GROUP)
                    .map(LayerGroupInfo.class::cast)
                    .forEach(member -> addContainer(member, groupData));
        }

        private void addContainer(LayerGroupInfo member, LayerGroupSummary container) {
            LayerGroupSummary memberData = register(member);
            memberData.containerGroups.add(container);
        }

        /**
         * Adds the group to the containers of the resource. The map computes the addition atomically, as it computes
         * the removal of an emptied set in {@link GsCloudLayerGroupContainmentCache#clearContainment}: the addition
         * never lands in a removed set.
         */
        void addResourceContainer(String resourceId, LayerGroupSummary container) {
            resources.compute(resourceId, (id, containers) -> {
                Set<LayerGroupSummary> resourceContainers =
                        containers == null ? CONCURRENT_SET_BUILDER.apply(id) : containers;
                resourceContainers.add(container);
                return resourceContainers;
            });
        }
    }

    /** Returns all groups directly or indirectly containing the resource */
    @Override
    public Collection<LayerGroupSummary> getContainerGroupsFor(ResourceInfo resource) {
        String id = resource.getId();
        Set<LayerGroupSummary> groups = resourceContainmentCache.get(id);
        if (groups == null) {
            return List.of();
        }
        Set<LayerGroupSummary> result = new HashSet<>();
        for (LayerGroupSummary lg : groups) {
            collectContainers(lg, result);
        }
        return result;
    }

    /**
     * Returns all groups containing directly or indirectly the specified group, and relevant for security (e.g.,
     * anything but {@link LayerGroupInfo.Mode#SINGLE} ones
     */
    @Override
    public Collection<LayerGroupSummary> getContainerGroupsFor(LayerGroupInfo lg) {
        if (null == lg.getId()) {
            return Set.of();
        }
        LayerGroupSummary summary = getGroupData(lg);
        if (summary != null) {
            Set<LayerGroupSummary> groups = new HashSet<>();
            summary.getContainerGroups().forEach(container -> collectContainers(container, groups));
            return groups;
        }
        return Set.of();
    }

    /** Recursively collects the group and all its containers in the <data>groups</data> collection */
    private void collectContainers(LayerGroupSummary lg, Set<LayerGroupSummary> groups) {
        if (!groups.contains(lg)) {
            if (lg.getMode() != LayerGroupInfo.Mode.SINGLE) {
                groups.add(lg);
            }
            lg.containerGroups.forEach(container -> collectContainers(container, groups));
        }
    }

    /**
     * Rebuilds the caches when the events of the other service instances reach this one again: the changes of the
     * groups sent meanwhile are lost. The listeners emptying the caches of the backend run first: the build must not
     * read their stale entries.
     */
    @EventListener(RemoteEventsResumed.class)
    @Order(REMOTE_EVENTS_ORDER)
    public void onRemoteEventsResumed(RemoteEventsResumed event) {
        log.info("Remote events resumed on the queue {}, re-building layer group containment cache", event.queue());
        buildLayerGroupCaches();
    }

    /** Registers a group added through another service instance. */
    @EventListener(CatalogInfoAdded.class)
    @Order(REMOTE_EVENTS_ORDER)
    public void onRemoteGroupAdded(CatalogInfoAdded event) {
        if (isRemoteGroupEvent(event)) {
            String groupId = event.getObjectId();
            recordGroupChange(groupId);
            applyOrWarn(event, () -> registerGroup(groupId));
        }
    }

    /**
     * Updates a group changed through another service instance. Only a change of the name, the workspace, the members
     * or the mode of the group updates the cache.
     */
    @EventListener(CatalogInfoModified.class)
    @Order(REMOTE_EVENTS_ORDER)
    public void onRemoteGroupModified(CatalogInfoModified event) {
        if (isRemoteGroupEvent(event) && changesSummary(event.getPatch())) {
            String groupId = event.getObjectId();
            recordGroupChange(groupId);
            applyOrWarn(event, () -> updateGroup(groupId));
        }
    }

    /** Clears the data of a group removed through another service instance. */
    @EventListener(CatalogInfoRemoved.class)
    @Order(REMOTE_EVENTS_ORDER)
    public void onRemoteGroupRemoved(CatalogInfoRemoved event) {
        if (isRemoteGroupEvent(event)) {
            String groupId = event.getObjectId();
            recordGroupChange(groupId);
            applyOrWarn(event, () -> clearGroupInfo(live(), groupId));
        }
    }

    /** Renames the workspace of the summaries, for a workspace renamed through another service instance. */
    @EventListener(CatalogInfoModified.class)
    @Order(REMOTE_EVENTS_ORDER)
    public void onRemoteWorkspaceRenamed(CatalogInfoModified event) {
        if (isRemoteWorkspaceRename(event)) {
            String oldName = event.getOldName();
            String newName = event.getObjectName();
            recordWorkspaceRename(oldName, newName);
            applyOrWarn(event, () -> updateWorkspaceNames(oldName, newName));
        }
    }

    private static boolean isRemoteGroupEvent(InfoEvent event) {
        return event.isRemote() && event.getObjectType() == ConfigInfoType.LAYERGROUP;
    }

    private static boolean changesSummary(Patch patch) {
        return patch.getPropertyNames().stream().anyMatch(SUMMARY_PROPERTIES::contains);
    }

    /** Whether a remote event renames a workspace. The event tells the names before and after the change. */
    private static boolean isRemoteWorkspaceRename(CatalogInfoModified event) {
        boolean remoteWorkspaceChange = event.isRemote() && event.getObjectType() == ConfigInfoType.WORKSPACE;
        String oldName = event.getOldName();
        return remoteWorkspaceChange && oldName != null && !oldName.equals(event.getObjectName());
    }

    /**
     * Applies the change of a remote event, and logs a warning if the change fails: the later listeners of the event
     * must still run. An add or a change looks the group up before its first write: a failed lookup leaves the cache as
     * it was.
     */
    private void applyOrWarn(InfoEvent event, Runnable change) {
        try {
            change.run();
        } catch (RuntimeException e) {
            log.warn("Could not update the layer group containment upon {}", event.toShortString(), e);
        }
    }

    /** Registers the group read from the raw catalog. A group removed in the meantime is not found. */
    private void registerGroup(String groupId) {
        LayerGroupInfo group = rawCatalog.getLayerGroup(groupId);
        if (group != null) {
            getGroupData(group);
        }
    }

    /**
     * Updates the summary of the group from the group read from the raw catalog. A group removed in the meantime is
     * only cleared, and a group unknown to the cache is registered.
     */
    private void updateGroup(String groupId) {
        LayerGroupInfo group = rawCatalog.getLayerGroup(groupId);
        Containment maps = live();
        LayerGroupSummary summary = maps.groups().get(groupId);
        if (group == null) {
            clearGroupInfo(maps, groupId);
        } else if (summary == null) {
            maps.register(group);
        } else {
            updateSummary(maps, summary, group);
        }
    }

    /**
     * Copies the name, the workspace and the mode of the group to its summary, and gives the summary the members of the
     * group. The summary keeps its containers: a change of the group does not change the groups containing it.
     *
     * <p>The event thread writes the fields of the summary in place. They are plain fields of the upstream class, and a
     * request may read an old value for a moment.
     */
    private static void updateSummary(Containment maps, LayerGroupSummary summary, LayerGroupInfo group) {
        Set<String> memberResources = memberResourceIds(group);
        Set<String> memberGroups = memberGroupIds(group);
        summary.name = group.getName();
        summary.workspace = workspaceName(group);
        summary.mode = group.getMode();
        removeLostMembers(maps, summary, memberResources, memberGroups);
        maps.registerMembers(group, summary);
    }

    private static Set<String> memberResourceIds(LayerGroupInfo group) {
        return group.getLayers().stream()
                .filter(IS_LAYER)
                .map(LayerInfo.class::cast)
                .map(layer -> layer.getResource().getId())
                .collect(Collectors.toSet());
    }

    private static Set<String> memberGroupIds(LayerGroupInfo group) {
        return group.getLayers().stream()
                .filter(IS_GROUP)
                .map(PublishedInfo::getId)
                .collect(Collectors.toSet());
    }

    private static String workspaceName(LayerGroupInfo group) {
        WorkspaceInfo workspace = group.getWorkspace();
        return workspace == null ? null : workspace.getName();
    }

    /**
     * Removes the summary from the containers of the resources and of the groups missing from the members. The event of
     * a change does not tell the members before the change: the method checks each resource and each group.
     */
    private static void removeLostMembers(
            Containment maps, LayerGroupSummary summary, Set<String> memberResources, Set<String> memberGroups) {
        for (String resourceId : maps.resources().keySet()) {
            if (!memberResources.contains(resourceId)) {
                clearContainment(maps, summary, resourceId);
            }
        }
        for (LayerGroupSummary other : maps.groups().values()) {
            if (!memberGroups.contains(other.getId())) {
                other.containerGroups.remove(summary);
            }
        }
    }

    private void updateWorkspaceNames(String oldName, String newName) {
        groupCache.values().stream()
                .filter(lg -> Objects.equals(lg.workspace, oldName))
                .forEach(lg -> lg.workspace = newName);
    }

    /**
     * Clears the data of the group with the id. A remote event does not tell the members of the group before the
     * change: the method removes the group from the containers of each resource.
     */
    private static void clearGroupInfo(Containment maps, String groupId) {
        LayerGroupSummary data = maps.groups().remove(groupId);
        if (data == null) {
            return;
        }
        for (String resourceId : maps.resources().keySet()) {
            clearContainment(maps, data, resourceId);
        }
        removeAsContainer(maps, data);
    }

    /** Clears the data of a group removed on this service instance: the removed group tells its members. */
    private static void clearGroupInfo(Containment maps, LayerGroupInfo lg) {
        final LayerGroupSummary data = maps.groups().remove(lg.getId());
        if (data == null) {
            return;
        }
        // clear the resource containment cache
        lg.getLayers().stream()
                .filter(IS_LAYER)
                .map(LayerInfo.class::cast)
                .map(LayerInfo::getResource)
                .map(ResourceInfo::getId)
                .forEach(rid -> clearContainment(maps, data, rid));
        removeAsContainer(maps, data);
    }

    /** Removes the group from the containers of the other groups. */
    private static void removeAsContainer(Containment maps, LayerGroupSummary data) {
        for (LayerGroupSummary d : maps.groups().values()) {
            // will be removed by id based equality
            d.containerGroups.remove(data);
        }
    }

    /**
     * Removes the group from the containers of the resource, and the emptied set from the map. The map computes the
     * removal atomically: a concurrent {@link Containment#addResourceContainer} adds to a set of the map.
     */
    private static void clearContainment(Containment maps, LayerGroupSummary containerSummary, String resourceId) {
        maps.resources().computeIfPresent(resourceId, (id, containers) -> {
            containers.remove(containerSummary);
            return containers.isEmpty() ? null : containers;
        });
    }

    /**
     * This listener keeps the "layer group" flags in the authorization tree current, in order to optimize the
     * application of layer group containment rules
     */
    final class CatalogChangeListener implements CatalogListener {

        @Override
        public void reloaded() {
            log.info("Catalog reloaded, re-building layer group containment cache");
            buildLayerGroupCaches();
        }

        @Override
        public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
            if (event.getSource() instanceof LayerGroupInfo lg) {
                recordGroupChange(lg.getId());
                getGroupData(lg);
            }
        }

        @Override
        public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
            if (event.getSource() instanceof LayerGroupInfo lg) {
                recordGroupChange(lg.getId());
                clearGroupInfo(live(), lg);
            }
            // no need to listen to workspace or layer removal, these will cascade to
            // layer groups
        }

        @Override
        public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
            // the change is applied once saved, by handlePostModifyEvent
        }

        /**
         * Applies the change of a group or a workspace once it is saved. A change made during a build is applied again
         * after the build, from the saved group: the group read then holds the change.
         */
        @Override
        public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
            final CatalogInfo source = event.getSource();
            if (source instanceof LayerGroupInfo lg) {
                recordGroupChange(lg.getId());
                Containment maps = live();
                LayerGroupSummary summary = maps.register(lg);
                // was the layer group renamed, moved, or its contents changed?
                handleNameChange(event, summary);
                handleWorkspaceChange(event, summary);
                handleLayersChange(event, maps, summary);
                handleModeChange(event, summary);
            } else if (source instanceof WorkspaceInfo) {
                int nameIdx = event.getPropertyNames().indexOf("name");
                if (nameIdx != -1) {
                    String oldName = (String) event.getOldValues().get(nameIdx);
                    String newName = (String) event.getNewValues().get(nameIdx);
                    recordWorkspaceRename(oldName, newName);
                    updateWorkspaceNames(oldName, newName);
                }
            }
        }

        private void handleNameChange(CatalogPostModifyEvent event, LayerGroupSummary summary) {
            int nameIdx = event.getPropertyNames().indexOf("name");
            if (nameIdx != -1) {
                String newName = (String) event.getNewValues().get(nameIdx);
                summary.name = newName;
            }
        }

        private void handleWorkspaceChange(CatalogPostModifyEvent event, LayerGroupSummary summary) {
            int wsIdx = event.getPropertyNames().indexOf("workspace");
            if (wsIdx != -1) {
                WorkspaceInfo newWorkspace =
                        (WorkspaceInfo) event.getNewValues().get(wsIdx);
                summary.workspace = newWorkspace == null ? null : newWorkspace.getName();
            }
        }

        @SuppressWarnings("unchecked")
        private void handleLayersChange(CatalogPostModifyEvent event, Containment maps, LayerGroupSummary summary) {

            int layersIdx = event.getPropertyNames().indexOf("layers");
            if (layersIdx != -1) {
                List<PublishedInfo> oldLayers;
                List<PublishedInfo> newLayers;

                oldLayers = (List<PublishedInfo>) event.getOldValues().get(layersIdx);
                newLayers = (List<PublishedInfo>) event.getNewValues().get(layersIdx);
                updateContainedLayers(maps, summary, oldLayers, newLayers);
            }
        }

        private void handleModeChange(CatalogPostModifyEvent event, LayerGroupSummary summary) {
            int modeIdx = event.getPropertyNames().indexOf("mode");
            if (modeIdx != -1) {
                Mode newMode = (Mode) event.getNewValues().get(modeIdx);
                summary.mode = newMode;
            }
        }

        private void updateContainedLayers(
                Containment maps,
                @NonNull LayerGroupSummary groupSummary,
                List<PublishedInfo> oldLayers,
                List<PublishedInfo> newLayers) {

            // do not rely on PublishedInfo.equals()...
            MapDifference<String, PublishedInfo> difference = Maps.difference(toIdMap(oldLayers), toIdMap(newLayers));

            Map<String, PublishedInfo> removedLayers = difference.entriesOnlyOnLeft();
            // process layers that are no longer contained
            for (PublishedInfo removed : removedLayers.values()) {
                if (removed instanceof LayerInfo layer) {
                    String resourceId = layer.getResource().getId();
                    clearContainment(maps, groupSummary, resourceId);
                } else if (removed instanceof LayerGroupInfo child) {
                    LayerGroupSummary summary = maps.groups().get(child.getId());
                    if (summary != null) {
                        summary.containerGroups.remove(groupSummary);
                    }
                }
            }

            // add the layers that are newly contained
            final Map<String, PublishedInfo> addedLayers = difference.entriesOnlyOnRight();
            for (PublishedInfo added : addedLayers.values()) {
                if (added instanceof LayerInfo layer) {
                    String resourceId = layer.getResource().getId();
                    maps.addResourceContainer(resourceId, groupSummary);
                } else if (added instanceof LayerGroupInfo child) {
                    LayerGroupSummary summary = maps.groups().get(child.getId());
                    if (summary != null) {
                        summary.containerGroups.add(groupSummary);
                    }
                }
            }
        }

        private Map<String, PublishedInfo> toIdMap(List<PublishedInfo> layers) {
            if (layers.isEmpty()) {
                return Map.of();
            }
            Map<String, PublishedInfo> map = new HashMap<>();
            layers.stream().forEach(l -> map.put(l.getId(), l));
            return map;
        }
    }
}
