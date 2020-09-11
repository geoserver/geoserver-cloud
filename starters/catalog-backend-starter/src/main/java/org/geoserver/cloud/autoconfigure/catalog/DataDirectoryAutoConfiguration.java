/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog;

import org.geoserver.cloud.config.datadirectory.DataDirectoryBackendConfigurer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnDataDirectoryEnabled
@Import(DataDirectoryBackendConfigurer.class)
public class DataDirectoryAutoConfiguration extends AbstractBackendAutoConfiguration {}
