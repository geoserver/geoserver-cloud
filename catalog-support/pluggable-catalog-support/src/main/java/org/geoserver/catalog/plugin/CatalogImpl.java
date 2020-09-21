/*
 * (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LockingCatalogFacade;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;

/**
 * Alternative to {@link org.geoserver.catalog.impl.CatalogImpl} to improve separation of concerns
 * between levels of abstractions and favor plug-ability of the underlying object store.
 *
 * <p>
 *
 * <ul>
 *   <li>Allows decorating the {@link CatalogFacade} with an {@link IsolatedCatalogFacade} when
 *       {@link #setFacade} is called, instead of only on the default constructor
 *   <li>Requires the {@code CatalogFacade} to derive from {@link
 *       org.geoserver.catalog.plugin.AbstractCatalogFacade}, whose underlying storage is abstracted
 *       out to {@link CatalogInfoRepository}
 *   <li>Uses {@link org.geoserver.catalog.plugin.DefaultCatalogFacade} as the default facade
 *       implementation.
 *   <li>Implements all business-logic, like event handling and ensuring no {@link CatalogInfo}
 *       instance gets in or out of the {@link Catalog} without being decorated with a {@link
 *       ModificationProxy}, releaving the lower-level {@link CatalogFacade} abstraction of such
 *       things. Hence {@link AbstractCatalogFacade} works on plain POJOS, or whatever is supplied
 *       by its {@link CatalogInfoRepository repositories}, though in practice it can only be
 *       implementations of {@code org.geoserver.catalog.impl} due to coupling in other areas. Of
 *       special interest is the use of {@link PropertyDiff} and {@link Patch} on all the {@link
 *       #save} methods, in order to keep the {@code ModificationProxy} logic local to this catalog
 *       implementation, and call {@link AbstractCatalogFacade#update(CatalogInfo, Patch)} instead
 * </ul>
 */
@SuppressWarnings("serial")
public class CatalogImpl extends org.geoserver.catalog.impl.CatalogImpl {
    private static final Logger LOGGER = Logging.getLogger(CatalogImpl.class);

    /** Hold on to the raw facade */
    private @Getter @NonNull AbstractCatalogFacade rawCatalogFacade;

    private final boolean isolated;

    /**
     * Flag to overcome the superclass' default constructor setting an illegal facade type. Set to
     * true once this class' constructor is reached
     */
    private boolean checkFacadeImplementation = false;

    public static org.geoserver.catalog.impl.CatalogImpl nonIsolated(
            CatalogFacade catalogFacadeImpl) {
        return new CatalogImpl(catalogFacadeImpl, false);
    }

    public static org.geoserver.catalog.impl.CatalogImpl isoLated(CatalogFacade catalogFacadeImpl) {
        return new CatalogImpl(catalogFacadeImpl, true);
    }

    public CatalogImpl() {
        this(new org.geoserver.catalog.plugin.DefaultCatalogFacade());
    }

    public CatalogImpl(CatalogFacade rawCatalogFacade) {
        this(rawCatalogFacade, true);
    }

    private CatalogImpl(CatalogFacade rawCatalogFacade, boolean isolated) {
        Objects.requireNonNull(rawCatalogFacade);
        this.checkFacadeImplementation = true;
        this.isolated = isolated;
        setFacade(rawCatalogFacade);
        // just to stress out the parent's default constructor is called nonetheless and we need to
        // completely replace its facade
        Objects.requireNonNull(super.resourcePool);
    }

    public @Override void setFacade(CatalogFacade facade) {
        Objects.requireNonNull(facade);
        if (checkFacadeImplementation) {
            if (!(facade instanceof AbstractCatalogFacade)) {
                throw new IllegalArgumentException(
                        "This implementation requires a subclass of org.geoserver.catalog.plugin.AbstractCatalogFacade");
            }
            this.rawCatalogFacade = (AbstractCatalogFacade) facade;
        }
        final GeoServerConfigurationLock configurationLock =
                GeoServerExtensions.bean(GeoServerConfigurationLock.class);
        if (configurationLock != null) {
            facade = LockingCatalogFacade.create(facade, configurationLock);
        }
        // wrap the default catalog facade with the facade capable of handling isolated workspaces
        // behavior
        this.facade = isolated ? new IsolatedCatalogFacade(facade) : facade;
        facade.setCatalog(this);
    }

