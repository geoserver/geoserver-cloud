/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.pgconfig;

import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.plugin.CatalogPluginStyleResourcePersister;
import org.geoserver.config.GeoServerConfigPersister;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceStore;

/**
 * A {@link GeoServerConfigPersister} alike {@link CatalogListener} to synchronize {@link
 * ResourceStore} resources related to catalog changes.
 *
 * @since 1.8.1
 */
@Slf4j(topic = "org.geoserver.cloud.config.catalog.backend.pgconfig")
public class PgconfigCatalogResourcesSynchronizer implements CatalogListener, ExtensionPriority {

    private GeoServerResourceLoader rl;
    private GeoServerDataDirectory dd;

    public PgconfigCatalogResourcesSynchronizer(GeoServerResourceLoader rl) {
        this.rl = rl;
        this.dd = new GeoServerDataDirectory(rl);
    }

    @Override
    public int getPriority() {
        return ExtensionPriority.HIGHEST;
    }

    @Override
    public void reloaded() {
        // no-op
    }

    /**
     * No-op, {@link GeoServerConfigPersister} would persist the xml files here, pgconfig doesn't do
     * that
     */
    @Override
    public void handleAddEvent(CatalogAddEvent event) {
        // no-op
    }

    /**
     * No-op, {@link GeoServerConfigPersister} would persist the xml files here, pgconfig doesn't do
     * that.
     */
    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) {
        // no-op
    }

    /**
     * @throws CatalogException
     */
    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) {
        final Object source = event.getSource();
        if (source instanceof WorkspaceInfo ws) {
            log.debug("Removing workspace {}", ws.getName());
            rmRes(dd.get(ws));
        } else if (source instanceof NamespaceInfo) {
            // do not remove the directory, only when removing the workspace
        } else if (source instanceof StoreInfo store) {
            log.debug("Removing datastore {}", store.getName());
            rmRes(dd.get(store));
        } else if (source instanceof ResourceInfo r) {
            log.debug("Removing ResourceInfo {}", r.getName());
            rmRes(dd.get(r));
        } else if (source instanceof LayerInfo l) {
            log.debug("Removing layer {}", l.getName());
            rmRes(dd.get(l));
        } else if (source instanceof LayerGroupInfo lg) {
            log.debug("Removing layer group " + lg.getName());
            Resource directory = dd.get(lg);
            boolean exists = directory.getType() == Type.DIRECTORY;
            if (exists && directory.list().isEmpty()) {
                rmRes(directory);
            }
        }
    }

    /**
     * @throws CatalogException
     */
    @Override
    public void handleModifyEvent(CatalogModifyEvent event) {
        event = CatalogPluginStyleResourcePersister.withRealSource(event);
        final Object source = event.getSource();
        // here we handle name changes
        handleRenames(event);

        // handle the case of a store changing workspace
        if (source instanceof StoreInfo s) {
            handleWorkspaceChange(event, s);
        } else if (source instanceof FeatureTypeInfo ft) {
            handleStoreChange(event, ft);
        }
    }

    private void handleWorkspaceChange(CatalogModifyEvent event, StoreInfo s) {
        final int i = event.getPropertyNames().indexOf("workspace");
        if (i > -1) {
            WorkspaceInfo newWorkspace = (WorkspaceInfo) event.getNewValues().get(i);
            Resource oldDir = dd.get(s);
            moveResToDir(oldDir, dd.get(newWorkspace));
        }
    }

    private void handleStoreChange(CatalogModifyEvent event, ResourceInfo resourceInfo) {
        final int i = event.getPropertyNames().indexOf("store");
        if (i > -1) {
            StoreInfo newStore = (StoreInfo) event.getNewValues().get(i);
            Resource oldDir = dd.get(resourceInfo);
            Resource newDir = dd.get(newStore);
            moveResToDir(oldDir, newDir);
        }
    }

    private void handleRenames(CatalogModifyEvent event) {
        final int i = event.getPropertyNames().indexOf("name");
        if (i > -1) {
            String newName = (String) event.getNewValues().get(i);
            var source = event.getSource();
            if (source instanceof WorkspaceInfo ws) {
                renameWorkspace(ws, newName);
            } else if (source instanceof StoreInfo s) {
                renameStore(s, newName);
            } else if (source instanceof ResourceInfo r) {
                renameResource(r, newName);
            }
        }
    }

    private void renameWorkspace(WorkspaceInfo ws, String newName) {
        log.debug("Renaming workspace {} to {}", ws.getName(), newName);
        renameRes(dd.get(ws), newName);
    }

    private void renameStore(StoreInfo s, String newName) {
        log.debug("Renaming store {} to {}", s.getName(), newName);
        renameRes(dd.get(s), newName);
    }

    private void renameResource(ResourceInfo r, String newName) {
        log.debug("Renaming resource {} to {}", r.getName(), newName);
        renameRes(dd.get(r), newName);
    }

    private void rmRes(Resource r) {
        try {
            rl.remove(r.path());
        } catch (Exception e) {
            throw new CatalogException(e);
        }
    }

    private void renameRes(Resource r, String newName) {
        try {
            rl.move(r.path(), r.parent().get(newName).path());
        } catch (Exception e) {
            throw new CatalogException(e);
        }
    }

    private void moveResToDir(Resource r, Resource newDir) {
        try {
            rl.move(r.path(), newDir.get(r.name()).path());
        } catch (Exception e) {
            throw new CatalogException(e);
        }
    }
}
