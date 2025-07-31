/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.main;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.cloud.event.security.SecurityConfigChanged;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.security.CloudGeoServerSecurityFilterChainProxy;
import org.geoserver.security.CloudGeoServerSecurityManager;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(
        locations = "jar:gs-main-.*!/applicationSecurityContext.xml",
        excludes = {"authenticationManager", "filterChainProxy"})
@Import(GeoServerMainSecurityConfiguration_Generated.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.security")
public class GeoServerMainSecurityConfiguration {

    public @PostConstruct void log() {
        log.info("GeoServer main security configuration loaded");
    }

    /**
     * @since 1.3, required since geoserver 2.23.2
     */
    @Bean
    @ConditionalOnMissingBean
    XStreamPersisterFactory xstreamPersisterFactory() {
        return new XStreamPersisterFactory();
    }

    /**
     * Provide bean excluded in {@code @TranspileXmlConfig}
     */
    @Bean
    GeoServerSecurityFilterChainProxy filterChainProxy(GeoServerSecurityManager sm) {
        return new CloudGeoServerSecurityFilterChainProxy(sm);
    }

    /**
     * Provide bean excluded in {@code @TranspileXmlConfig}
     * <p>
     * Override the {@code authenticationManager} bean defined in {@code gs-main}'s
     * {@code
     * applicationSecurityContext.xml} with a version that notifies other services
     * of any security configuration change, and listens to remote events from other
     * services in order to {@link GeoServerSecurityManager#reload() reload} the
     * config.
     *
     * @param lock
     *
     * @return {@link CloudGeoServerSecurityManager}
     */
    @Bean(name = {"authenticationManager", "geoServerSecurityManager"})
    @DependsOn({"extensions"})
    CloudGeoServerSecurityManager cloudAuthenticationManager( //
            @Lazy GeoServerConfigurationLock lock,
            GeoServerDataDirectory dataDir, //
            ApplicationEventPublisher localContextPublisher, //
            UpdateSequence updateSequence, //
            List<GeoServerAuthenticationProvider> authProviders //
            ) throws Exception {

        Consumer<SecurityConfigChanged> publisher = localContextPublisher::publishEvent;
        Supplier<Long> updateSequenceIncrementor = updateSequence::nextValue;

        return new CloudGeoServerSecurityManager(lock, dataDir, publisher, updateSequenceIncrementor, authProviders);
    }
}
