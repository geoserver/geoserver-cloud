/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.restconfig;

import org.geoserver.cloud.core.GeoServerServletConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
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
    GeoServerServletConfig.class,
    org.geoserver.rest.RestConfiguration.class //
})
public class RestConfigApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(RestConfigApplication.class, args);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    // @ConditionalOnMissingBean(RequestMappingHandlerMapping.class)
    // public @Bean RequestMappingHandlerMapping requestMappingHandlerMapping() {
    // RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
    // return mapping;
    // }
}