    protected <I extends CatalogInfo> void doSave(final I proxy) {
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(proxy);
        // figure out what changed
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();

        // this could be the event's payload instead of three separate lists
        PropertyDiff diff = PropertyDiff.valueOf(propertyNames, oldValues, newValues);
        Patch patch = diff.toPatch();

        // use the proxy, may some listener update it
        fireModified(proxy, propertyNames, oldValues, newValues);

        // pass on the un-modified object and the patch
        I real = ModificationProxy.unwrap(proxy);
        real = rawCatalogFacade.update(real, patch);

        Class<I> type = ClassMappings.fromImpl(real.getClass()).getInterface();
        firePostModified(ModificationProxy.create(real, type), propertyNames, oldValues, newValues);
    }

    public @Override void save(LayerGroupInfo layerGroup) {
        validate(layerGroup, false);
        doSave(layerGroup);
    }

    public @Override void save(LayerInfo layer) {
        validate(layer, false);
        doSave(layer);
    }

    public @Override void save(MapInfo map) {
        doSave(map);
    }

    public @Override void save(NamespaceInfo namespace) {
        validate(namespace, false);
        doSave(namespace);
    }

    public @Override void save(ResourceInfo resource) {
        validate(resource, false);
        doSave(resource);
    }

    public @Override void save(StoreInfo store) {
        // really?
        // if (store.getId() == null) {
        // // add it instead of saving
        // add(store);
        // return;
        // }
        validate(store, false);
        doSave(store);
    }

    public @Override void save(WorkspaceInfo workspace) {
        validate(workspace, false);
        doSave(workspace);
    }

    public @Override void save(StyleInfo style) {
        validate(style, false);

        final boolean renamed = renameStyle(style);
        try {
            doSave(style);
        } catch (RuntimeException e) {
            if (renamed) {
                revertRenameStyle(style);
            }
            throw e;
        }
    }

    private void revertRenameStyle(StyleInfo style) {
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(style);
        final int i = h.getPropertyNames().indexOf("name");
        String oldName = (String) h.getOldValues().get(i);
        if (oldName != null) {
            renameStyle(style, oldName);
        }
    }

    private boolean renameStyle(StyleInfo style) {
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(style);
        final int i = h.getPropertyNames().indexOf("name");
        if (i > -1 && !h.getNewValues().get(i).equals(h.getOldValues().get(i))) {
            String newName = (String) h.getNewValues().get(i);
            renameStyle(style, newName);
            return true;
        }
        return false;
    }

    // copy private method
    private void renameStyle(StyleInfo s, String newName) {
        // rename style definition file
        Resource style = new GeoServerDataDirectory(resourceLoader).style(s);
        StyleHandler format = Styles.handler(s.getFormat());
        try {
            Resource target = Resources.uniqueResource(style, newName, format.getFileExtension());
            style.renameTo(target);
            s.setFilename(target.name());

            // rename generated sld if appropriate
            if (!SLDHandler.FORMAT.equals(format.getFormat())) {
                Resource sld = style.parent().get(FilenameUtils.getBaseName(style.name()) + ".sld");
                if (sld.getType() == Type.RESOURCE) {
                    LOGGER.fine("Renaming style resource " + s.getName() + " to " + newName);

                    Resource generated = Resources.uniqueResource(sld, newName, "sld");
                    sld.renameTo(generated);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to rename style file along with name.", e);
        }
    }

    public <C extends CatalogInfo> C get(String id, Class<? extends C> type) {
        if (WorkspaceInfo.class.isAssignableFrom(type)) return type.cast(getWorkspace(id));
        if (NamespaceInfo.class.isAssignableFrom(type)) return type.cast(getNamespace(id));
        if (StoreInfo.class.isAssignableFrom(type))
            return type.cast(getStore(id, (Class<? extends StoreInfo>) type));
        if (ResourceInfo.class.isAssignableFrom(type))
            return type.cast(getResource(id, (Class<? extends ResourceInfo>) type));
        if (LayerInfo.class.isAssignableFrom(type)) return type.cast(getLayer(id));
        if (LayerGroupInfo.class.isAssignableFrom(type)) return type.cast(getLayerGroup(id));
        if (StyleInfo.class.isAssignableFrom(type)) return type.cast(getStyle(id));
        if (MapInfo.class.isAssignableFrom(type)) return type.cast(getMap(id));
        throw new IllegalArgumentException("unknown CatalogInfo class: " + type.getCanonicalName());
    }
}
