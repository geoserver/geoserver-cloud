/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.locking;

import static org.geoserver.catalog.plugin.locking.LockingSupport.nameOf;
import static org.geoserver.catalog.plugin.locking.LockingSupport.typeOf;

import lombok.NonNull;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.plugin.GeoServerImpl;

/**
 * A locking {@link GeoServerImpl} that ensures cluster-wide configuration updates safety using a
 * {@link GeoServerConfigurationLock}.
 *
 * <p>This class enhances {@link GeoServerImpl} by overriding mutating methods to run within a
 * {@link GeoServerConfigurationLock}, which in GeoServer Cloud is required to provide cluster-level
 * locking. This ensures that configuration changes (e.g., adding or saving services, settings) are
 * executed atomically across the cluster, preventing concurrent modification issues.
 *
 * <p>Key mutating methods overridden include {@link #setGlobal}, {@link #save}, {@link #add}, and
 * {@link #remove} for various configuration objects. Locking can be enabled or disabled via
 * {@link #enableLocking()} and {@link #disableLocking()}.
 *
 * <p>Example usage:
 * <pre>
 * GeoServerConfigurationLock lock = ...;
 * LockingGeoServer geoServer = new LockingGeoServer(lock);
 * geoServer.add(new SettingsInfoImpl()); // Runs within cluster-wide lock
 * </pre>
 *
 * @since 1.0
 * @see GeoServerConfigurationLock
 * @see GeoServerImpl
 */
public class LockingGeoServer extends GeoServerImpl {

    private LockingSupport lockingSupport;
    private final GeoServerConfigurationLock configurationLock;

    /**
     * Constructs a locking GeoServer with a configuration lock.
     *
     * @param locking The {@link GeoServerConfigurationLock} for cluster-wide safety; must not be null.
     * @throws NullPointerException if {@code locking} is null.
     */
    public LockingGeoServer(@NonNull GeoServerConfigurationLock locking) {
        super();
        this.configurationLock = locking;
        enableLocking();
    }

    /**
     * Constructs a locking GeoServer with a configuration lock and custom facade.
     *
     * @param locking The {@link GeoServerConfigurationLock} for cluster-wide safety; must not be null.
     * @param facade The {@link GeoServerFacade} to use; must not be null.
     * @throws NullPointerException if {@code locking} or {@code facade} is null.
     */
    public LockingGeoServer(@NonNull GeoServerConfigurationLock locking, @NonNull GeoServerFacade facade) {
        super(facade);
        this.configurationLock = locking;
        enableLocking();
    }

    /**
     * Enables locking for all mutating operations using the configured {@link GeoServerConfigurationLock}.
     */
    public void enableLocking() {
        this.lockingSupport = LockingSupport.locking(configurationLock);
    }

    /**
     * Disables locking, bypassing the {@link GeoServerConfigurationLock} for mutating operations.
     */
    public void disableLocking() {
        this.lockingSupport = LockingSupport.ignoringLocking();
    }

    /**
     * Returns the configuration lock used for cluster-wide safety.
     *
     * @return The {@link GeoServerConfigurationLock}; never null.
     */
    public @NonNull GeoServerConfigurationLock getConfigurationLock() {
        return this.configurationLock;
    }

    /**
     * {@inheritDoc}
     * <p>Runs within a cluster-wide write lock to ensure update safety for global configuration.
     */
    @Override
    public void setGlobal(GeoServerInfo global) {
        lockingSupport.runInWriteLock(() -> super.setGlobal(global), "setGlobal(%s)".formatted(nameOf(global)));
    }

    /**
     * {@inheritDoc}
     * <p>Runs within a cluster-wide write lock to ensure update safety for global configuration.
     */
    @Override
    public void save(GeoServerInfo geoServer) {
        lockingSupport.runInWriteLock(() -> super.save(geoServer), "save(%s)".formatted(nameOf(geoServer)));
    }

    /**
     * {@inheritDoc}
     * <p>Runs within a cluster-wide write lock to ensure update safety for adding settings.
     */
    @Override
    public void add(SettingsInfo settings) {
        lockingSupport.runInWriteLock(
                () -> super.add(settings), "add(%s[%s])".formatted(typeOf(settings), nameOf(settings)));
    }

    /**
     * {@inheritDoc}
     * <p>Runs within a cluster-wide write lock to ensure update safety for saving settings.
     */
    @Override
    public void save(SettingsInfo settings) {
        lockingSupport.runInWriteLock(
                () -> super.save(settings), "save(%s[%s])".formatted(typeOf(settings), nameOf(settings)));
    }

    /**
     * {@inheritDoc}
     * <p>Runs within a cluster-wide write lock to ensure update safety for removing settings.
     */
    @Override
    public void remove(SettingsInfo settings) {
        lockingSupport.runInWriteLock(
                () -> super.remove(settings), "remove(%s[%s])".formatted(typeOf(settings), nameOf(settings)));
    }

    /**
     * {@inheritDoc}
     * <p>Runs within a cluster-wide write lock to ensure update safety for logging configuration.
     */
    @Override
    public void setLogging(LoggingInfo logging) {
        lockingSupport.runInWriteLock(() -> super.setLogging(logging), "setLogging(LoggingInfo)");
    }

    /**
     * {@inheritDoc}
     * <p>Runs within a cluster-wide write lock to ensure update safety for saving logging configuration.
     */
    @Override
    public void save(LoggingInfo logging) {
        lockingSupport.runInWriteLock(() -> super.save(logging), "save(LoggingInfo)");
    }

    /**
     * {@inheritDoc}
     * <p>Runs within a cluster-wide write lock to ensure update safety for adding services.
     */
    @Override
    public void add(ServiceInfo service) {
        lockingSupport.runInWriteLock(
                () -> super.add(service), "add(%s[%s])".formatted(typeOf(service), nameOf(service)));
    }

    /**
     * {@inheritDoc}
     * <p>Runs within a cluster-wide write lock to ensure update safety for removing services.
     */
    @Override
    public void remove(ServiceInfo service) {
        lockingSupport.runInWriteLock(
                () -> super.remove(service), "remove(%s[%s])".formatted(typeOf(service), nameOf(service)));
    }

    /**
     * {@inheritDoc}
     * <p>Runs within a cluster-wide write lock to ensure update safety for saving services.
     */
    @Override
    public void save(ServiceInfo service) {
        lockingSupport.runInWriteLock(
                () -> super.save(service), "save(%s[%s])".formatted(typeOf(service), nameOf(service)));
    }
}
