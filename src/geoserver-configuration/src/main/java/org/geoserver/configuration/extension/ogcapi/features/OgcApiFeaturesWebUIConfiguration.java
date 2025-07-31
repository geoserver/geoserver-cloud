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
 * @see OgcApiCoreConfiguration
 * @see OgcApiCoreWebConfiguration
 */
@Configuration(proxyBeanMethods = false)
@Import({OgcApiCoreConfiguration.class, OgcApiCoreWebConfiguration.class})
@ImportFilteredResource("jar:gs-web-features-.*!/applicationContext.xml")
public class OgcApiFeaturesWebUIConfiguration {}
