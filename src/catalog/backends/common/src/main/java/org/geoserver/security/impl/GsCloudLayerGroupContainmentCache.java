/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.security.impl;

import com.google.common.base.Stopwatch;

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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
public class GsCloudLayerGroupContainmentCache extends LayerGroupContainmentCache
        implements ApplicationContextAware {

    private Catalog catalog;
    private ApplicationContext applicationContext;

    public GsCloudLayerGroupContainmentCache(Catalog rawCatalog) {
        /*
         * fix: constructor calls buildLayerGroupCaches, give it an empty catalog,
         * we override everything here, and avoid building the cache twice,
         * on the constructor and on the app context event
         */
        super(new CatalogImpl());
        this.catalog = rawCatalog;
        catalog.addListener(new CatalogChangeListener());
        catalog.addListener(new LayerGroupStyleListener());
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
        Stopwatch sw = Stopwatch.createStarted();
        buildLayerGroupCaches();
        sw.stop();
        log.info(
                "Built layer group containment cache in {}. Group cache size: {}, resource containment cache size: {} with {} layergroups",
                sw,
                groupCache.size(),
                resourceContainmentCache.size(),
                resourceContainmentCache.values().stream().flatMap(Set::stream).count());
    }

    private void buildLayerGroupCaches() {
        groupCache.clear();
        resourceContainmentCache.clear();

        /*
         * fix: make a single pass over the groups in a streaming way
         */
        try (var groups = catalog.list(LayerGroupInfo.class, Filter.INCLUDE)) {

            while (groups.hasNext()) {
                LayerGroupInfo group = groups.next();
                addGroupInfo(group);
                registerContainedGroups(group);
            }
        }
    }

    private void registerContainedGroups(LayerGroupInfo lg) {
        lg.getLayers().stream()
                .filter(IS_GROUP)
                .map(LayerGroupInfo.class::cast)
                .forEach(
                        p -> {
                            LayerGroupSummary container = getGroupData(lg);
                            LayerGroupSummary contained = getGroupData(p);
                            if (container != null && contained != null) {
                                contained.containerGroups.add(container);
                            }
                        });
    }

    private void addGroupInfo(LayerGroupInfo lg) {
        LayerGroupSummary groupData = getGroupData(lg);
        lg.getLayers().stream()
                .filter(IS_LAYER)
                .map(LayerInfo.class::cast)
                .forEach(
                        p -> {
                            String id = p.getResource().getId();
                            Set<LayerGroupSummary> containers =
                                    resourceContainmentCache.computeIfAbsent(
                                            id, CONCURRENT_SET_BUILDER);
                            containers.add(groupData);
                        });
    }

    /*
     * fix: use computeIfAbsent, addGroupInfo and registerContainedGroups can hence be called in a single pass
     */
    private LayerGroupSummary getGroupData(LayerGroupInfo lg) {
        return groupCache.computeIfAbsent(lg.getId(), id -> new LayerGroupSummary(lg));
    }

    private void clearGroupInfo(LayerGroupInfo lg) {
        LayerGroupSummary data = groupCache.remove(lg.getId());
        // clear the resource containment cache
        lg.getLayers().stream()
                .filter(IS_LAYER)
                .forEach(
                        p -> {
                            String rid = ((LayerInfo) p).getResource().getId();
                            synchronized (rid) {
                                Set<LayerGroupSummary> containers =
                                        resourceContainmentCache.get(rid);
                                if (containers != null) {
                                    containers.remove(data);
                                }
                            }
                        });
        // this group does not contain anything anymore, remove from containment
        for (LayerGroupSummary d : groupCache.values()) {
            // will be removed by equality
            d.containerGroups.remove(new LayerGroupSummary(lg));
        }
    }

    /** Returns all groups containing directly or indirectly containing the resource */
    @Override
    public Collection<LayerGroupSummary> getContainerGroupsFor(ResourceInfo resource) {
        String id = resource.getId();
        Set<LayerGroupSummary> groups = resourceContainmentCache.get(id);
        if (groups == null) {
            return Collections.emptyList();
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
        String id = lg.getId();
        if (id == null) {
            return Collections.emptyList();
        }
        LayerGroupSummary summary = groupCache.get(id);
        if (summary == null) {
            return Collections.emptyList();
        }

        Set<LayerGroupSummary> result = new HashSet<>();
        for (LayerGroupSummary container : summary.getContainerGroups()) {
            collectContainers(container, result);
        }
        return result;
    }

    /**
     * Recursively collects the group and all its containers in the <data>groups</data> collection
     */
    private void collectContainers(LayerGroupSummary lg, Set<LayerGroupSummary> groups) {
        if (!groups.contains(lg)) {
            if (lg.getMode() != LayerGroupInfo.Mode.SINGLE) {
                groups.add(lg);
            }
            for (LayerGroupSummary container : lg.containerGroups) {
                collectContainers(container, groups);
            }
        }
    }

    /**
     * This listener keeps the "layer group" flags in the authorization tree current, in order to
     * optimize the application of layer group containment rules
     */
    final class CatalogChangeListener implements CatalogListener {

        @Override
        public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
            if (event.getSource() instanceof LayerGroupInfo) {
                LayerGroupInfo lg = (LayerGroupInfo) event.getSource();
                addGroupInfo(lg);
                registerContainedGroups(lg);
            }
        }

        @Override
        public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
            if (event.getSource() instanceof LayerGroupInfo) {
                LayerGroupInfo lg = (LayerGroupInfo) event.getSource();
                clearGroupInfo(lg);
            }
            // no need to listen to workspace or layer removal, these will cascade to
            // layer groups
        }

        @Override
        public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
            final CatalogInfo source = event.getSource();
            if (source instanceof LayerGroupInfo) {
                LayerGroupInfo lg = (LayerGroupInfo) event.getSource();
                // was the layer group renamed, moved, or its contents changed?
                int nameIdx = event.getPropertyNames().indexOf("name");
                if (nameIdx != -1) {
                    String newName = (String) event.getNewValues().get(nameIdx);
                    updateGroupName(lg.getId(), newName);
                }
                int wsIdx = event.getPropertyNames().indexOf("workspace");
                if (wsIdx != -1) {
                    WorkspaceInfo newWorkspace = (WorkspaceInfo) event.getNewValues().get(wsIdx);
                    updateGroupWorkspace(lg.getId(), newWorkspace);
                }
                int layerIdx = event.getPropertyNames().indexOf("layers");
                if (layerIdx != -1) {
                    @SuppressWarnings("unchecked")
                    List<PublishedInfo> oldLayers =
                            (List<PublishedInfo>) event.getOldValues().get(layerIdx);
                    @SuppressWarnings("unchecked")
                    List<PublishedInfo> newLayers =
                            (List<PublishedInfo>) event.getNewValues().get(layerIdx);
                    updateContainedLayers(groupCache.get(lg.getId()), oldLayers, newLayers);
                }
                int modeIdx = event.getPropertyNames().indexOf("mode");
                if (modeIdx != -1) {
                    Mode newMode = (Mode) event.getNewValues().get(modeIdx);
                    updateGroupMode(lg.getId(), newMode);
                }
            } else if (source instanceof WorkspaceInfo) {
                int nameIdx = event.getPropertyNames().indexOf("name");
                if (nameIdx != -1) {
                    String oldName = (String) event.getOldValues().get(nameIdx);
                    String newName = (String) event.getNewValues().get(nameIdx);
                    updateWorkspaceNames(oldName, newName);
                }
            }
        }

        private void updateGroupMode(String id, Mode newMode) {
            LayerGroupSummary summary = groupCache.get(id);
            summary.mode = newMode;
        }

        private void updateContainedLayers(
                LayerGroupSummary groupSummary,
                List<PublishedInfo> oldLayers,
                List<PublishedInfo> newLayers) {

            // process layers that are no more contained
            final HashSet<PublishedInfo> removedLayers = new HashSet<>(oldLayers);
            removedLayers.removeAll(newLayers);
            for (PublishedInfo removed : removedLayers) {
                if (removed instanceof LayerInfo) {
                    String resourceId = ((LayerInfo) removed).getResource().getId();
                    Set<LayerGroupSummary> containers = resourceContainmentCache.get(resourceId);
                    if (containers != null) {
                        synchronized (resourceId) {
                            containers.remove(groupSummary);
                            if (containers.isEmpty()) {
                                resourceContainmentCache.remove(resourceId, containers);
                            }
                        }
                    }
                } else {
                    LayerGroupInfo child = (LayerGroupInfo) removed;
                    LayerGroupSummary summary = groupCache.get(child.getId());
                    if (summary != null) {
                        summary.containerGroups.remove(groupSummary);
                    }
                }
            }

            // add the layers that are newly contained
            final HashSet<PublishedInfo> addedLayers = new HashSet<>(newLayers);
            addedLayers.removeAll(oldLayers);
            for (PublishedInfo added : addedLayers) {
                if (added instanceof LayerInfo) {
                    String resourceId = ((LayerInfo) added).getResource().getId();
                    synchronized (resourceId) {
                        Set<LayerGroupSummary> containers =
                                resourceContainmentCache.computeIfAbsent(
                                        resourceId, CONCURRENT_SET_BUILDER);
                        containers.add(groupSummary);
                    }
                } else {
                    LayerGroupInfo child = (LayerGroupInfo) added;
                    LayerGroupSummary summary = groupCache.get(child.getId());
                    if (summary != null) {
                        summary.containerGroups.add(groupSummary);
                    }
                }
            }
        }

        private void updateGroupWorkspace(String id, WorkspaceInfo newWorkspace) {
            LayerGroupSummary summary = groupCache.get(id);
            if (summary != null) {
                summary.workspace = newWorkspace == null ? null : newWorkspace.getName();
            }
        }

        private void updateGroupName(String id, String newName) {
            LayerGroupSummary summary = groupCache.get(id);
            if (summary != null) {
                summary.name = newName;
            }
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

        @Override
        public void reloaded() {
            // rebuild the containment cache
            buildLayerGroupCaches();
        }
    }
}
