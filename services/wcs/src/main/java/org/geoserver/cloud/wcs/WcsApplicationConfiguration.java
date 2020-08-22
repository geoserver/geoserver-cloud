/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.wcs;

import org.geoserver.cloud.catalog.GeoServerCatalogConfig;
import org.geoserver.cloud.core.FilteringXmlBeanDefinitionReader;
import org.geoserver.cloud.core.GeoServerServletConfig;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

@Configuration
@EnableAutoConfiguration(
    exclude = { //
        DataSourceAutoConfiguration.class, //
        DataSourceTransactionManagerAutoConfiguration.class, //
        HibernateJpaAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        UserDetailsServiceAutoConfiguration.class, //
        ManagementWebSecurityAutoConfiguration.class
    }
)
@Import({GeoServerCatalogConfig.class, GeoServerServletConfig.class})
@ImportResource( //
    reader = FilteringXmlBeanDefinitionReader.class, //
    locations = { //
        "jar:gs-wcs-.*!/applicationContext.xml", //
        "jar:gs-wcs1_0-.*!/applicationContext.xml", //
        "jar:gs-wcs1_1-.*!/applicationContext.xml", //
        "jar:gs-wcs2_0-.*!/applicationContext.xml" //
    }
)
public class WcsApplicationConfiguration {}
