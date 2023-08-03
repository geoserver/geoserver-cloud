/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.datadir;

import org.geoserver.cloud.autoconfigure.catalog.backend.core.DefaultUpdateSequenceAutoConfiguration;
import org.geoserver.cloud.config.catalog.backend.datadirectory.DataDirectoryBackendConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration(before = DefaultUpdateSequenceAutoConfiguration.class)
@ConditionalOnDataDirectoryEnabled
@Import(DataDirectoryBackendConfiguration.class)
public class DataDirectoryAutoConfiguration {}
