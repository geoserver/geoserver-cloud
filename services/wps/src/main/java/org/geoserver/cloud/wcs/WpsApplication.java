package org.geoserver.cloud.wcs;

import org.geoserver.cloud.core.GeoServerServletConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
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
@Import(GeoServerServletConfig.class)
@EnableDiscoveryClient
public class WpsApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(WpsApplication.class, args);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }
}
