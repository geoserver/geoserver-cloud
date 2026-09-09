/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.cloud.event.security.RolesChanged;
import org.geoserver.cloud.event.security.SecurityConfigChanged;
import org.geoserver.cloud.event.security.SecurityManagerReloaded;
import org.geoserver.cloud.event.security.UsersAndGroupsChanged;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.password.MasterPasswordConfig;
import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AuthenticationProvider;

/**
 * Extends {@link GeoServerSecurityManager} to {@link #fireRemoteChangedEvent(String) notify} other services of changes
 * to the security configuration happened on the currently running service, and to
 * {@link #onRemoteSecurityConfigChangeEvent listen to} those events to {@link GeoServerSecurityManager#reload() reload}
 * the security config when other service made a change.
 *
 * <p>Changes to the contents of user/group and role services (users, groups, roles and their associations) are notified
 * separately, through {@link UsersAndGroupsChanged} and {@link RolesChanged}: the services returned by
 * {@link #loadUserGroupService(String)} and {@link #loadRoleService(String)} create stores that fire those events once
 * saved, and {@link #onRemoteUsersAndGroupsChanged} and {@link #onRemoteRolesChanged} reload just the named service,
 * instead of the whole security configuration, when another service instance saved such a store.
 */
@Slf4j(topic = "org.geoserver.cloud.security")
public class CloudGeoServerSecurityManager extends GeoServerSecurityManager {

    private final Consumer<GeoServerEvent> eventPublisher;
    private final Supplier<Long> updateSequenceIncrementor;

    // Store applicationContext from setApplicationContext method
    private ApplicationContext applicationContext;

    private final AtomicBoolean reloading = new AtomicBoolean(false);
    private List<AuthenticationProvider> additionalAuthenticationProviders;
    private GeoServerConfigurationLock configLock;

    public CloudGeoServerSecurityManager(
            GeoServerConfigurationLock configLock,
            GeoServerDataDirectory dataDir,
            @NonNull Consumer<GeoServerEvent> eventPublisher,
            @NonNull Supplier<Long> updateSequenceIncrementor,
            @NonNull List<AuthenticationProvider> additionalAuthenticationProviders)
            throws Exception {
        super(dataDir);
        this.configLock = configLock;
        this.additionalAuthenticationProviders = additionalAuthenticationProviders;
        this.eventPublisher = eventPublisher;
        this.updateSequenceIncrementor = updateSequenceIncrementor;
    }

    /**
     * Override to capture applicationContext for our use. GeoServerSecurityManager implements ApplicationContextAware
     * but doesn't provide access to the applicationContext it stores.
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        super.setApplicationContext(applicationContext);
        this.applicationContext = applicationContext;
    }

    @Override
    @SuppressWarnings("java:S5803")
    public void setProviders(List<AuthenticationProvider> providers) throws Exception {
        providers = new ArrayList<>(providers);
        providers.addAll(0, additionalAuthenticationProviders);
        super.setProviders(providers);
    }

    @Override
    public void reload() {
        if (reloading.compareAndSet(false, true)) {
            // gotta grab a global lock, super.reload() tends to change things
            log.debug("Obtaining global lock to reload the security configuration");
            configLock.lock(LockType.WRITE);
            log.debug("Obtained global lock to reload the security configuration");

            SecurityManagerReloaded event = null;
            try {
                super.reload();

                // Create event but don't publish yet - wait until after lock release
                if (applicationContext != null) {
                    long sequence = updateSequenceIncrementor.get();
                    event = new SecurityManagerReloaded(this, sequence);
                }
            } finally {
                configLock.unlock();
                log.debug("Released global lock after reloading the security configuration");
                reloading.set(false);
                // Now publish the event after the lock has been released
                if (event != null && applicationContext != null) {
                    log.debug("Publishing SecurityManagerReloaded event");
                    applicationContext.publishEvent(event);
                }
            }
        } else {
            log.warn("Config already being reloaded, ignoring");
        }
    }

    /**
     * Listens to {@link SecurityConfigChanged} sent by other services and {@link #reload() reloads} the configuration
     */
    @EventListener(SecurityConfigChanged.class)
    public void onRemoteSecurityConfigChangeEvent(SecurityConfigChanged event) {
        if (event.isLocal()) {
            return;
        }
        if (!isInitialized()) {
            log.info("Ignoring security config change event, security subsystem not yet initialized: {}", event);
            return;
        }
        log.info("Reloading security configuration due to change event: {}", event);
        reload();
        log.debug("Security configuration reloaded due to change event:", event);
    }

    /** Listens to {@link UsersAndGroupsChanged} sent by other services and reloads the named user group service */
    @EventListener(UsersAndGroupsChanged.class)
    public void onRemoteUsersAndGroupsChanged(UsersAndGroupsChanged event) {
        if (shallHandle(event)) {
            reloadUserGroupService(event.getServiceName());
        }
    }

    /** Listens to {@link RolesChanged} sent by other services and reloads the named role service */
    @EventListener(RolesChanged.class)
    public void onRemoteRolesChanged(RolesChanged event) {
        if (shallHandle(event)) {
            reloadRoleService(event.getServiceName());
        }
    }

    private boolean shallHandle(GeoServerEvent event) {
        if (event.isLocal()) {
            return false;
        }
        if (!isInitialized()) {
            log.info("Ignoring event, security subsystem not yet initialized: {}", event);
            return false;
        }
        return true;
    }

