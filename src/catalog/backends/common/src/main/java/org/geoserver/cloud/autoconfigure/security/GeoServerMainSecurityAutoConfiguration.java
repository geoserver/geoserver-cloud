/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.security;

import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.catalog.backend.core.GeoServerBackendAutoConfiguration;
import org.geoserver.configuration.core.main.GeoServerMainSecurityConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * @see GeoServerMainSecurityConfiguration
 */
@AutoConfiguration(after = GeoServerBackendAutoConfiguration.class)
@Import(GeoServerMainSecurityConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.security")
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class GeoServerMainSecurityAutoConfiguration {}
