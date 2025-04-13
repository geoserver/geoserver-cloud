/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.catalog.backend.core;

import org.geoserver.cloud.autoconfigure.geotools.GeoToolsHttpClientAutoConfiguration;
import org.geoserver.cloud.config.catalog.backend.core.CoreBackendConfiguration;
import org.geoserver.cloud.config.jndi.JNDIDataSourceConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Import;

/**
 * Groups imports to all the available backend auto-configurations
 *
 * <p>Interested auto-configuration classes can use a single reference to this class in {@link
 * AutoConfigureAfter @AutoConfigureAfter} without having to know what specific kind of backend
 * configurations there exist, and also we only need to register this single class in {@code
 * META-INF/spring.factories} regardless of how many backend configs there are in the future
 * (provided their configs are included in this class' {@code @Import} list)
 *
 * @see CoreBackendConfiguration
 */
@AutoConfiguration(after = {GeoToolsHttpClientAutoConfiguration.class, JNDIDataSourceConfiguration.class})
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@Import(CoreBackendConfiguration.class)
public class GeoServerBackendAutoConfiguration {}
