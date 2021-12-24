/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog;

import org.geoserver.cloud.config.catalog.GeoServerBackendConfigurer;
import org.geoserver.cloud.config.catalogclient.CatalogClientBackendConfigurer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration @EnableAutoConfiguration} auto-configuration to use the {@code
 * catalog-service} microservice as the {@link GeoServerBackendConfigurer catalog and configuration
 * backend} through the {@link CatalogClientBackendConfigurer}
 *
 * <p>Engages if enabled and configured as described by the {@link CatalogService} configuration
 * properties under the {@code geoserver.backend.catalog-service} prefix.
 *
 * @see ConditionalOnCatalogServiceClientEnabled
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnCatalogServiceClientEnabled
@Import(CatalogClientBackendConfigurer.class)
public class CatalogClientBackendAutoConfiguration {}
