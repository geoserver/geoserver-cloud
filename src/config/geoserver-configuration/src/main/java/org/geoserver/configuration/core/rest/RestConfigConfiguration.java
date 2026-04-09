/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.core.rest;

import org.geoserver.rest.RestConfiguration;
import org.geoserver.spring.config.annotations.ComponentScanStrategy;
import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Core Spring configuration for the GeoServer Cloud REST microservice.
 *
 * <p>This class extends the standard GeoServer {@link RestConfiguration} to adapt the Spring MVC environment for
 * cloud-native microservices. It is responsible for:
 *
 * <ul>
 *   <li>Avoid component scanning for GeoServer REST controllers, replaced by transpiled bean methods in
 *       {@link RestConfigConfiguration_Generated.ComponentScannedBeans}
 *   <li>Registering specialized filters like {@link RestRequestPathInfoFilter} and {@link NpeAwareSuffixStripFilter} to
 *       ensure the REST API receives requests in the expected format (servlet path and path info).
 *   <li>Exclude component scanning of controllers in {@code gs-restconfig-wcs}, {@code gs-restconfig-wfs},
 *       {@code gs-restconfig-wms}, and {@code gs-restconfig-wmts}. Those modules have no {@code applicationContext.xml}
 *       and there's a split-package issue amongst them and {@code gs-restconfig}
 * </ul>
 *
 * @see RestConfigConfiguration_Generated
 * @see RestConfigConfiguration_Generated.ComponentScans
 * @see RestconfigWcsConfiguration
 * @see RestconfigWfsConfiguration
 * @see RestconfigWmsConfiguration
 * @see RestconfigWmtsConfiguration
 */
@Configuration(proxyBeanMethods = false)
@Import({RestConfiguration.class, RestConfigConfiguration_Generated.class})
@TranspileXmlConfig(
        locations = {
            "jar:gs-rest-.*!/applicationContext.xml",
            "jar:gs-restconfig-[0-9]+.*!/applicationContext.xml",
            // has no applicationContext.xml "jar:gs-restconfig-wcs-[0-9]+.*!/applicationContext.xml",
            // has no applicationContext.xml "jar:gs-restconfig-wfs-[0-9]+.*!/applicationContext.xml",
            // has no applicationContext.xml "jar:gs-restconfig-wms-[0-9]+.*!/applicationContext.xml",
            // has no applicationContext.xml "jar:gs-restconfig-wmts-[0-9]+.*!/applicationContext.xml",
        },
        excludes = { //
            "org.geoserver.rest.RestConfiguration", // this configuration
            "org.geoserver.rest.SuffixStripFilter", // replaced with NpeAwareSuffixStripFilter
            "org.geoserver.rest.service.WCSSettingsController", // in RestconfigWcsConfiguration
            "org.geoserver.rest.service.WFSSettingsController", // in RestconfigWfsConfiguration
            "org.geoserver.rest.service.WMSSettingsController", // in RestconfigWmsConfiguration
            "org.geoserver.rest.service.WMTSSettingsController", // in RestconfigWmtsConfiguration
        },
        publicAccess = true,
        componentScanStrategy = ComponentScanStrategy.GENERATE)
public class RestConfigConfiguration {}
