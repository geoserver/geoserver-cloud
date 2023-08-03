/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.server;

import org.geoserver.cloud.catalog.server.config.CatalogServerConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@Import(CatalogServerConfiguration.class)
public class CatalogServerAutoConfiguration {}
