/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.core;

import org.apache.wicket.protocol.http.WicketServlet;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.GeoServerWicketServlet;
import org.geoserver.web.HeaderContribution;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = true)
@ImportFilteredResource({
    "jar:gs-web-core-.*!/applicationContext.xml#name=" + WebCoreConfiguration.EXCLUDED_BEANS_PATTERN, //
    "jar:gs-web-rest-.*!/applicationContext.xml" //
})
public class WebCoreConfiguration {

    static final String EXCLUDED_BEANS_PATTERN = "^(?!logsPage).*$";

    @Bean
    GeoServerWicketServlet geoServerWicketServlet() {
        return new GeoServerWicketServlet();
    }

    /** Register the {@link WicketServlet} */
    @Bean
    ServletRegistrationBean<GeoServerWicketServlet> geoServerWicketServletRegistration() {
        GeoServerWicketServlet servlet = geoServerWicketServlet();
        return new ServletRegistrationBean<>(servlet, "/web", "/web/*");
    }

    @Bean
    HeaderContribution geoserverCloudCssTheme() {
        HeaderContribution contribution = new HeaderContribution();
        contribution.setScope(GeoServerBasePage.class);
        contribution.setCSSFilename("geoserver-cloud.css");
        return contribution;
    }
}
