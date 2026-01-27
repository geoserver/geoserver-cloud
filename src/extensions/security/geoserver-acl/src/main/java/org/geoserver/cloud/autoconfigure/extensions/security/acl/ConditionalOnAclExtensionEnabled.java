/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.security.acl;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.geoserver.acl.plugin.config.spring.AclEnabledCondition;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot {@link AutoConfiguration @AutoConfiguration} conditional to enable/disable the plugin when:
 *
 * <ul>
 *   <li>A GeoServer instance is available in the application context</li>
 *   <li>The GeoServer ACL extension is enabled via configuration property {@code geoserver.extension.security.acl.enabled=true}</li>
 * </ul>
 *
 * <p>This can be used on any Spring components that should only be registered when the
 * GeoServer ACL extension is enabled.
 *
 * <p>For plain Spring (without spring boot auto configuration support),
 * {@link AclEnabledCondition @Conditional(AclEnabledCondition.class)} is to be used on plain
 * {@link Configuration @Configuration} classes
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnGeoServer
@ConditionalOnProperty(
        prefix = AclExtensionConfigurationProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = AclExtensionConfigurationProperties.DEFAULT)
@ConditionalOnProperty(prefix = "geoserver.acl", name = "enabled", havingValue = "true", matchIfMissing = true)
public @interface ConditionalOnAclExtensionEnabled {}
