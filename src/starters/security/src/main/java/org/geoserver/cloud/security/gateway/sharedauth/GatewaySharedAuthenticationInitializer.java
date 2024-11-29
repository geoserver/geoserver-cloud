/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.security.gateway.sharedauth;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.security.gateway.sharedauth.GatewaySharedAuthenticationFilter.Config;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Lock;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

/**
 * Upon a GeoServer {@link ContextLoadedEvent}, creates the {@link
 * GatewaySharedAuthenticationFilter} filter in the {@link GeoServerSecurityManager} configuration,
 * and appends the filter to the {@link SecurityManagerConfig#getFilterChain() filter chain}.
 *
 * @since 1.9
 */
@Slf4j(topic = "org.geoserver.cloud.security.gateway.sharedauth")
@RequiredArgsConstructor
public class GatewaySharedAuthenticationInitializer implements ApplicationListener<ContextLoadedEvent>, Ordered {

    @NonNull
    private final GeoServerSecurityManager securityManager;

    @Override
    public void onApplicationEvent(ContextLoadedEvent event) {
        if (!filterIsMissing()) {
            log.info("{} config is present.", GatewaySharedAuthenticationFilter.class.getSimpleName());
            return;
        }
        log.info(
                "{} config is missing, acquiring security lock",
                GatewaySharedAuthenticationFilter.class.getSimpleName());
        final Resource security = securityManager.security();
        final Lock securityLock = security.lock();
        try {
            if (filterIsMissing()) {
                Config filterConfig = createFilter();
                addToFilterChains(filterConfig);
            } else {
                log.info(
                        "{} config already present after acquiring security lock",
                        GatewaySharedAuthenticationFilter.class.getSimpleName());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            securityLock.release();
        }
    }

    private void addToFilterChains(Config filterConfig) throws Exception {
        final String filterName = filterConfig.getName();
        SecurityManagerConfig config = securityManager.getSecurityConfig();
        GeoServerSecurityFilterChain filterChain = config.getFilterChain();

        final Set<String> applyTo = Set.of("web", "rest", "gwc", "default");

        filterChain.getRequestChains().forEach(requestFiltersChain -> {
            String chainName = requestFiltersChain.getName();
            if (applyTo.contains(chainName)) {
                addToFilterChain(requestFiltersChain, filterName);
            }
        });

        securityManager.saveSecurityConfig(config);
        log.info("SecurityManagerConfig saved");
    }

    private void addToFilterChain(RequestFilterChain requestFiltersChain, String filterName) {
        List<String> filterNames = new ArrayList<>(requestFiltersChain.getFilterNames());
        if (filterNames.contains(filterName)) {
            log.info(
                    "Filter chain {} already contains {} -> {}",
                    requestFiltersChain.getName(),
                    filterName,
                    requestFiltersChain.getFilterNames().stream().collect(Collectors.joining(", ")));
        } else {
            if (filterNames.contains("anonymous")) {
                // anonymous must always be the last one if present
                int index = filterNames.indexOf("anonymous");
                filterNames.add(index, filterName);
            } else {
                filterNames.add(filterName);
            }

            requestFiltersChain.setFilterNames(filterNames);
            log.info(
                    "Applied filter {} to filter chain {} -> {}",
                    filterName,
                    requestFiltersChain.getName(),
                    requestFiltersChain.getFilterNames().stream().collect(Collectors.joining(", ")));
        }
    }

    private boolean filterIsMissing() {
        try {
            return securityManager
                    .listFilters(GatewaySharedAuthenticationFilter.class)
                    .isEmpty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Config createFilter() throws SecurityConfigException, IOException {
        var config = new GatewaySharedAuthenticationFilter.Config();
        securityManager.saveFilter(config);
        log.info("Created {}", GatewaySharedAuthenticationFilter.class.getSimpleName());
        return config;
    }

    /**
     * @return {@link Ordered#LOWEST_PRECEDENCE} to make sure it runs after {@link
     *     GeoServerSecurityManager#onApplicationEvent}
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
