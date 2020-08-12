/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.wms;

import org.geoserver.cloud.catalog.GeoServerCatalogConfig;
import org.geoserver.cloud.core.FilteringXmlBeanDefinitionReader;
import org.geoserver.cloud.core.GeoServerServletConfig;
import org.geoserver.cloud.core.UrlProxifyingConfiguration;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geoserver.wfs.xml.GML3OutputFormat;
import org.geoserver.wfs.xml.v1_1_0.WFS;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication(
    exclude = { //
        DataSourceAutoConfiguration.class, //
        DataSourceTransactionManagerAutoConfiguration.class, //
        HibernateJpaAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        UserDetailsServiceAutoConfiguration.class, //
        ManagementWebSecurityAutoConfiguration.class
    }
)
@Import({
    GeoServerCatalogConfig.class,
    GeoServerServletConfig.class,
    UrlProxifyingConfiguration.class
})
@ImportResource( //
    reader = FilteringXmlBeanDefinitionReader.class, //
    locations = "jar:gs-wms-.*!/applicationContext.xml" //
)
public class WmsApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(WmsApplication.class, args);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public @Bean WFSConfiguration wfsConfiguration(GeoServer geoServer) {
        FeatureTypeSchemaBuilder schemaBuilder = new FeatureTypeSchemaBuilder.GML3(geoServer);
        return new WFSConfiguration(geoServer, schemaBuilder, new WFS(schemaBuilder));
    }

    public @Bean GML3OutputFormat gml3OutputFormat(
            GeoServer geoServer, WFSConfiguration configuration) {
        return new GML3OutputFormat(geoServer, configuration);
    }
}
