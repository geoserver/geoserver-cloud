/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.server.config;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.catalog.server.api.v1.ReactiveCatalogController;
import org.geoserver.cloud.catalog.server.config.CatalogServerConfigProperties.SchedulerConfig;
import org.geoserver.cloud.catalog.server.service.ReactiveCatalog;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
@ComponentScan(basePackageClasses = {ReactiveCatalog.class, ReactiveCatalogController.class})
@Slf4j
public class CatalogServerConfiguration implements WebFluxConfigurer {

    /** Global Jackson ObjectMapper, configured in {@link #configureHttpMessageCodecs} */
    // private @Autowired ObjectMapper objectMapper;

    @ConfigurationProperties(prefix = "geoserver.catalog-service")
    @Bean
    CatalogServerConfigProperties applicationConfig() {
        return new CatalogServerConfigProperties();
    }

    /**
     * Configures the reactive Scheduler thread pool on which {@link ReactiveCatalogService}
     * performs the blocking catalog calls
     */
    @Bean
    Scheduler catalogScheduler() {
        CatalogServerConfigProperties config = applicationConfig();
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

    /**
     * Configures the {@link ObjectMapper} used by {@link Jackson2JsonEncoder} and {@link
     * Jackson2JsonDecoder} to handle http message payloads, especially in order to set {@link
     * SerializationFeature#WRAP_ROOT_VALUE} to {@code false}, or the responses are like <code>
     * {"WorkspaceInfoImpl" : {"Workspace" : {"WorkspaceInfo" : {...}}}}
     * </code> instead of like <code>
     * {"WorkspaceInfo" : {...}}
     * </code>
     */
    // @Bean ObjectMapper objectMapper() {
    // ObjectMapper objectMapper = new ObjectMapper();
    // objectMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
    // objectMapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, false);
    //
    // objectMapper.setDefaultPropertyInclusion(Include.NON_EMPTY);
    //
    // objectMapper.configure(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED,
    // false);
    // objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    // objectMapper.findAndRegisterModules();
    // return objectMapper;
    // }
}
