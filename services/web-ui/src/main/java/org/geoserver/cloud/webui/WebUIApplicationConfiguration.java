/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.webui;

import org.apache.wicket.protocol.http.WicketServlet;
import org.geoserver.cloud.catalog.GeoServerCatalogConfig;
import org.geoserver.cloud.core.FilteringXmlBeanDefinitionReader;
import org.geoserver.cloud.core.GeoServerServletConfig;
import org.geoserver.web.GeoServerWicketServlet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

@Configuration
@Import({GeoServerCatalogConfig.class, GeoServerServletConfig.class})
@ConditionalOnClass(value = GeoServerWicketServlet.class)
@ImportResource( //
    reader = FilteringXmlBeanDefinitionReader.class, //
    locations = {
        "jar:gs-web-.*!/applicationContext.xml", //
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
