/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.vectorformats.pmtiles;

import java.util.Optional;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWMS;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.pmtiles.data.PMTilesPluginConfiguration;
import org.geoserver.pmtiles.data.PMTilesWmsIntegrationConfiguration;
import org.geoserver.pmtiles.web.data.PMTilesWebUIConfiguration;
import org.geotools.autoconfigure.vectorformats.DataAccessFactoryFilteringAutoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for the PMTiles plugin in GeoServer Cloud.
 *
 * <p>
 * This configuration class:
 *
 * <ul>
 * <li>Imports {@link PMTilesPluginConfiguration} which provides the core
 * PMTiles beans
 * <li>Integrates PMTiles' internal Caffeine caches with Spring's
 * {@link CacheManager} when available
 * </ul>
 *
 * <p>
 * The cache integration allows PMTiles cache metrics to be exposed through
 * Spring Actuator's {@code /actuator/metrics}, {@code /actuator/caches}, and
 * {@code /actuator/prometheus} endpoints.
 *
 * @see PMTilesPluginConfiguration
 * @see PMTilesWmsIntegrationConfiguration
 * @see PMTilesWebUIConfiguration
 * @see SpringCaffeineCacheManagerAdapter
 * @since 2.28.0
 */
@AutoConfiguration(after = DataAccessFactoryFilteringAutoConfiguration.class)
@Import({
    PMTilesPluginAutoConfiguration.Enabled.class,
    PMTilesPluginAutoConfiguration.WmsIntegration.class,
    PMTilesPluginAutoConfiguration.WebuiIntegration.class
})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.vectorformats.pmtiles")
public class PMTilesPluginAutoConfiguration {

    @Bean
    @SuppressWarnings("java:S125")
    ModuleStatusImpl pmtilesStoreExtension(@Value("${geoserver.extension.pmtiles.enabled:true}") boolean enabled) {
        ModuleStatusImpl module =
                new ModuleStatusImpl("gs-pmtiles-store", "PMTiles Store Extension", "PMtiles Store Extension");
        module.setAvailable(true);
        module.setEnabled(enabled);
        // set it on 3.x: module.setCategory(ModuleStatus.Category.COMMUNITY);
        return module;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnPMTiles
    @Import(PMTilesPluginConfiguration.class)
    static class Enabled {

        private Optional<CacheManager> springCacheManager;

        public Enabled(Optional<CacheManager> springCacheManager) {
            this.springCacheManager = springCacheManager;
        }

        /**
         * Sets up the cache manager integration after bean initialization.
         *
         * <p>
         * If Spring's {@link CaffeineCacheManager} is available, creates a
         * {@link SpringCaffeineCacheManagerAdapter} and sets it as the default for
         * {@link io.tileverse.cache.CacheManager}. This allows PMTiles' internal caches
         * to be managed and monitored through Spring's cache infrastructure.
         */
        @PostConstruct
        void setUpCacheManager() {
            boolean cacheManagerAdapted = false;
            if (springCacheManager.isPresent()) {
                CacheManager cacheManager = springCacheManager.get();
                if (cacheManager instanceof CaffeineCacheManager springCaffeineCacheManager) {
                    SpringCaffeineCacheManagerAdapter adapter;
                    adapter = new SpringCaffeineCacheManagerAdapter(springCaffeineCacheManager);
                    io.tileverse.cache.CacheManager.setDefault(adapter);
                    cacheManagerAdapted = true;
                }
            }
            log.info("PMTiles extension installed. CaffeineCacheManager integration: {}", cacheManagerAdapted);
        }
    }

    /**
     * Imports {@link PMTilesWmsIntegrationConfiguration} when {@link ConditionalOnGeoServerWMS} is satisfied
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnPMTiles
    @ConditionalOnGeoServerWMS
    @Import(PMTilesWmsIntegrationConfiguration.class)
    static class WmsIntegration {

        @PostConstruct
        void log() {
            log.info("PMTiles WMS extension installed");
        }
    }

    /**
     * Configuration for PMTiles extension that provides a data store configuration panel for the web admin interface.
     * <p>
     * Imports {@link PMTilesWebUIConfiguration} when {@link ConditionalOnGeoServerWebUI} is satisfied
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnPMTiles
    @ConditionalOnGeoServerWebUI
    @Import(PMTilesWebUIConfiguration.class)
    static class WebuiIntegration {

        @PostConstruct
        void log() {
            log.info("PMTiles WebUI extension installed");
        }
    }
}
