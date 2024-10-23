/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.security.impl;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.LayerGroupStyleListener;
import org.geotools.api.filter.Filter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.context.WebApplicationContext;

/**
 * Alternative to {@link LayerGroupContainmentCache}
 *
 * <p>
 *
 * <ul>
 *   <li>Avoids building the cache twice during startup, once at the class constructor and again at
 *       {@link #onApplicationEvent(ContextRefreshedEvent)}
 *   <li>{@link #onApplicationEvent(ContextRefreshedEvent)} ignores the event if it's not for a
 *       {@link WebApplicationContext} (e.g. the spring boot actuator's context)
 *   <li>Makes a single pass over the {@link LayerGroupInfo}s in the catalog at {@link
 *       #buildLayerGroupCaches()}
 *   <li>Traverses the layer groups in a streaming fashion, avoiding loading them all in memory
 *       through {@link Catalog#getLayerGroups()}
 * </ul>
 *
 * With this, it takes 1 mintue to build the cache with the pgconfig catalog backend, on a catalog
 * with 70k layer groups, whereas previously it would go out of memory after several minutes.
 *
 * <p>Further improvements may involve making the cache being build lazily as required by calls to
 * {@link #getContainerGroupsFor(LayerGroupInfo)} and/or {@link
 * #getContainerGroupsFor(ResourceInfo)}. For the later, only global and same-workspace layer groups
 * may be queried.
 *
 * @see NoopLayerGroupContainmentCache
 * @since 1.8.2
 */
@Slf4j
@SuppressWarnings({"java:S2177", "java:S6201", "java:S3776", "java:S3398"})
public class GsCloudLayerGroupContainmentCache extends LayerGroupContainmentCache implements ApplicationContextAware {

    private Catalog rawCatalog;
    private ApplicationContext applicationContext;

    /** Async task submitted by {@link #buildLayerGroupCaches()} */
    CompletableFuture<Void> buildTask = CompletableFuture.completedFuture(null);

    /**
     * abort flag for a running {@link #buildLayerGroupCaches()} before running again in response to
     * {@link CatalogChangeListener#reloaded()} or {@link
     * #onApplicationEvent(ContextRefreshedEvent)}
     */
    private volatile boolean abort;

