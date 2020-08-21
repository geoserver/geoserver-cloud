/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.wfs;

import org.geoserver.cloud.catalog.GeoServerCatalogConfig;
import org.geoserver.cloud.core.FilteringXmlBeanDefinitionReader;
import org.geoserver.cloud.core.GeoServerServletConfig;
import org.geoserver.wfs.WFSInfoImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

@Configuration
@Import({GeoServerCatalogConfig.class, GeoServerServletConfig.class})
@ConditionalOnClass(value = WFSInfoImpl.class)
@ImportResource( //
    reader = FilteringXmlBeanDefinitionReader.class, //
    locations = "jar:gs-wfs-.*!/applicationContext.xml" //
)
public class WfsConfiguration {}
