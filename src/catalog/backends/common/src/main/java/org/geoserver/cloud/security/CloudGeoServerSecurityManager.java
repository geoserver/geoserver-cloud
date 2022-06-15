/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.security;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.event.security.SecurityConfigEvent;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

import java.io.IOException;

/**
 * Extends {@link GeoServerSecurityManager} to {@link #fireRemoteChangedEvent(String) notify} other
 * services of changes to the security configuration happened on the currently running service, and
 * to {@link #onRemoteSecurityConfigChangeEvent listen to} those events to {@link
 * GeoServerSecurityManager#reload() reload} the security config when other serice made a change.
 */
@Slf4j(topic = "org.geoserver.cloud.security")
public class CloudGeoServerSecurityManager extends GeoServerSecurityManager {

    private @Autowired ApplicationEventPublisher eventPublisher;

    private boolean reloading = false;
    private boolean changedDuringReload = false;

    public CloudGeoServerSecurityManager(GeoServerDataDirectory dataDir) throws Exception {
        super(dataDir);
    }

    public @Override void reload() {
        reloading = true;
        changedDuringReload = false;
        try {
            super.reload();
        } finally {
            reloading = false;
        }
        if (changedDuringReload) {
            fireRemoteChangedEvent("Changed during reload");
        }
    }

    /** Fires a {@link SecurityConfigEvent} for other services to react accordingly. */
    public void fireRemoteChangedEvent(@NonNull String reason) {
        if (reloading) {
            changedDuringReload = true;
        } else {
            log.debug("Publishing remote security event due to {}", reason);
            eventPublisher.publishEvent(event(reason));
        }
    }

    /**
     * Listens to {@link SecurityConfigEvent} sent by other services and {@link #reload() reloads}
     * the configuration
     */
    @EventListener(SecurityConfigEvent.class)
    public void onRemoteSecurityConfigChangeEvent(SecurityConfigEvent event) {
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
        synchronized (this) {
            super.reload();
            log.debug("Security configuration reloaded due to change event:", event);
        }
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigEvent} */
    public @Override void saveRoleService(SecurityRoleServiceConfig config)
            throws IOException, SecurityConfigException {
        super.saveRoleService(config);
        fireRemoteChangedEvent("SecurityRoleServiceConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigEvent} */
    public @Override void savePasswordPolicy(PasswordPolicyConfig config)
            throws IOException, SecurityConfigException {
        super.savePasswordPolicy(config);
        fireRemoteChangedEvent("PasswordPolicyConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigEvent} */
    public @Override void saveUserGroupService(SecurityUserGroupServiceConfig config)
            throws IOException, SecurityConfigException {
        super.saveUserGroupService(config);
        fireRemoteChangedEvent("SecurityUserGroupServiceConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigEvent} */
    public @Override void saveAuthenticationProvider(SecurityAuthProviderConfig config)
            throws IOException, SecurityConfigException {
        super.saveAuthenticationProvider(config);
        fireRemoteChangedEvent("SecurityAuthProviderConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigEvent} */
    public @Override void saveFilter(SecurityNamedServiceConfig config)
            throws IOException, SecurityConfigException {
        super.saveFilter(config);
        fireRemoteChangedEvent("SecurityNamedServiceConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigEvent} */
    public @Override void saveSecurityConfig(SecurityManagerConfig config) throws Exception {
        super.saveSecurityConfig(config);
        fireRemoteChangedEvent("SecurityManagerConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigEvent} */
    public @Override void saveMasterPasswordConfig(
            MasterPasswordConfig config,
            char[] currPasswd,
            char[] newPasswd,
            char[] newPasswdConfirm)
            throws Exception {
        super.saveMasterPasswordConfig(config, currPasswd, newPasswd, newPasswdConfirm);
        fireRemoteChangedEvent("MasterPasswordConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigEvent} */
    public @Override void saveMasterPasswordConfig(MasterPasswordConfig config) throws IOException {
        super.saveMasterPasswordConfig(config);
        fireRemoteChangedEvent("MasterPasswordConfig changed");
    }

    /** Override to {@link #fireChanged fire} a remote {@link SecurityConfigEvent} */
    public @Override void saveMasterPasswordProviderConfig(MasterPasswordProviderConfig config)
            throws IOException, SecurityConfigException {
        super.saveMasterPasswordProviderConfig(config);
        fireRemoteChangedEvent("MasterPasswordProviderConfig changed");
    }

    protected SecurityConfigEvent event(@NonNull String reason) {
        return SecurityConfigEvent.createLocal(reason);
    }
}
