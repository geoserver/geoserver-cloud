/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.webui;

import org.apache.wicket.protocol.http.WicketServlet;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.web.GeoServerWicketServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration(proxyBeanMethods = true)
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
