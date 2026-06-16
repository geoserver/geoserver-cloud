/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.main;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.cloud.event.security.SecurityConfigChanged;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.configuration.core.main.GeoServerMainSecurityConfiguration;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.security.CloudGeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.filter.DisabledSecurityFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;

/**
 * Enables {@code gs-main} security.
 *
 * <p>{@link GeoServerMainSecurityConfiguration} excludes {@link GeoServerSecurityManager}, we provide an alternative
 * implementation here
 *
 * @see GeoServerMainSecurityConfiguration
 * @see CloudGeoServerSecurityManager
 */
@AutoConfiguration
@Import(GeoServerMainSecurityConfiguration.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.security")
public class GeoServerMainSecurityAutoConfiguration {

    /** @since 1.3, required since geoserver 2.23.2 */
    @Bean
    @ConditionalOnMissingBean
    XStreamPersisterFactory xstreamPersisterFactory() {
        return new XStreamPersisterFactory();
    }

    /**
     * Provide bean excluded in {@code @TranspileXmlConfig}
     *
     * <p>Overrides the {@code authenticationManager} bean defined in {@code gs-main}'s
     * {@code applicationSecurityContext.xml} with a version that notifies other services of any security configuration
     * change, and listens to remote events from other services in order to {@link GeoServerSecurityManager#reload()
     * reload} the config.
     *
     * @param lock
     * @return {@link CloudGeoServerSecurityManager}
     */
    @Bean(name = {"authenticationManager", "geoServerSecurityManager"})
    @DependsOn({"extensions"})
    CloudGeoServerSecurityManager cloudAuthenticationManager( //
            @Lazy GeoServerConfigurationLock lock,
            GeoServerDataDirectory dataDir, //
            ApplicationEventPublisher localContextPublisher, //
            UpdateSequence updateSequence, //
            List<AuthenticationProvider> additionalAuthenticationProviders)
            throws Exception {

        if (additionalAuthenticationProviders.isEmpty()) {
            log.info("No additional authentication providers found");
        } else {
            log.info(
                    "Using additional authentication providers {}",
                    additionalAuthenticationProviders.stream()
                            .map(Object::getClass)
                            .map(Class::getCanonicalName)
                            .collect(Collectors.joining(", ")));
        }
        Consumer<SecurityConfigChanged> publisher = localContextPublisher::publishEvent;
        Supplier<Long> updateSequenceIncrementor = updateSequence::nextValue;

        return new CloudGeoServerSecurityManager(
                lock, dataDir, publisher, updateSequenceIncrementor, additionalAuthenticationProviders);
    }

    /**
     * Provides the {@code disabledFilter} bean excluded from {@link GeoServerMainSecurityConfiguration}'s transpiled
     * configuration.
     *
     * <p>{@code gs-main}'s {@code applicationSecurityContext.xml} declares a {@link DisabledSecurityFilter} named
     * {@code disabledFilter}. GeoServer's security subsystem injects it into a request filter chain that has lost its
     * last authentication filter, as a fail-closed stand-in that denies access with {@code 403}. The bean must remain
     * available for GeoServer's internal filter lookup, but it must not act as a top-level servlet filter; see
     * {@link #disabledSecurityFilterRegistration(DisabledSecurityFilter)}.
     */
    @Bean(name = "disabledFilter")
    DisabledSecurityFilter disabledFilter() {
        return new DisabledSecurityFilter();
    }

    /**
     * Keeps the {@link #disabledFilter()} bean out of the servlet filter chain.
     *
     * <p>In a servlet container GeoServer wires only {@code filterChainProxy} as a servlet filter, but Spring Boot
     * registers every {@link jakarta.servlet.Filter} bean as a servlet filter mapped to {@code /*}. Without this
     * disabled registration the fail-closed {@link DisabledSecurityFilter} would deny every request with {@code 403}.
     */
    @Bean
    FilterRegistrationBean<DisabledSecurityFilter> disabledSecurityFilterRegistration(
            DisabledSecurityFilter disabledFilter) {
        FilterRegistrationBean<DisabledSecurityFilter> registration = new FilterRegistrationBean<>(disabledFilter);
        registration.setEnabled(false);
        return registration;
    }
}
