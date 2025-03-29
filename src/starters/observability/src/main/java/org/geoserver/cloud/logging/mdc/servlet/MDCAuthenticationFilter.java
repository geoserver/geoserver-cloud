/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.logging.mdc.servlet;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.cloud.logging.mdc.config.AuthenticationMdcConfigProperties;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Appends the {@code enduser.id} and {@code enduser.role} MDC properties
 * depending on whether {@link AuthenticationMdcConfigProperties#isUser() user}
 * and {@link AuthenticationMdcConfigProperties#isRoles() roles} config
 * properties are enabled, respectively.
 *
 * <p>
 * Note the appended MDC properties follow the <a href=
 * "https://opentelemetry.io/docs/specs/semconv/general/attributes/#general-identity-attributes">OpenTelemetry
 * identity attributes</a> convention, so we can replace this component if OTel
 * would automatically add them to the logs.
 */
@RequiredArgsConstructor
public class MDCAuthenticationFilter implements Filter {

    private final @NonNull AuthenticationMdcConfigProperties config;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            addEnduserMdcProperties();
        } finally {
            chain.doFilter(request, response);
        }
    }

    void addEnduserMdcProperties() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated;
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            authenticated = false;
        } else {
            authenticated = auth.isAuthenticated();
        }
        MDC.put("enduser.authenticated", String.valueOf(authenticated));
        if (authenticated) {
            if (config.isId()) MDC.put("enduser.id", auth.getName());
            if (config.isRoles()) MDC.put("enduser.role", roles(auth));
        }
    }

    private String roles(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
    }
}
