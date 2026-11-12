/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.oidc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers a placeholder bean named {@code oidcHttpSecurityConfiguration} so that the transpiled
 * {@code oauth2LoginAuthenticationProvider}'s {@code @DependsOn("oidcHttpSecurityConfiguration")} resolves.
 *
 * <p>In the upstream XML, {@code oidcHttpSecurityConfiguration} names a {@code HttpSecurityConfiguration} bean; in this
 * Cloud port that class is imported as a configuration source via {@link OidcSecurityInfrastructureImportSelector}, so
 * its bean is registered under the fully qualified name Spring Security expects
 * ({@code org.springframework.security.config.annotation.web.configuration.HttpSecurityConfiguration}). The upstream
 * XML name is preserved only to satisfy the {@code @DependsOn} ordering hint; it carries no runtime behavior of its
 * own.
 */
@Configuration(proxyBeanMethods = false)
class OidcDependsOnAliasConfig {

    @Bean(name = "oidcHttpSecurityConfiguration")
    Object oidcHttpSecurityConfigurationAlias() {
        return new Object();
    }
}
