/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.security;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Extends {@link GeoServerSecurityManager} to {@link #fireRemoteChangedEvent(String) notify} other
 * services of changes to the security configuration happened on the currently running service, and
 * to {@link #onRemoteSecurityConfigChangeEvent listen to} those events to {@link
 * GeoServerSecurityManager#reload() reload} the security config when other serice made a change.
 */
@Slf4j(topic = "org.geoserver.cloud.security")
public class CloudGeoServerSecurityManager extends GeoServerSecurityManager {

    private final Consumer<SecurityConfigChanged> eventPublisher;
    private final Supplier<Long> updateSequenceIncrementor;

    private final AtomicBoolean reloading = new AtomicBoolean(false);
    private boolean changedDuringReload = false;

    public CloudGeoServerSecurityManager(
            GeoServerDataDirectory dataDir,
            @NonNull Consumer<SecurityConfigChanged> eventPublisher,
            @NonNull Supplier<Long> updateSequenceIncrementor)
            throws Exception {
        super(dataDir);
        this.eventPublisher = eventPublisher;
        this.updateSequenceIncrementor = updateSequenceIncrementor;
    }

    public @Override void reload() {
        if (reloading.compareAndSet(false, true)) {
            changedDuringReload = false;
            try {
                super.reload();
            } finally {
                reloading.set(false);
            }
            if (changedDuringReload) {
                fireRemoteChangedEvent("Changed during reload");
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
            log.info(
                    "Ignoring security config change event, security subsystem not yet initialized: {}",
                    event);
            return;
        }
        log.info("Reloading security configuration due to change event: {}", event);
        reload();
        log.debug("Security configuration reloaded due to change event:", event);
    }

    /** Fires a {@link SecurityConfigChanged} for other services to react accordingly. */
    public void fireRemoteChangedEvent(@NonNull String reason) {
        if (reloading.get()) {
            changedDuringReload = true;
        } else {
            log.debug("Publishing remote security event due to {}", reason);
            eventPublisher.accept(event(reason));
        }
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigChanged} */
    public @Override void saveRoleService(SecurityRoleServiceConfig config)
            throws IOException, SecurityConfigException {
        super.saveRoleService(config);
        fireRemoteChangedEvent("SecurityRoleServiceConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigChanged} */
    public @Override void savePasswordPolicy(PasswordPolicyConfig config)
            throws IOException, SecurityConfigException {
        super.savePasswordPolicy(config);
        fireRemoteChangedEvent("PasswordPolicyConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigChanged} */
    public @Override void saveUserGroupService(SecurityUserGroupServiceConfig config)
            throws IOException, SecurityConfigException {
        super.saveUserGroupService(config);
        fireRemoteChangedEvent("SecurityUserGroupServiceConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigChanged} */
    public @Override void saveAuthenticationProvider(SecurityAuthProviderConfig config)
            throws IOException, SecurityConfigException {
        super.saveAuthenticationProvider(config);
        fireRemoteChangedEvent("SecurityAuthProviderConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigChanged} */
    public @Override void saveFilter(SecurityNamedServiceConfig config)
            throws IOException, SecurityConfigException {
        super.saveFilter(config);
        fireRemoteChangedEvent("SecurityNamedServiceConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigChanged} */
    public @Override void saveSecurityConfig(SecurityManagerConfig config) throws Exception {
        super.saveSecurityConfig(config);
        fireRemoteChangedEvent("SecurityManagerConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigChanged} */
    public @Override void saveMasterPasswordConfig(
            MasterPasswordConfig config,
            char[] currPasswd,
            char[] newPasswd,
            char[] newPasswdConfirm)
            throws Exception {
        super.saveMasterPasswordConfig(config, currPasswd, newPasswd, newPasswdConfirm);
        fireRemoteChangedEvent("MasterPasswordConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigChanged} */
    public @Override void saveMasterPasswordConfig(MasterPasswordConfig config) throws IOException {
        super.saveMasterPasswordConfig(config);
        fireRemoteChangedEvent("MasterPasswordConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigChanged} */
    public @Override void saveMasterPasswordProviderConfig(MasterPasswordProviderConfig config)
            throws IOException, SecurityConfigException {
        super.saveMasterPasswordProviderConfig(config);
        fireRemoteChangedEvent("MasterPasswordProviderConfig changed");
    }

    protected SecurityConfigChanged event(@NonNull String reason) {
        long sequence = updateSequenceIncrementor.get();
        return SecurityConfigChanged.createLocal(sequence, reason);
    }
}