    private void reloadUserGroupService(String serviceName) {
        try {
            GeoServerUserGroupService service = loadUserGroupService(serviceName);
            if (service == null) {
                log.info("Ignoring users and groups change, no user group service named {}", serviceName);
            } else {
                log.info("Reloading users and groups of service {}", serviceName);
                service.load();
            }
        } catch (IOException e) {
            log.error("Error reloading users and groups of service {}", serviceName, e);
        }
    }

    private void reloadRoleService(String serviceName) {
        try {
            GeoServerRoleService service = loadRoleService(serviceName);
            if (service == null) {
                log.info("Ignoring roles change, no role service named {}", serviceName);
            } else {
                log.info("Reloading roles of service {}", serviceName);
                service.load();
            }
        } catch (IOException e) {
            log.error("Error reloading roles of service {}", serviceName, e);
        }
    }

    /**
     * Override to make the stores created by the returned service {@link #fireUsersAndGroupsChanged fire} a remote
     * {@link UsersAndGroupsChanged} once saved
     */
    @Override
    public GeoServerUserGroupService loadUserGroupService(String name) throws IOException {
        GeoServerUserGroupService service = super.loadUserGroupService(name);
        if (service == null) {
            return null;
        }
        return new ChangeNotifyingUserGroupService(service, this::fireUsersAndGroupsChanged);
    }

    /**
     * Override to make the stores created by the returned service {@link #fireRolesChanged fire} a remote
     * {@link RolesChanged} once saved
     */
    @Override
    public GeoServerRoleService loadRoleService(String name) throws IOException {
        GeoServerRoleService service = super.loadRoleService(name);
        if (service == null) {
            return null;
        }
        return new ChangeNotifyingRoleService(service, this::fireRolesChanged);
    }

    /**
     * Fires a {@link SecurityConfigChanged} for other services to react accordingly, unless it is {@link #reload()
     * reloading} .
     */
    public void fireRemoteChangedEvent(@NonNull String reason) {
        publishUnlessReloading(reason, () -> event(reason));
    }

    /**
     * Fires a {@link UsersAndGroupsChanged} for the named user group service, unless it is {@link #reload() reloading}
     */
    void fireUsersAndGroupsChanged(@NonNull String serviceName) {
        String reason = "users and groups of service %s changed".formatted(serviceName);
        publishUnlessReloading(reason, () -> UsersAndGroupsChanged.createLocal(serviceName));
    }

    /** Fires a {@link RolesChanged} for the named role service, unless it is {@link #reload() reloading} */
    void fireRolesChanged(@NonNull String serviceName) {
        String reason = "roles of service %s changed".formatted(serviceName);
        publishUnlessReloading(reason, () -> RolesChanged.createLocal(serviceName));
    }

    private void publishUnlessReloading(String reason, Supplier<GeoServerEvent> event) {
        if (reloading.get()) {
            log.info("{}: won't send security change event, config is reloading", reason);
        } else {
            log.debug("Publishing remote security event due to {}", reason);
            eventPublisher.accept(event.get());
        }
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigChanged} */
    @Override
    public void saveRoleService(SecurityRoleServiceConfig config) throws IOException, SecurityConfigException {
        super.saveRoleService(config);
        fireRemoteChangedEvent("SecurityRoleServiceConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigChanged} */
    @Override
    public void savePasswordPolicy(PasswordPolicyConfig config) throws IOException, SecurityConfigException {
        super.savePasswordPolicy(config);
        fireRemoteChangedEvent("PasswordPolicyConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigChanged} */
    @Override
    public void saveUserGroupService(SecurityUserGroupServiceConfig config)
            throws IOException, SecurityConfigException {
        super.saveUserGroupService(config);
        fireRemoteChangedEvent("SecurityUserGroupServiceConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigChanged} */
    @Override
    public void saveAuthenticationProvider(SecurityAuthProviderConfig config)
            throws IOException, SecurityConfigException {
        super.saveAuthenticationProvider(config);
        fireRemoteChangedEvent("SecurityAuthProviderConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigChanged} */
    @Override
    public void saveFilter(SecurityNamedServiceConfig config) throws IOException, SecurityConfigException {
        super.saveFilter(config);
        fireRemoteChangedEvent("SecurityNamedServiceConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigChanged} */
    @Override
    public synchronized void saveSecurityConfig(SecurityManagerConfig config) throws Exception {
        super.saveSecurityConfig(config);
        fireRemoteChangedEvent("SecurityManagerConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigChanged} */
    @Override
    public synchronized void saveMasterPasswordConfig(
            MasterPasswordConfig config, char[] currPasswd, char[] newPasswd, char[] newPasswdConfirm)
            throws Exception {
        super.saveMasterPasswordConfig(config, currPasswd, newPasswd, newPasswdConfirm);
        fireRemoteChangedEvent("MasterPasswordConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigChanged} */
    @Override
    public void saveMasterPasswordConfig(MasterPasswordConfig config) throws IOException {
        super.saveMasterPasswordConfig(config);
        fireRemoteChangedEvent("MasterPasswordConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigChanged} */
    @Override
    public void saveMasterPasswordProviderConfig(MasterPasswordProviderConfig config)
            throws IOException, SecurityConfigException {
        super.saveMasterPasswordProviderConfig(config);
        fireRemoteChangedEvent("MasterPasswordProviderConfig changed");
    }

    protected SecurityConfigChanged event(@NonNull String reason) {
        long sequence = updateSequenceIncrementor.get();
        return SecurityConfigChanged.createLocal(sequence, reason);
    }
}
