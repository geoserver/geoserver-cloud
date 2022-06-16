/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.core;

import org.geoserver.cloud.autoconfigure.catalog.backend.core.GeoServerBackendAutoConfiguration;
import org.geoserver.cloud.config.catalog.backend.core.GeoServerBackendConfigurer;
import org.geoserver.cloud.config.main.GeoServerMainModuleConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the registration of the GeoServer main
 * module (as in the {@code gs-main.jar} file) spring beans to work on reactive (servlet-less)
 * applications.
 *
 * <p>Requires the correct configuration and enablement of a {@link GeoServerBackendConfigurer}
 *
 * @see GeoServerMainModuleConfiguration
 */
@Configuration
@AutoConfigureAfter(value = GeoServerBackendAutoConfiguration.class)
@Import({GeoServerMainModuleConfiguration.class})
public class ReactiveGeoServerMainAutoConfiguration {}
