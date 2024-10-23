/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.observability;

import java.util.Optional;
import org.geoserver.cloud.observability.logging.config.MDCConfigProperties;
import org.geoserver.cloud.observability.logging.servlet.HttpRequestMdcConfigProperties;
import org.geoserver.cloud.observability.logging.servlet.HttpRequestMdcFilter;
import org.geoserver.cloud.observability.logging.servlet.MDCAuthenticationFilter;
import org.geoserver.cloud.observability.logging.servlet.MDCCleaningFilter;
import org.geoserver.cloud.observability.logging.servlet.SpringEnvironmentMdcConfigProperties;
import org.geoserver.cloud.observability.logging.servlet.SpringEnvironmentMdcFilter;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;

/**
 * {@link AutoConfiguration @AutoConfiguration} to enable logging MDC (Mapped Diagnostic Context)
 * contributions during the request life cycle
 *
 * @see GeoServerDispatcherMDCConfiguration
 */
@AutoConfiguration
@EnableConfigurationProperties({
    MDCConfigProperties.class,
    HttpRequestMdcConfigProperties.class,
    SpringEnvironmentMdcConfigProperties.class
})
@Import(GeoServerDispatcherMDCConfiguration.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
public class LoggingMDCAutoConfiguration {

    /**
     * @return servlet filter to {@link MDC#clear() clear} the MDC after the servlet request is
     *     executed
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    HttpRequestMdcFilter httpMdcFilter(HttpRequestMdcConfigProperties config) {
        return new HttpRequestMdcFilter(config);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    MDCCleaningFilter mdcCleaningServletFilter() {
        return new MDCCleaningFilter();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SpringEnvironmentMdcFilter springEnvironmentMdcFilter(
            Environment env, SpringEnvironmentMdcConfigProperties config, Optional<BuildProperties> buildProperties) {
        return new SpringEnvironmentMdcFilter(env, buildProperties, config);
    }

    /**
     * A servlet registration for {@link MDCAuthenticationFilter}, with {@link
     * FilterRegistrationBean#setMatchAfter setMatchAfter(true)} to ensure it runs after {@link
     * GeoServerSecurityFilterChainProxy} and hence the {@link SecurityContext} already has the
     * {@link Authentication} object.
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.security.core.Authentication")
    FilterRegistrationBean<MDCAuthenticationFilter> mdcAuthenticationPropertiesServletFilter(
            MDCConfigProperties config) {
        FilterRegistrationBean<MDCAuthenticationFilter> registration = new FilterRegistrationBean<>();

        var filter = new MDCAuthenticationFilter(config);
        registration.setMatchAfter(true);

        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);
        registration.setFilter(filter);
        return registration;
    }
}
