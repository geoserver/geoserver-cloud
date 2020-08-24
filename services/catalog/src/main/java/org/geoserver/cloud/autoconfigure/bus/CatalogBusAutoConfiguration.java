/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.bus;

import org.geoserver.cloud.bus.catalog.BusDisabledLogger;
import org.geoserver.cloud.bus.catalog.CatalogBusConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.bus.ConditionalOnBusEnabled;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** {@link EnableAutoConfiguration auto-configuration} for {@link CatalogBusConfiguration} */
@Configuration
@ConditionalOnBusEnabled
@AutoConfigureAfter(BusAutoConfiguration.class)
@Import({CatalogBusConfiguration.class, BusDisabledLogger.class})
public class CatalogBusAutoConfiguration {}
