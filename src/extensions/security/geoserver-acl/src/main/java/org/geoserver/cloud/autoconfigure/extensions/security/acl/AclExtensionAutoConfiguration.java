/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.acl;

import jakarta.annotation.PostConstruct;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.acl.authorization.AuthorizationService;
import org.geoserver.acl.autoconfigure.messaging.bus.AclSpringCloudBusAutoConfiguration;
import org.geoserver.acl.config.webapi.client.ApiClientProperties;
import org.geoserver.acl.plugin.accessmanager.AclResourceAccessManager;
import org.geoserver.acl.plugin.config.accessmanager.AclWebApiAccessManagerConfiguration;
import org.geoserver.acl.plugin.config.cache.CachingAuthorizationServicePluginConfiguration;
import org.geoserver.acl.plugin.config.webui.AclWebUIConfiguration;
import org.geoserver.acl.plugin.config.wps.AclWpsIntegrationConfiguration;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWPS;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Placeholder auto-configuration for the GeoServer ACL extension.
 *
 * <p>
 * <strong>Both</strong> {@code geoserver.extension.security.acl.enabled} and
 * {@code geoserver.acl.client.enabled} configuration properties must be
 * {@code true}.
 * <p>
 * The former enables the extension in GeoServer Cloud, when disabled no attempt
 * to create an {@link AclResourceAccessManager} is performed.
 * <p>
 * {@code geoserver.acl.client.enabled=true} sets up an
 * {@link AclResourceAccessManager} backed by the GeoServer ACL REST API client.
 * In the future there might be other composites that create the resource access
 * manager with ACL different application/domain port implementations (e.g. JPA,
 * GRPC, etc.).
 * <p>
 * Only when both are enabled, the {@link ApiClientProperties ACL client
 * configuration properties} are taken into account:
 *
 * <pre>{@code
 * geoserver:
 *   extension.security.acl.enabled: true
 *   acl:
 *     client:
 *       enabled: true
 *       basePath: http://acl:8080/api
 *       username: geoserver
 *       password: pwd
 *       caching: true
 *       startupCheck: true
 *       initTimeout: 5
 * }</pre>
 *
 * @since 2.27.0.0
 * @see AclWebApiAccessManagerAutoConfiguration
 * @see AclCachingAutoConfiguration
 * @see AclWebUIAutoConfiguration
 * @see AclWpsAutoConfiguration
 */
@AutoConfiguration
@ConditionalOnAclExtensionEnabled
@Import({
    AclExtensionAutoConfiguration.AclResourceAccessManagerCheck.class,
    AclExtensionAutoConfiguration.AclWebApiAccessManagerAutoConfiguration.class,
    AclExtensionAutoConfiguration.AclWebUIAutoConfiguration.class,
    AclExtensionAutoConfiguration.AclCachingAutoConfiguration.class,
    AclExtensionAutoConfiguration.AclWpsAutoConfiguration.class,
    AclSpringCloudBusAutoConfiguration.class
})
@EnableConfigurationProperties(AclExtensionConfigurationProperties.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.security.acl")
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class AclExtensionAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    static class AclResourceAccessManagerCheck {

        AclResourceAccessManagerCheck(Optional<AclResourceAccessManager> accessManager) {
            if (accessManager.isPresent()) {
                log.info("GeoServer ACL extension installed");
            } else {
                throw new IllegalStateException("No AclResourceAccessManager configured");
            }
        }
    }
    /**
     * Imports {@link AclWebApiAccessManagerConfiguration} for an
     * {@link AclResourceAccessManager} backed by the GeoServer ACL REST API client
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "geoserver.acl.client.enabled", havingValue = "true", matchIfMissing = true)
    @Import(AclWebApiAccessManagerConfiguration.class)
    public static class AclWebApiAccessManagerAutoConfiguration {}

    /**
     * Imports {@link CachingAuthorizationServicePluginConfiguration} for a Spring Cache caching decorator on {@link AuthorizationService}
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "geoserver.acl.client.caching", havingValue = "true", matchIfMissing = true)
    @ConditionalOnBean(org.springframework.cache.CacheManager.class)
    @EnableCaching
    @Import(CachingAuthorizationServicePluginConfiguration.class)
    static class AclCachingAutoConfiguration {
        @PostConstruct
        void log() {
            log.info("geoserver.acl.client.caching: true");
        }
    }

    /**
     * Imports {@link AclWebUIConfiguration} when the {@link ConditionalOnGeoServerWebUI WEB-UI} is running
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnGeoServerWebUI
    @ConditionalOnProperty(name = "geoserver.web-ui.acl.enabled", havingValue = "true", matchIfMissing = false)
    @Import(AclWebUIConfiguration.class)
    static class AclWebUIAutoConfiguration {
        @PostConstruct
        void log() {
            log.info("geoserver.web-ui.acl.enabled: true");
        }
    }

    /**
     * Imports {@link AclWpsIntegrationConfiguration} when the {@link ConditionalOnGeoServerWPS WPS} is running
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnGeoServerWPS
    @Import(AclWpsIntegrationConfiguration.class)
    static class AclWpsAutoConfiguration {}
}
