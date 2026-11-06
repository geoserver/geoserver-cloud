/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.gatewaybearer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

/** Validates OIDC Bearer JWTs once at the gateway and provides the authenticated identity to SharedAuth. */
@AutoConfiguration
@ConditionalOnGatewayBearerAuth
@EnableConfigurationProperties(GatewayBearerAuthConfigProperties.class)
public class GatewayBearerAuthAutoConfiguration {

    @Bean
    @Order(1)
    SecurityFilterChain bearerTokenSecurityFilterChain(
            HttpSecurity http, JwtAuthenticationConverter bearerJwtAuthenticationConverter) throws Exception {
        http.securityMatcher(bearerTokenRequestMatcher())
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer ->
                        resourceServer.jwt(jwt -> jwt.jwtAuthenticationConverter(bearerJwtAuthenticationConverter)));
        return http.build();
    }

    @Bean
    RequestMatcher bearerTokenRequestMatcher() {
        return request -> {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            return authorization != null && authorization.regionMatches(true, 0, "Bearer", 0, 6);
        };
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder bearerJwtDecoder(GatewayBearerAuthConfigProperties properties) {
        Assert.hasText(properties.getIssuerUri(), GatewayBearerAuthConfigProperties.PREFIX + ".issuer-uri is required");
        Assert.hasText(properties.getAudience(), GatewayBearerAuthConfigProperties.PREFIX + ".audience is required");
        Assert.hasText(
                properties.getUsernameClaim(),
                GatewayBearerAuthConfigProperties.PREFIX + ".username-claim is required");
        Assert.hasText(
                properties.getRolesClaim(), GatewayBearerAuthConfigProperties.PREFIX + ".roles-claim is required");
        Assert.notEmpty(
                properties.getRoleMappings(),
                GatewayBearerAuthConfigProperties.PREFIX + ".role-mappings must not be empty");
        Assert.hasText(
                properties.getRolePrefix(), GatewayBearerAuthConfigProperties.PREFIX + ".role-prefix is required");

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withIssuerLocation(properties.getIssuerUri()).build();
        OAuth2TokenValidator<Jwt> issuerAndTimeValidator =
                JwtValidators.createDefaultWithIssuer(properties.getIssuerUri());
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(properties.getAudience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Required audience is missing", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerAndTimeValidator, audienceValidator));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter bearerJwtAuthenticationConverter(GatewayBearerAuthConfigProperties properties) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName(properties.getUsernameClaim());
        converter.setJwtGrantedAuthoritiesConverter(jwt -> mappedAuthorities(jwt, properties));
        return converter;
    }

    private static Collection<GrantedAuthority> mappedAuthorities(
            Jwt jwt, GatewayBearerAuthConfigProperties properties) {
        Object roleClaim = nestedClaim(jwt.getClaims(), properties.getRolesClaim());
        List<?> sourceRoles;
        if (roleClaim instanceof Collection<?> values) {
            sourceRoles = List.copyOf(values);
        } else if (roleClaim instanceof String value) {
            sourceRoles = List.of(value);
        } else {
            sourceRoles = List.of();
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Object sourceRole : sourceRoles) {
            if (!(sourceRole instanceof String role)) continue;
            String mappedRole = properties.getRoleMappings().get(role);
            if (mappedRole != null && !mappedRole.isBlank()) {
                authorities.add(new SimpleGrantedAuthority(properties.getRolePrefix() + mappedRole));
            }
        }
        return authorities;
    }

    private static Object nestedClaim(Map<String, Object> claims, String path) {
        Object current = claims;
        for (String segment : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) return null;
            current = map.get(segment);
        }
        return current;
    }
}
