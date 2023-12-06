/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.locking;

import static org.geoserver.catalog.plugin.locking.LockingSupport.nameOf;
import static org.geoserver.catalog.plugin.locking.LockingSupport.typeOf;

import static java.lang.String.format;

import lombok.NonNull;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LockingCatalogFacade;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogPlugin;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A locking catalog, akin to {@link LockingCatalogFacade}, but at a higher level of granularity,
 * because a single catalog operation (e.g. save()), can imply multiple config changes (e.g. sets
 * the default workspace/namespace/datastore), and we want all those automatic batch operations to
 * run inside the same lock, which is not possible with {@link LockingCatalogFacade}.
 *
 * <p>The consistency guarantees are the ones provided by the {@link GeoServerConfigurationLock},
 * whether it guarantees atomicity at the JVM or cluster-wide level.
 *
 * <p>This {@link CatalogPlugin} implementation overrides all mutating methods to run inside a
 * cluster-wide lock. For instance, {@link #doAdd}, {@link #doSave}, {@link #doRemove}, and all the
 * {@code setDefault*} methods.
 */
@SuppressWarnings("serial")
public class LockingCatalog extends CatalogPlugin {

    private final transient GeoServerConfigurationLock configurationLock;
    private transient LockingSupport locking;

    public LockingCatalog(@NonNull GeoServerConfigurationLock configurationLock) {
        super();
        this.configurationLock = configurationLock;
        enableLocking();
    }

    public LockingCatalog(@NonNull GeoServerConfigurationLock configurationLock, boolean isolated) {
        super(isolated);
        this.configurationLock = configurationLock;
        enableLocking();
    }

    public LockingCatalog(
            @NonNull GeoServerConfigurationLock configurationLock, CatalogFacade facade) {
        super(facade);
        this.configurationLock = configurationLock;
        enableLocking();
    }

    public LockingCatalog(
            @NonNull GeoServerConfigurationLock configurationLock,
            CatalogFacade facade,
            boolean isolated) {
        super(facade, isolated);
        this.configurationLock = configurationLock;
        enableLocking();
    }

    public void enableLocking() {
        this.locking = LockingSupport.locking(configurationLock);
    }

    public void disableLocking() {
        this.locking = LockingSupport.ignoringLocking();
    }

    @Override
    public void setDefaultDataStore(@NonNull WorkspaceInfo workspace, DataStoreInfo store) {
        locking.runInWriteLock(
                () -> super.setDefaultDataStore(workspace, store),
                format(
                        "setDefaultDataStore(%s, %s)",
                        workspace.getName(), store == null ? "null" : store.getName()));
    }

    @Override
    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        locking.runInWriteLock(
                () -> super.setDefaultNamespace(defaultNamespace),
                format(
                        "setDefaultNamespace(%s)",
                        defaultNamespace == null ? "null" : defaultNamespace.getName()));
    }

    @Override
    public void setDefaultWorkspace(WorkspaceInfo defaultWorkspace) {
        locking.runInWriteLock(
                () -> super.setDefaultWorkspace(defaultWorkspace),
                format(
                        "setDefaultWorkspace(%s)",
                        defaultWorkspace == null ? "null" : defaultWorkspace.getName()));
    }

    /** {@inheritDoc} */
    protected @Override <T extends CatalogInfo> void doAdd(T info, Function<T, T> inserter) {
        locking.runInWriteLock(
                () -> super.doAdd(info, inserter),
                format("add(%s[%s])", typeOf(info), nameOf(info)));
    }

    /** {@inheritDoc} */
    protected @Override <I extends CatalogInfo> void doSave(final I info) {
        locking.runInWriteLock(
                () -> super.doSave(info), format("save(%s[%s])", typeOf(info), nameOf(info)));
    }

    /** {@inheritDoc} */
    protected @Override <T extends CatalogInfo> void doRemove(T info, Consumer<T> remover) {
        locking.runInWriteLock(
                () -> super.doRemove(info, remover),
                format("remove(%s[%s])", typeOf(info), nameOf(info)));
    }

    // TODO: Remove once CatalogPlugin moves the namespace update logic to
    // validationrules.onBefore/AfterSave and just call doSave(store)
    @Override
    public void save(StoreInfo store) {
        locking.runInWriteLock(
                () -> super.save(store), format("save(%s[%s])", typeOf(store), nameOf(store)));
    }
}
