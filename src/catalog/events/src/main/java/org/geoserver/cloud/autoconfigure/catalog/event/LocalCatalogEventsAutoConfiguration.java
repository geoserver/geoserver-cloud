/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.event;

import org.geoserver.cloud.config.catalog.events.CatalogApplicationEventsConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration auto-configuration} for {@link
 * CatalogApplicationEventsConfiguration}
 */
@Configuration
@ConditionalOnCatalogEvents
@Import(CatalogApplicationEventsConfiguration.class)
public class LocalCatalogEventsAutoConfiguration {}
