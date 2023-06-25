/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.security;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.security.filter.GeoServerLogoutFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

@Configuration(proxyBeanMethods = true)
@ImportResource( //
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = { //
            "jar:gs-web-sec-core-.*!/applicationContext.xml", //
            "jar:gs-web-sec-jdbc-.*!/applicationContext.xml", //
            "jar:gs-web-sec-ldap-.*!/applicationContext.xml" //
        } //
        )
public class SecurityConfiguration {

    @Bean
    @ConditionalOnProperty("geoserver.web-ui.security.logout-url")
    FilterRegistrationBean<ConfigurableLogoutUrlFilter> configurableLogoutUrlFilterRegistration(
            @Value("${geoserver.web-ui.security.logout-url}") String logoutUrl) {

        ConfigurableLogoutUrlFilter filter = new ConfigurableLogoutUrlFilter(logoutUrl);

        FilterRegistrationBean<ConfigurableLogoutUrlFilter> registration =
                new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.setOrder(1);
        return registration;
    }

    @RequiredArgsConstructor
    private static class ConfigurableLogoutUrlFilter implements Filter {

        private final @NonNull String logoutUrl;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            request.setAttribute(GeoServerLogoutFilter.LOGOUT_REDIRECT_ATTR, logoutUrl);

            chain.doFilter(request, response);
        }
    }
}
