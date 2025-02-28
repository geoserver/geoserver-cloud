/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.security;

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
import org.geoserver.cloud.event.security.SecurityConfigChanged;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.password.MasterPasswordConfig;
import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AuthenticationProvider;

/**
 * Extends {@link GeoServerSecurityManager} to {@link #fireRemoteChangedEvent(String) notify} other
 * services of changes to the security configuration happened on the currently running service, and
 * to {@link #onRemoteSecurityConfigChangeEvent listen to} those events to {@link
 * GeoServerSecurityManager#reload() reload} the security config when other service made a change.
 */
@Slf4j(topic = "org.geoserver.cloud.security")
public class CloudGeoServerSecurityManager extends GeoServerSecurityManager {

    private final Consumer<SecurityConfigChanged> eventPublisher;
    private final Supplier<Long> updateSequenceIncrementor;

    private final AtomicBoolean reloading = new AtomicBoolean(false);
    private List<AuthenticationProvider> additionalAuthenticationProviders;
    private GeoServerConfigurationLock configLock;

    public CloudGeoServerSecurityManager(
            GeoServerConfigurationLock configLock,
            GeoServerDataDirectory dataDir,
            @NonNull Consumer<SecurityConfigChanged> eventPublisher,
            @NonNull Supplier<Long> updateSequenceIncrementor,
            @NonNull List<AuthenticationProvider> additionalAuthenticationProviders)
            throws Exception {
        super(dataDir);
        this.configLock = configLock;
        this.additionalAuthenticationProviders = additionalAuthenticationProviders;
        this.eventPublisher = eventPublisher;
        this.updateSequenceIncrementor = updateSequenceIncrementor;
    }

    @Override
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
            try {
                super.reload();
            } finally {
                configLock.unlock();
                log.debug("Released global lock after reloading the security configuration");
                reloading.set(false);
            }
        } else {
            log.warn("Config already being reloaded, ignoring");
        }
    }

    /**
     * Listens to {@link SecurityConfigChanged} sent by other services and {@link #reload() reloads}
     * the configuration
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

    /** Fires a {@link SecurityConfigChanged} for other services to react accordingly, unless it is {@link #reload() reloading} . */
    public void fireRemoteChangedEvent(@NonNull String reason) {
        if (reloading.get()) {
            log.info("{}: won't send security change event, config is reloading", reason);
        } else {
            log.debug("Publishing remote security event due to {}", reason);
            eventPublisher.accept(event(reason));
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
