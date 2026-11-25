/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.oidc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;

/**
 * Registers the {@code contentNegotiationStrategy} bean under the exact name expected by Spring Security's
 * {@code HttpSecurityConfiguration} autowiring. Pulled in by {@link OidcSecurityInfrastructureImportSelector}.
 *
 * <p>The bean is a {@link ContentNegotiationManager} on purpose. GeoServer's REST configuration asks the
 * {@link ContentNegotiationStrategy} beans for the requested media types before its own strategies, and skips the
 * managers. A plain header strategy would answer first, and the REST APIs would ignore path extensions such as
 * {@code .xml}.
 */
@Configuration(proxyBeanMethods = false)
class OidcContentNegotiationConfig {

    @Bean
    ContentNegotiationStrategy contentNegotiationStrategy() {
        return new ContentNegotiationManager(new HeaderContentNegotiationStrategy());
    }
}
