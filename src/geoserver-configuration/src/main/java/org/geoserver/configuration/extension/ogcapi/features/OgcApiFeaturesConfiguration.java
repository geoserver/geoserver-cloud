/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.ogcapi.features;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.configuration.extension.ogcapi.core.OgcApiCoreConfiguration;
import org.geoserver.configuration.extension.ogcapi.core.OgcApiCoreWebConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration class for OGC API Features service
 *
 * @see OgcApiCoreConfiguration
 * @see OgcApiCoreWebConfiguration
 */
@Configuration
@Import(OgcApiCoreConfiguration.class)
@ImportFilteredResource("jar:gs-ogcapi-features-.*!/applicationContext.xml")
public class OgcApiFeaturesConfiguration {}
