/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.restconfig;

import org.geoserver.cloud.catalog.GeoServerCatalogConfig;
import org.geoserver.cloud.config.main.GeoServerSecurityConfiguration;
import org.geoserver.cloud.config.main.UrlProxifyingConfiguration;
import org.geoserver.cloud.core.GeoServerServletConfig;
import org.geoserver.rest.security.RestConfigXStreamPersister;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

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
@Import({ //
    GeoServerCatalogConfig.class, //
    GeoServerSecurityConfiguration.class, //
    GeoServerServletConfig.class, //
    UrlProxifyingConfiguration.class //
})
@ComponentScan(basePackageClasses = org.geoserver.rest.AbstractGeoServerController.class)
public class RestConfigApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(RestConfigApplication.class, args);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    //	  <bean id="restConfigXStreamPersister"
    // class="org.geoserver.rest.security.RestConfigXStreamPersister" />
    public @Bean RestConfigXStreamPersister restConfigXStreamPersister() {
        return new RestConfigXStreamPersister();
    }
}
