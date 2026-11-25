/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.oidc;

import org.geoserver.spring.config.annotations.ComponentScanStrategy;
import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Transpiled Spring configuration for the GeoServer OIDC extension's core context (bean definitions from
 * {@code gs-sec-oidc-core}'s {@code applicationContext.xml}).
 *
 * <p>The upstream XML declares a {@code <context:component-scan>} for {@code org.geoserver.security.oauth2}. That
 * package contains no {@code @Component}-annotated classes; all beans are registered explicitly. The component-scan is
 * therefore ignored via {@link ComponentScanStrategy#IGNORE}.
 *
 * @since 3.0.3
 * @see OidcWebUIConfiguration
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(
        locations = "jar:gs-sec-oidc-core-.*!/applicationContext.xml",
        componentScanStrategy = ComponentScanStrategy.IGNORE,
        // Spring Security internal @Configuration classes. The XML registers them as <bean>s,
        // and Spring XML treats them as configuration sources: their own @Bean methods get processed.
        // Transpiled to plain @Bean methods they would return bare instances without their
        // nested @Bean methods. OidcSecurityInfrastructureImportSelector imports them instead.
        excludes = OidcCoreConfiguration.INTERNAL_SPRING_SECURITY_BEANS)
@Import({OidcCoreConfiguration_Generated.class, OidcSecurityInfrastructureImportSelector.class})
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class OidcCoreConfiguration {
    static final String INTERNAL_SPRING_SECURITY_BEANS =
            "oidcHttpSecurityConfiguration|oidcObjectPostProcessorConfiguration|oidcAuthenticationConfiguration|contentNegotiationStrategy";
}