    public GsCloudLayerGroupContainmentCache(Catalog rawCatalog) {
        /*
         * fix: constructor calls buildLayerGroupCaches, give it an empty catalog,
         * we override everything here, and avoid building the cache twice,
         * on the constructor and on the app context event
         */
        super(new CatalogImpl());
        this.rawCatalog = rawCatalog;
        rawCatalog.addListener(new CatalogChangeListener());
        rawCatalog.addListener(new LayerGroupStyleListener());
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

    /** Builds the layer group cache asynchronously */
    private void buildLayerGroupCaches() {
        if (!buildTask.isDone()) {
            abort = true;
        }
        buildTask = CompletableFuture.runAsync(this::doBuildLayerGroupCaches);
    }

    private void doBuildLayerGroupCaches() {
        Stopwatch sw = Stopwatch.createStarted();

        groupCache.clear();
        resourceContainmentCache.clear();

        /*
         * fix: make a single pass over the groups in a streaming way
         */
        try (var groups = rawCatalog.list(LayerGroupInfo.class, Filter.INCLUDE)) {
            while (!abort && groups.hasNext()) {
                LayerGroupInfo group = groups.next();
                getGroupData(group);
            }
        }

        sw.stop();
        if (abort) log.info("Layer group containment cache build cancelled after {}", sw);
        else
            log.info(
                    "Built layer group containment cache in {}. Group cache size: {}, resource containment cache size: {} with {} layergroups",
                    sw,
                    groupCache.size(),
                    resourceContainmentCache.size(),
                    resourceContainmentCache.values().stream()
                            .flatMap(Set::stream)
                            .count());
    }

    private LayerGroupSummary createGroupInfo(LayerGroupInfo group, LayerGroupSummary groupData) {
        addGroupInfo(group, groupData);
        registerContainedGroups(group, groupData);
        return groupData;
    }

    private void registerContainedGroups(LayerGroupInfo group, LayerGroupSummary groupData) {
        group.getLayers().stream()
                .filter(IS_GROUP)
                .map(LayerGroupInfo.class::cast)
                .forEach(p -> registerContainerGroup(groupData, p));
    }

    private void registerContainerGroup(LayerGroupSummary container, LayerGroupInfo containedGroup) {
        LayerGroupSummary contained = getGroupData(containedGroup);
        if (container != null && contained != null) {
            contained.containerGroups.add(container);
        }
    }

    private void addGroupInfo(LayerGroupInfo lg, LayerGroupSummary groupData) {
        lg.getLayers().stream().filter(IS_LAYER).map(LayerInfo.class::cast).forEach(p -> addGroupInfo(groupData, p));
    }

    private void addGroupInfo(LayerGroupSummary groupData, LayerInfo containedLayer) {
        String id = containedLayer.getResource().getId();
        Set<LayerGroupSummary> containers = getResourceContainers(id);
        containers.add(groupData);
    }

    private Set<LayerGroupSummary> getResourceContainers(String resourceId) {
        return resourceContainmentCache.computeIfAbsent(resourceId, CONCURRENT_SET_BUILDER);
    }

    /**
     * fix: use computeIfAbsent, {@link #addGroupInfo} and {@link #registerContainedGroups} can
     * hence be called in a single pass
     */
    private LayerGroupSummary getGroupData(LayerGroupInfo lg) {
        LayerGroupSummary groupData = groupCache.get(lg.getId());
        if (null == groupData) {
            groupData = groupCache.computeIfAbsent(lg.getId(), id -> new LayerGroupSummary(lg));
            return createGroupInfo(lg, groupData);
        }
        return groupData;
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
     * Returns all groups containing directly or indirectly the specified group, and relevant for
     * security (e.g., anything but {@link LayerGroupInfo.Mode#SINGLE} ones
     */
    @Override
    public Collection<LayerGroupSummary> getContainerGroupsFor(LayerGroupInfo lg) {
        if (null == lg.getId()) return Set.of();
        LayerGroupSummary summary = getGroupData(lg);
        if (summary != null) {
            Set<LayerGroupSummary> groups = new HashSet<>();
            summary.getContainerGroups().forEach(container -> collectContainers(container, groups));
            return groups;
        }
        return Set.of();
    }

    /**
     * Recursively collects the group and all its containers in the <data>groups</data> collection
     */
    private void collectContainers(LayerGroupSummary lg, Set<LayerGroupSummary> groups) {
        if (!groups.contains(lg)) {
            if (lg.getMode() != LayerGroupInfo.Mode.SINGLE) {
                groups.add(lg);
            }
            lg.containerGroups.forEach(container -> collectContainers(container, groups));
        }
    }

    /**
     * This listener keeps the "layer group" flags in the authorization tree current, in order to
     * optimize the application of layer group containment rules
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
                getGroupData(lg);
            }
        }

        @Override
        public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
            if (event.getSource() instanceof LayerGroupInfo lg) {
                clearGroupInfo(lg);
            }
            // no need to listen to workspace or layer removal, these will cascade to
            // layer groups
        }

        private void clearGroupInfo(LayerGroupInfo lg) {
            final LayerGroupSummary data = groupCache.remove(lg.getId());
            if (data == null) return;
            // clear the resource containment cache
            lg.getLayers().stream()
                    .filter(IS_LAYER)
                    .map(LayerInfo.class::cast)
                    .map(LayerInfo::getResource)
                    .map(ResourceInfo::getId)
                    .forEach(rid -> clearContainment(data, rid));
            // this group does not contain anything anymore, remove from containment
            for (LayerGroupSummary d : groupCache.values()) {
                // will be removed by id based equality
                d.containerGroups.remove(data);
            }
        }

        private void clearContainment(final LayerGroupSummary containerSummary, String resourceId) {
            Set<LayerGroupSummary> containers = resourceContainmentCache.get(resourceId);
            if (containers != null) {
                containers.remove(containerSummary);
                if (containers.isEmpty()) {
                    resourceContainmentCache.remove(resourceId);
                }
            }
        }

        @Override
        public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
            final CatalogInfo source = event.getSource();
            if (source instanceof LayerGroupInfo lg) {
                LayerGroupSummary summary = getGroupData(lg);
                // was the layer group renamed, moved, or its contents changed?
                handleNameChange(event, summary);
                handleWorkspaceChange(event, summary);
                handleLayersChange(event, summary);
                handleModeChange(event, summary);
            } else if (source instanceof WorkspaceInfo) {
                int nameIdx = event.getPropertyNames().indexOf("name");
                if (nameIdx != -1) {
                    String oldName = (String) event.getOldValues().get(nameIdx);
                    String newName = (String) event.getNewValues().get(nameIdx);
                    updateWorkspaceNames(oldName, newName);
                }
            }
        }

        private void handleNameChange(CatalogModifyEvent event, LayerGroupSummary summary) {
            int nameIdx = event.getPropertyNames().indexOf("name");
            if (nameIdx != -1) {
                String newName = (String) event.getNewValues().get(nameIdx);
                summary.name = newName;
            }
        }

        private void handleWorkspaceChange(CatalogModifyEvent event, LayerGroupSummary summary) {
            int wsIdx = event.getPropertyNames().indexOf("workspace");
            if (wsIdx != -1) {
                WorkspaceInfo newWorkspace =
                        (WorkspaceInfo) event.getNewValues().get(wsIdx);
                summary.workspace = newWorkspace == null ? null : newWorkspace.getName();
            }
        }

        @SuppressWarnings("unchecked")
        private void handleLayersChange(CatalogModifyEvent event, LayerGroupSummary summary) {

            int layersIdx = event.getPropertyNames().indexOf("layers");
            if (layersIdx != -1) {
                List<PublishedInfo> oldLayers;
                List<PublishedInfo> newLayers;

                oldLayers = (List<PublishedInfo>) event.getOldValues().get(layersIdx);
                newLayers = (List<PublishedInfo>) event.getNewValues().get(layersIdx);
                updateContainedLayers(summary, oldLayers, newLayers);
            }
        }

        private void handleModeChange(CatalogModifyEvent event, LayerGroupSummary summary) {
            int modeIdx = event.getPropertyNames().indexOf("mode");
            if (modeIdx != -1) {
                Mode newMode = (Mode) event.getNewValues().get(modeIdx);
                summary.mode = newMode;
            }
        }

        private void updateContainedLayers(
                @NonNull LayerGroupSummary groupSummary, List<PublishedInfo> oldLayers, List<PublishedInfo> newLayers) {

            // do not rely on PublishedInfo.equals()...
            var difference = Maps.difference(toIdMap(oldLayers), toIdMap(newLayers));

            Map<String, PublishedInfo> removedLayers = difference.entriesOnlyOnLeft();
            // process layers that are no longer contained
            for (PublishedInfo removed : removedLayers.values()) {
                if (removed instanceof LayerInfo layer) {
                    String resourceId = layer.getResource().getId();
                    clearContainment(groupSummary, resourceId);
                } else if (removed instanceof LayerGroupInfo child) {
                    //// getGroupData(child)
                    LayerGroupSummary summary = groupCache.get(child.getId());
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
                    getResourceContainers(resourceId).add(groupSummary);
                } else if (added instanceof LayerGroupInfo child) {
                    LayerGroupSummary summary = groupCache.get(child.getId());
                    if (summary != null) {
                        summary.containerGroups.add(groupSummary);
                    }
                }
            }
        }

        private Map<String, PublishedInfo> toIdMap(List<PublishedInfo> layers) {
            if (layers.isEmpty()) return Map.of();
            Map<String, PublishedInfo> map = new HashMap<>();
            layers.stream().forEach(l -> map.put(l.getId(), l));
            return map;
        }

        private void updateWorkspaceNames(String oldName, String newName) {
            groupCache.values().stream()
                    .filter(lg -> Objects.equals(lg.workspace, oldName))
                    .forEach(lg -> lg.workspace = newName);
        }

        @Override
        public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
            // nothing to do here

        }
    }
}
