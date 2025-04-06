/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.datadir;

import org.geoserver.cloud.autoconfigure.catalog.backend.core.DefaultUpdateSequenceAutoConfiguration;
import org.geoserver.cloud.autoconfigure.geotools.GeoToolsHttpClientAutoConfiguration;
import org.geoserver.cloud.config.catalog.backend.datadirectory.DataDirectoryBackendConfiguration;
import org.geoserver.cloud.config.catalog.backend.datadirectory.DataDirectoryProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration(
        before = DefaultUpdateSequenceAutoConfiguration.class,
        after = GeoToolsHttpClientAutoConfiguration.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnDataDirectoryEnabled
@EnableConfigurationProperties(DataDirectoryProperties.class)
@Import(DataDirectoryBackendConfiguration.class)
public class DataDirectoryAutoConfiguration {}
