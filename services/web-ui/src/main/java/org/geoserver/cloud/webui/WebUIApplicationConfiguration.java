/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.webui;

import org.apache.wicket.protocol.http.WicketServlet;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.web.GeoServerWicketServlet;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@EnableAutoConfiguration(
    exclude = { //
        DataSourceAutoConfiguration.class, //
        DataSourceTransactionManagerAutoConfiguration.class, //
        HibernateJpaAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        UserDetailsServiceAutoConfiguration.class, //
        ManagementWebSecurityAutoConfiguration.class, //
        LdapAutoConfiguration.class
    }
)
@ImportResource( //
    reader = FilteringXmlBeanDefinitionReader.class, //
    locations = { //
        "jar:gs-web-core-.*!/applicationContext.xml", //
        "jar:gs-web-sec-core-.*!/applicationContext.xml", //
        "jar:gs-web-sec-jdbc-.*!/applicationContext.xml", //
        "jar:gs-web-sec-ldap-.*!/applicationContext.xml", //
        "jar:gs-web-rest-.*!/applicationContext.xml", //
        "jar:gs-web-wps-.*!/applicationContext.xml", //
        "jar:gs-web-demo-.*!/applicationContext.xml", //
        "jar:gs-web-wcs-.*!/applicationContext.xml", //
        "jar:gs-web-wfs-.*!/applicationContext.xml", //
        "jar:gs-web-wms-.*!/applicationContext.xml", //
        "jar:gs-wms-.*!/applicationContext.xml", //
        "jar:gs-wfs-.*!/applicationContext.xml", //
        "jar:gs-wcs-.*!/applicationContext.xml" //
    } //
)
public class WebUIApplicationConfiguration {

    public @Bean GeoServerWicketServlet geoServerWicketServlet() {
        return new GeoServerWicketServlet();
    }

    /** Register the {@link WicketServlet} */
    public @Bean ServletRegistrationBean<GeoServerWicketServlet>
            geoServerWicketServletRegistration() {
        GeoServerWicketServlet servlet = geoServerWicketServlet();
        ServletRegistrationBean<GeoServerWicketServlet> registration;
        registration =
                new ServletRegistrationBean<GeoServerWicketServlet>(servlet, "/web", "/web/*");

        return registration;
    }
}
