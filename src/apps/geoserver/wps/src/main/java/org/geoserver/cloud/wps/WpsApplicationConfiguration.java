/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.wps;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.virtualservice.VirtualServiceVerifier;
import org.geoserver.configuration.core.wcs.WCSConfiguration;
import org.geoserver.configuration.core.wfs.WFSCoreConfiguration;
import org.geoserver.configuration.extension.dxf.DxfWpsConfiguration;
import org.geoserver.configuration.extension.wps.WPSCoreConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @see WPSCoreConfiguration
 */
@Configuration
@Import({WPSCoreConfiguration.class, DxfWpsConfiguration.class, WCSConfiguration.class, WFSCoreConfiguration.class})
public class WpsApplicationConfiguration {

    @Bean
    VirtualServiceVerifier virtualServiceVerifier(@Qualifier("rawCatalog") Catalog catalog) {
        return new VirtualServiceVerifier(catalog);
    }
}
