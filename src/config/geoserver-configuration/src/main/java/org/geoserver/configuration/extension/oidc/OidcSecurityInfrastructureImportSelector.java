/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.oidc;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * {@link ImportSelector} that registers the Spring Security internal {@code @Configuration} classes required by
 * {@link org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationFilterBuilder} to obtain a fresh
 * prototype-scoped {@code HttpSecurity} instance.
 *
 * <p>{@code HttpSecurityConfiguration} and {@code ObjectPostProcessorConfiguration} are package-private in Spring
 * Security, so they cannot be referenced by class literal in {@code @Import(...)} from another package. Returning their
 * fully qualified names from an {@link ImportSelector} lets Spring load them by reflection while still processing their
 * {@code @Bean} methods as configuration sources.
 *
 * <p>{@link org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration} is
 * public, but it depends on an {@code ObjectPostProcessor<Object>} produced by
 * {@code ObjectPostProcessorConfiguration}, so it is registered here alongside the others for locality.
 *
 * <p>A synthetic bean named {@code oidcHttpSecurityConfiguration} is also registered so that the transpiled
 * {@code oauth2LoginAuthenticationProvider} bean's {@code @DependsOn("oidcHttpSecurityConfiguration")} resolves (the
 * name comes from the upstream XML and is purely an ordering hint).
 *
 * <p>A public {@code contentNegotiationStrategy} bean is registered from {@link OidcContentNegotiationConfig}; the
 * {@code oidcHttpSecurityConfiguration} alias bean (for the {@code @DependsOn} ordering hint) is registered from
 * {@link OidcDependsOnAliasConfig}. Both are included in the selector's FQN list below.
 *
 * @since 3.0.3
 */
class OidcSecurityInfrastructureImportSelector implements ImportSelector {

    private static final String[] SPRING_SECURITY_INTERNAL_CONFIGS = {
        "org.springframework.security.config.annotation.configuration.ObjectPostProcessorConfiguration",
        "org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration",
        "org.springframework.security.config.annotation.web.configuration.HttpSecurityConfiguration",
        "org.geoserver.configuration.extension.oidc.OidcContentNegotiationConfig",
        "org.geoserver.configuration.extension.oidc.OidcDependsOnAliasConfig"
    };

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return SPRING_SECURITY_INTERNAL_CONFIGS;
    }
}
