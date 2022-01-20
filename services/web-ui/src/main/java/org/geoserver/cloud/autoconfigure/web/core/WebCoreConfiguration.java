/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.core;

import org.apache.wicket.protocol.http.WicketServlet;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.GeoServerWicketServlet;
import org.geoserver.web.HeaderContribution;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration(proxyBeanMethods = true)
@ImportResource( //
    reader = FilteringXmlBeanDefinitionReader.class, //
    locations = { //
        "jar:gs-web-core-.*!/applicationContext.xml" //
    }
)
public class WebCoreConfiguration {

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

    public @Bean HeaderContribution geoserverCloudCssTheme() {
        HeaderContribution contribution = new HeaderContribution();
        contribution.setScope(GeoServerBasePage.class);
        contribution.setCSSFilename("geoserver-cloud.css");
        // contribution.setFaviconFilename("favicon.ico");
        return contribution;
    }
}
