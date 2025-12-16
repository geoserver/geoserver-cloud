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
 * @see OgcApiCoreConfiguration
 * @see OgcApiCoreWebConfiguration
 * @see OgcApiFeaturesConfiguration
 * @see OgcApiFeaturesWebConfiguration_Generated
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(locations = "jar:gs-web-features-.*!/applicationContext.xml")
@Import({
    // OgcApiFeaturesConfiguration must be present to provide `APIServiceFactoryBean featuresServiceFactory()`
    // or it won't show up in the home page
    OgcApiCoreConfiguration.class,
    OgcApiCoreWebConfiguration.class,
    OgcApiFeaturesConfiguration.class,
    OgcApiFeaturesWebConfiguration_Generated.class
})
public class OgcApiFeaturesWebConfiguration {}
