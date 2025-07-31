/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.ogcapi.features;

import org.geoserver.configuration.extension.ogcapi.core.OgcApiCoreConfiguration;
import org.geoserver.configuration.extension.ogcapi.core.OgcApiCoreWebConfiguration;
import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration class for OGC API Features service
 *
 * @see OgcApiCoreConfiguration
 * @see OgcApiCoreWebConfiguration
 */
@Configuration
@TranspileXmlConfig(locations = "jar:gs-ogcapi-features-.*!/applicationContext.xml")
@Import({OgcApiCoreConfiguration.class, OgcApiFeaturesConfiguration_Generated.class})
public class OgcApiFeaturesConfiguration {}
