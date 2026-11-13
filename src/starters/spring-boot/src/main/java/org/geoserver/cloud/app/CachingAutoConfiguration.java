/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.app;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Enables Spring caching in the GeoServer services. PMTiles and GeoServer ACL keep their caches in the Spring
 * {@link CacheManager} configured by {@code geoserver_spring_boot_profiles.yml}. Spring Boot creates this
 * {@code CacheManager} only when caching is enabled.
 *
 * <p>The config and gateway services do not include GeoServer, and their load balancer manages its own cache.
 */
@AutoConfiguration(before = CacheAutoConfiguration.class)
@ConditionalOnClass(name = "org.geoserver.platform.GeoServerExtensions")
@EnableCaching
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class CachingAutoConfiguration {}
