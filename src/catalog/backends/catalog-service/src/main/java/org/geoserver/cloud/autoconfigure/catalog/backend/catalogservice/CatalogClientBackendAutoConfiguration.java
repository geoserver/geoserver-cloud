/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.catalogservice;

import org.geoserver.cloud.autoconfigure.catalog.backend.core.DefaultUpdateSequenceAutoConfiguration;
import org.geoserver.cloud.config.catalog.backend.catalogservice.CatalogClientBackendConfigurer;
import org.geoserver.cloud.config.catalog.backend.core.GeoServerBackendConfigurer;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
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
@AutoConfigureBefore(DefaultUpdateSequenceAutoConfiguration.class)
public class CatalogClientBackendAutoConfiguration {}
