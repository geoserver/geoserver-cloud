/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.security;

import static org.springframework.util.StringUtils.hasText;

import org.geoserver.security.impl.GeoServerRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.List;

/**
 * {@link AuthenticationProvider} that allows to set an administrator account (username and
 * password) through {@link Environment} properties {@code ${geoserver.admin.username:admin}} and
 * {@code ${geoserver.admin.password:}}.
 *
 * <p>Useful for devOps to set the admin password through a Kubernetes secret, instead of having to
 * tweak the security configuration XML files with an init container or similar.
 *
 * <p>This authentication provider will be the first one tested for an HTTP Basic authorization,
 * only if a password is provided, and regardless of the authentication chain configured in
 * GeoServer.
 *
 * <p>If enabled (i.e. password provided), a failed attempt to log in will cancel the authentication
 * chain, and no other authentication providers will be tested. If the default {@literal admin}
 * username is used, it effectively overrides the admin password set in the xml configuration. If a
 * separate administrator username is given, the regular {@literal admin} user is still active, so
 * it's up to the devOps to handle the {@literal admin} user password as usual.
 *
 * @since 1.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EnvironmentAdminAuthenticationProvider implements AuthenticationProvider {

    @Value("${geoserver.admin.username:admin}")
    private String adminUserName;

    @Value("${geoserver.admin.password:}")
    private String adminPassword;

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    public Authentication authenticate(Authentication token) throws AuthenticationException {
        final String adminUserName = this.adminUserName;
        final String adminPassword = this.adminPassword;

        final boolean sameName = hasText(adminUserName) && adminUserName.equals(token.getName());
        final boolean checkPwd = sameName && hasText(adminPassword);
        UsernamePasswordAuthenticationToken authenticated = null;
        if (checkPwd) {
            final String pwd =
                    token.getCredentials() == null ? null : token.getCredentials().toString();
            if (adminPassword.equals(pwd)) {
                authenticated =
                        new UsernamePasswordAuthenticationToken(
                                adminUserName, null, List.of(GeoServerRole.ADMIN_ROLE));
                authenticated.setDetails(token.getDetails());
            } else {
                // this breaks the cycle through other providers, as opposed to
                // BadCredentialsException
                throw new InternalAuthenticationServiceException(
                        "Bad credentials for: " + token.getPrincipal());
            }
        }
        return authenticated;
    }
}
