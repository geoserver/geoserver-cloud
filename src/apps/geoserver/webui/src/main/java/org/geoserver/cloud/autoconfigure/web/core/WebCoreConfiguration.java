/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.core;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.GeoServerWicketServlet;
import org.geoserver.web.HeaderContribution;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the GeoServer Wicket-based Web UI.
 *
 * <p>Imports beans from upstream {@code gs-web-core} and {@code gs-web-rest}, excluding:
 *
 * <ul>
 *   <li>{@code logsPage} - excluded to avoid conflicts
 *   <li>{@code wicket} - replaced by {@link #geoServerWicketServletRegistration} to work around
 *       Spring Security 6 path matching issues
 * </ul>
 *
 * <h2>Why ServletRegistrationBean instead of ServletWrappingController</h2>
 *
 * <p>Upstream GeoServer uses a {@code ServletWrappingController} for the Wicket servlet, routed
 * through {@code webDispatcherMapping} (a {@code SimpleUrlHandlerMapping}). This approach fails in
 * Spring Boot 3 / Spring Security 6 due to a path matching strategy mismatch:
 *
 * <ul>
 *   <li>Upstream's {@code SimpleUrlHandlerMapping} uses {@code patternParser=null} for Spring 5.x
 *       backwards compatibility (AntPathMatcher)
 *   <li>Spring Security 6's {@code PathPatternRequestMatcher} (used in {@code
 *       GeoServerSecurityMetadataSource}) expects {@code PathPatternParser}
 *   <li>When Security tries to match paths like {@code /web/wicket/resource/...}, it incorrectly
 *       computes the contextPath, causing: {@code IllegalArgumentException: Invalid contextPath
 *       '/wicket': must match the start of requestPath}
 * </ul>
 *
 * <p>Using {@code ServletRegistrationBean} with {@code /*} mapping bypasses Spring MVC's handler
 * mapping entirely, letting the servlet container route requests directly to the Wicket servlet.
 * The Wicket servlet's internal {@code filterPath=/web} handles the actual path matching.
 *
 * @see GeoServerWicketServlet
 * @see org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
 */
@Configuration(proxyBeanMethods = false)
@ImportFilteredResource({
    "jar:gs-web-core-.*!/applicationContext.xml#name=" + WebCoreConfiguration.EXCLUDED_BEANS_PATTERN, //
    "jar:gs-web-rest-.*!/applicationContext.xml" //
})
public class WebCoreConfiguration {

    /** Pattern to exclude {@code logsPage} and {@code wicket} beans from upstream imports. */
    static final String EXCLUDED_BEANS_PATTERN = "^(?!logsPage|wicket).*$";

    @Bean
    GeoServerWicketServlet geoServerWicketServlet() {
        return new GeoServerWicketServlet();
    }

    /**
     * Registers the Wicket servlet directly with the container, bypassing Spring MVC handler
     * mappings.
     *
     * <p>Named "wicket" to match upstream's bean name referenced by {@code webDispatcherMapping},
     * though that mapping is effectively bypassed by this direct servlet registration.
     *
     * @param servlet the GeoServer Wicket servlet instance
     * @return servlet registration mapped to {@code /*}
     */
    @Bean(name = "wicket")
    ServletRegistrationBean<GeoServerWicketServlet> geoServerWicketServletRegistration(GeoServerWicketServlet servlet) {
        return new ServletRegistrationBean<>(servlet, "/*");
    }

    /** Contributes the GeoServer Cloud CSS theme to the Wicket UI. */
    @Bean
    HeaderContribution geoserverCloudCssTheme() {
        HeaderContribution contribution = new HeaderContribution();
        contribution.setScope(GeoServerBasePage.class);
        contribution.setCSSFilename("geoserver-cloud.css");
        return contribution;
    }
}
