/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.locking;

import static org.geoserver.catalog.plugin.locking.LockingSupport.nameOf;
import static org.geoserver.catalog.plugin.locking.LockingSupport.typeOf;

import java.util.function.UnaryOperator;
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

/**
 * A locking {@link CatalogPlugin} that ensures cluster-wide catalog updates safety using a
 * {@link GeoServerConfigurationLock}.
 *
 * <p>This class enhances {@link CatalogPlugin} by overriding mutating methods to run within a
 * {@link GeoServerConfigurationLock}, which in GeoServer Cloud is required to provide cluster-level
 * locking. This offers higher granularity than {@link LockingCatalogFacade}, ensuring that batch
 * operations (e.g., {@code save()} affecting multiple config changes like setting defaults) execute
 * atomically under the same lock. The consistency guarantees depend on the lock’s scope, typically
 * cluster-wide in GeoServer Cloud.
 *
 * <p>Key mutating methods overridden include {@link #doAdd}, {@link #doSave}, {@link #doRemove}, and
 * default-setting methods like {@link #setDefaultWorkspace}. Locking can be enabled or disabled via
 * {@link #enableLocking()} and {@link #disableLocking()}.
 *
 * <p>Example usage:
 * <pre>
 * GeoServerConfigurationLock lock = ...;
 * LockingCatalog catalog = new LockingCatalog(lock);
 * catalog.add(new WorkspaceInfoImpl()); // Runs within cluster-wide lock
 * </pre>
 *
 * @since 1.0
 * @see GeoServerConfigurationLock
 * @see CatalogPlugin
 * @see LockingCatalogFacade
 */
@SuppressWarnings("serial")
public class LockingCatalog extends CatalogPlugin {

    private final transient GeoServerConfigurationLock configurationLock;
    private transient LockingSupport locking;

    /**
     * Constructs a locking catalog with a configuration lock.
     *
     * @param configurationLock The {@link GeoServerConfigurationLock} for cluster-wide safety; must not be null.
     * @throws NullPointerException if {@code configurationLock} is null.
     */
    public LockingCatalog(@NonNull GeoServerConfigurationLock configurationLock) {
        super();
        this.configurationLock = configurationLock;
        enableLocking();
    }

    /**
     * Constructs a locking catalog with a configuration lock and isolation option.
     *
     * @param configurationLock The {@link GeoServerConfigurationLock} for cluster-wide safety; must not be null.
     * @param isolated Whether the catalog operates in isolated mode.
     * @throws NullPointerException if {@code configurationLock} is null.
     */
    public LockingCatalog(@NonNull GeoServerConfigurationLock configurationLock, boolean isolated) {
        super(isolated);
        this.configurationLock = configurationLock;
        enableLocking();
    }

    /**
     * Constructs a locking catalog with a configuration lock and custom facade.
     *
     * @param configurationLock The {@link GeoServerConfigurationLock} for cluster-wide safety; must not be null.
     * @param facade The {@link CatalogFacade} to use; may be null.
     * @throws NullPointerException if {@code configurationLock} is null.
     */
    public LockingCatalog(@NonNull GeoServerConfigurationLock configurationLock, CatalogFacade facade) {
        super(facade);
        this.configurationLock = configurationLock;
        enableLocking();
    }

    /**
     * Constructs a locking catalog with a configuration lock, custom facade, and isolation option.
     *
     * @param configurationLock The {@link GeoServerConfigurationLock} for cluster-wide safety; must not be null.
     * @param facade The {@link CatalogFacade} to use; may be null.
     * @param isolated Whether the catalog operates in isolated mode.
     * @throws NullPointerException if {@code configurationLock} is null.
     */
    public LockingCatalog(
            @NonNull GeoServerConfigurationLock configurationLock, CatalogFacade facade, boolean isolated) {
        super(facade, isolated);
        this.configurationLock = configurationLock;
        enableLocking();
    }

    /**
     * Enables locking for all mutating operations using the configured {@link GeoServerConfigurationLock}.
     */
    public void enableLocking() {
        this.locking = LockingSupport.locking(configurationLock);
    }

    /**
     * Disables locking, bypassing the {@link GeoServerConfigurationLock} for mutating operations.
     */
    public void disableLocking() {
        this.locking = LockingSupport.ignoringLocking();
    }

    /**
     * {@inheritDoc}
     * <p>Runs within a cluster-wide write lock to ensure update safety across related config changes.
     */
    @Override
    public void setDefaultDataStore(@NonNull WorkspaceInfo workspace, DataStoreInfo store) {
        locking.runInWriteLock(
                () -> super.setDefaultDataStore(workspace, store),
                "setDefaultDataStore(%s, %s)".formatted(workspace.getName(), store == null ? "null" : store.getName()));
    }

    /**
     * {@inheritDoc}
     * <p>Runs within a cluster-wide write lock to ensure update safety across related config changes.
     */
    @Override
    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        locking.runInWriteLock(
                () -> super.setDefaultNamespace(defaultNamespace),
                "setDefaultNamespace(%s)".formatted(defaultNamespace == null ? "null" : defaultNamespace.getName()));
    }

    /**
     * {@inheritDoc}
     * <p>Runs within a cluster-wide write lock to ensure update safety across related config changes.
     */
    @Override
    public void setDefaultWorkspace(WorkspaceInfo defaultWorkspace) {
        locking.runInWriteLock(
                () -> super.setDefaultWorkspace(defaultWorkspace),
                "setDefaultWorkspace(%s)".formatted(defaultWorkspace == null ? "null" : defaultWorkspace.getName()));
    }

    /**
     * {@inheritDoc}
     * <p>Runs within a cluster-wide write lock to ensure update safety for addition.
     */
    protected @Override <T extends CatalogInfo> void doAdd(T info, UnaryOperator<T> inserter) {
        locking.runInWriteLock(() -> super.doAdd(info, inserter), "add(%s[%s])".formatted(typeOf(info), nameOf(info)));
    }

    /**
     * {@inheritDoc}
     * <p>Runs within a cluster-wide write lock to ensure update safety for saving.
     */
    protected @Override <I extends CatalogInfo> void doSave(final I info) {
        locking.runInWriteLock(() -> super.doSave(info), "save(%s[%s])".formatted(typeOf(info), nameOf(info)));
    }

    /**
     * {@inheritDoc}
     * <p>Runs within a cluster-wide write lock to ensure update safety for removal.
     */
    protected @Override <T extends CatalogInfo> void doRemove(T info, Class<T> type) {
        locking.runInWriteLock(
                () -> super.doRemove(info, type), "remove(%s[%s])".formatted(typeOf(info), nameOf(info)));
    }

    /**
     * Overrides to call {@code super.save(StoreInfo)} instead of {@code super.doSave()} because
     * {@code CatalogPlugin} performs additional logic.
     * <p>Runs within a cluster-wide write lock to ensure update safety for saving store information.
     *
     * @param store The {@link StoreInfo} to save; must not be null.
     */
    @Override
    public void save(StoreInfo store) {
        locking.runInWriteLock(() -> super.save(store), "save(%s[%s])".formatted(typeOf(store), nameOf(store)));
    }
}
