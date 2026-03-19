/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.core;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.GeoServerWicketServlet;
import org.geoserver.web.HeaderContribution;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the GeoServer Wicket-based Web UI.
 *
 * <p>Imports beans from upstream {@code gs-web-core}, {@code gs-theme}, and {@code gs-web-rest}, excluding:
 *
 * <ul>
 *   <li>{@code logsPage} - excluded to avoid conflicts
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@ImportFilteredResource({
    "jar:gs-web-core-.*!/applicationContext.xml#name=" + WebCoreConfiguration.EXCLUDED_BEANS_PATTERN, //
    "jar:gs-web-rest-.*!/applicationContext.xml", //
    "jar:gs-theme-.*!/applicationContext.xml" //
})
public class WebCoreConfiguration {

    /** Pattern to exclude {@code logsPage} from upstream imports. */
    static final String EXCLUDED_BEANS_PATTERN = "^(?!logsPage).*$";

    @Bean
    GeoServerWicketServlet geoServerWicketServlet() {
        return new GeoServerWicketServlet();
    }

    /** Contributes the GeoServer Cloud CSS theme to the Wicket UI. */
    @Bean
    HeaderContribution geoserverCloudCssTheme() {
        HeaderContribution contribution = new HeaderContribution();
        contribution.setScope(GeoServerBasePage.class);
        contribution.setCSSFilename("geoserver-cloud.css");
        return contribution;
    }
}
