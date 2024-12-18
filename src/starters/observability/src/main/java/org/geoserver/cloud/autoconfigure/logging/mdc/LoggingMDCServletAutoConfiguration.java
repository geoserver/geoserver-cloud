/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.logging.mdc;

import java.util.Optional;
import org.geoserver.cloud.logging.mdc.config.MDCConfigProperties;
import org.geoserver.cloud.logging.mdc.servlet.HttpRequestMdcFilter;
import org.geoserver.cloud.logging.mdc.servlet.MDCAuthenticationFilter;
import org.geoserver.cloud.logging.mdc.servlet.MDCCleaningFilter;
import org.geoserver.cloud.logging.mdc.servlet.SpringEnvironmentMdcFilter;
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
@EnableConfigurationProperties({MDCConfigProperties.class})
@Import(GeoServerDispatcherMDCConfiguration.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
public class LoggingMDCServletAutoConfiguration {

    @Bean
    MDCCleaningFilter mdcCleaningServletFilter() {
        return new MDCCleaningFilter();
    }

    /**
     * @return servlet filter to {@link MDC#clear() clear} the MDC after the servlet request is
     *     executed
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 2)
    HttpRequestMdcFilter httpMdcFilter(MDCConfigProperties config) {
        return new HttpRequestMdcFilter(config.getHttp());
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 2)
    SpringEnvironmentMdcFilter springEnvironmentMdcFilter(
            Environment env, MDCConfigProperties config, Optional<BuildProperties> buildProperties) {
        return new SpringEnvironmentMdcFilter(env, buildProperties, config.getApplication());
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

        var filter = new MDCAuthenticationFilter(config.getUser());
        registration.setMatchAfter(true);

        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);
        registration.setFilter(filter);
        return registration;
    }
}
