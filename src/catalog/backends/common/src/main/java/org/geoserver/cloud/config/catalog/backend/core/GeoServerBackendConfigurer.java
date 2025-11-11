/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.config.catalog.backend.core;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Marker interface for GeoServer catalog and configuration backend storage implementations.
 *
 * <h2>Overview</h2>
 * <p>While vanilla GeoServer defaults to file-based "data directory" storage, GeoServer Cloud
 * requires explicit backend configuration through Spring Boot's externalized configuration properties.
 * This marker interface identifies configuration classes that provide complete catalog and configuration
 * storage implementations for specific backend technologies (e.g., data directory, PostgreSQL).
 *
 * <h2>Usage</h2>
 * <p>Backend implementations should extend this class and be annotated with {@code @Configuration(proxyBeanMethods = false)}.
 * This allows implementations to use modern Spring dependency injection patterns with constructor or method
 * parameter injection, avoiding the limitations of {@code proxyBeanMethods = true} where bean methods
 * must call each other directly.
 *
 * <h2>Required Beans</h2>
 * <p>A backend configuration class extending {@link GeoServerBackendConfigurer} must provide the following
 * core beans to establish a complete GeoServer catalog and configuration storage backend:
 * <ul>
 *  <li>{@code ExtendedCatalogFacade catalogFacade} - Core catalog persistence layer
 *  <li>{@code GeoServerFacade geoserverFacade} - Configuration persistence layer
 *  <li>{@code GeoServerConfigurationLock configurationLock} - Distributed locking for configuration changes
 *  <li>{@code UpdateSequence updateSequence} - Tracks configuration version for WMS/WFS capabilities caching
 *  <li>{@code GeoServerLoader geoServerLoaderImpl} - Loads initial catalog and configuration on startup
 *  <li>{@code ResourceStore resourceStoreImpl} - Storage for resources (styles, icons, templates, etc.)
 *  <li>{@code GeoServerResourceLoader resourceLoader} - High-level resource access API
 * </ul>
 *
 * <h2>Auto-Configuration</h2>
 * <p>Backend configuration classes should be registered as Spring Boot auto-configurations by adding them
 * to {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}. They should
 * implement conditional activation using annotations such as:
 * <ul>
 *  <li>{@link ConditionalOnProperty @ConditionalOnProperty} - Enable/disable via configuration properties
 *  <li>{@link ConditionalOnClass @ConditionalOnClass} - Require specific classes on classpath
 *  <li>Custom conditional annotations for backend-specific requirements
 * </ul>
 *
 * <h2>Implementation Examples</h2>
 * <ul>
 *  <li>{@code DataDirectoryBackendConfiguration} - Traditional file-based storage
 *  <li>{@code PgconfigBackendConfiguration} - PostgreSQL-based storage with JNDI support
 * </ul>
 *
 * @since 1.0
 * @see org.geoserver.cloud.config.catalog.backend.datadirectory.DataDirectoryBackendConfiguration
 * @see org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigBackendConfiguration
 */
public abstract class GeoServerBackendConfigurer {
    // Marker class - no methods required
}
