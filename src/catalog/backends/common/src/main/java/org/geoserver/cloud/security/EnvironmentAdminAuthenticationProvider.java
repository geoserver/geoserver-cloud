/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.security;

import static org.springframework.util.StringUtils.hasText;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;

import java.util.List;

import javax.annotation.PostConstruct;

/**
 * {@link AuthenticationProvider} that allows to set an administrator account (username and
 * password) through {@link Environment} properties {@code ${geoserver.admin.username:admin}} and
 * {@code ${geoserver.admin.password:}}.
 *
 * <p>Useful for devOps to set the admin password through a Kubernetes secret, instead of having to
 * tweak the security configuration XML files with an init container or similar.
 *
 * <p>This authentication provider will be the first one tested for an HTTP Basic authorization,
 * only if both the username and password are provided, and regardless of the authentication chain
 * configured in GeoServer.
 *
 * <p>If enabled (i.e. both username and password provided), a failed attempt to log in will cancel
 * the authentication chain, and no other authentication providers will be tested.
 *
 * <p>If the default {@literal admin} username is used, it effectively overrides the admin password
 * set in the xml configuration. If a separate administrator username is given, the regular
 * {@literal admin} user is disabled.
 *
 * @since 1.0
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EnvironmentAdminAuthenticationProvider implements AuthenticationProvider {

    @Value("${geoserver.admin.username:}")
    private String adminUserName;

    @Value("${geoserver.admin.password:}")
    private String adminPassword;

    private boolean enabled;

    @PostConstruct
    void validateConfig() {
        final boolean userSet = StringUtils.hasText(adminUserName);
        final boolean passwordSet = StringUtils.hasText(adminPassword);
        if (userSet && !passwordSet) {
            String msg =
                    String.format(
                            """
                    Found overriding admin username config property geoserver.admin.username=%s, \
                    but password not provided through config property geoserver.admin.password
                    """,
                            adminUserName);
            throw new BeanInstantiationException(getClass(), msg);
        }
        if (passwordSet && !userSet) {
            String msg =
                    String.format(
                            """
                    Found overriding admin password config property geoserver.admin.password, \
                    but admin username not provided through config property geoserver.admin.username
                    """,
                            adminUserName);
            throw new BeanInstantiationException(getClass(), msg);
        }
        enabled = userSet && passwordSet;
        if (enabled) {
            log.info(
                    "The default admin username and password are overridden by the externalized geoserver.admin.username and geoserver.admin.password config properties.");
        }
    }

    public static List<GrantedAuthority> adminRoles() {
        return List.of(
                new GeoServerRole("ADMIN"),
                GeoServerRole.ADMIN_ROLE,
                GeoServerRole.AUTHENTICATED_ROLE);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /**
     * {@inheritDoc}
     *
     * @return a fully authenticated {@link UsernamePasswordAuthenticationToken} if {@code token} is
     *     a {@code UsernamePasswordAuthenticationToken}, and both the username and password match
     *     the ones provided by the configuration properties {@literal geoserver.admin.username} and
     *     {@literal geoserver.admin.password}
     * @throws InternalAuthenticationServiceException to break the authentication chain if the
     *     credentials don't match, or the
     */
    @Override
    public Authentication authenticate(Authentication token) throws AuthenticationException {
        if (!enabled) {
            // proceed with the authentication chain
            return null;
        }
        final String adminUserName = this.adminUserName;
        final String adminPassword = this.adminPassword;

        final String name = token.getName();
        if (GeoServerUser.ADMIN_USERNAME.equals(name) && !adminUserName.equals(name)) {
            throw new InternalAuthenticationServiceException("Default admin user is disabled");
        }

        final boolean sameName = hasText(adminUserName) && adminUserName.equals(name);
        if (!sameName) {
            // not the configured admin username, proceed with the authentication chain
            return null;
        }
        // enabled and requesting authentication against the configured admin user name, perform the
        // auth checks

        final String pwd =
                token.getCredentials() == null ? null : token.getCredentials().toString();
        if (adminPassword.equals(pwd)) {
            List<GrantedAuthority> adminRoles = adminRoles();
            UsernamePasswordAuthenticationToken authenticated =
                    UsernamePasswordAuthenticationToken.authenticated(
                            adminUserName, null, adminRoles);
            authenticated.setDetails(token.getDetails());
            return authenticated;
        }
        // this breaks the cycle through other providers, as opposed to
        // BadCredentialsException
        throw new InternalAuthenticationServiceException(
                "Bad credentials for: " + token.getPrincipal());
    }
}
