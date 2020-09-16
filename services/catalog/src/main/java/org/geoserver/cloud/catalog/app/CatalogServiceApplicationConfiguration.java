/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.app;

import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.catalog.api.v1.AbstractCatalogInfoController;
import org.geoserver.cloud.catalog.app.CatalogServiceApplicationProperties.SchedulerConfig;
import org.geoserver.cloud.catalog.service.ReactiveCatalogService;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
@EnableAutoConfiguration
@ComponentScan(
    basePackageClasses = {ReactiveCatalogService.class, AbstractCatalogInfoController.class}
)
@Slf4j
public class CatalogServiceApplicationConfiguration implements WebFluxConfigurer {

    private @Autowired(required = true) @Qualifier("rawCatalog") Catalog rawCatalog;
    private @Autowired(required = true) @Qualifier("xstreamPersisterFactory")
    XStreamPersisterFactory xstreamPersisterFactory;

    @ConfigurationProperties(prefix = "geoserver.catalog-service")
    public @Bean CatalogServiceApplicationProperties applicationConfig() {
        return new CatalogServiceApplicationProperties();
    }

    /**
     * Configures the reactive Scheduler thread pool on which {@link ReactiveCatalogService}
     * performs the blocking catalog calls
     */
    public @Bean Scheduler catalogScheduler() {
        CatalogServiceApplicationProperties config = applicationConfig();
        SchedulerConfig schedulerConfig = config.getIoThreads();
        int maxThreads = schedulerConfig.getMaxSize();
        int maxQueued = schedulerConfig.getMaxQueued();
        if (maxThreads <= 0) {
            log.warn(SchedulerConfig.buildInvalidMaxSizeMessage(maxThreads));
            maxThreads = SchedulerConfig.DEFAULT_MAX_SIZE;
        }
        if (maxQueued <= 0) {
            log.warn(SchedulerConfig.buildInvalidMaxQueuedMessage(maxQueued));
            maxQueued = SchedulerConfig.DEFAULT_MAX_QUEUED;
        }
        log.info("configured catalogScheduler: maxThreads={}, maxQueued={}", maxThreads, maxQueued);
        return Schedulers.newBoundedElastic(maxThreads, maxQueued, "catalogScheduler");
    }

    public @Override void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        //
    }
}
