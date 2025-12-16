/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.core;

import org.geoserver.cloud.autoconfigure.core.GeoServerMainAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.demo.WebDemosAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.extension.WebExtensionsAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.rest.WebRestAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.security.WebSecurityAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.tools.WebToolsAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.wcs.WebWcsAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.wfs.WebWfsAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.wms.WebWmsAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.wps.WebWpsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

@AutoConfiguration(after = {GeoServerMainAutoConfiguration.class})
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@EnableConfigurationProperties(WebUIConfigurationProperties.class)
@Import({ //
    WebCoreAutoConfiguration.class, // this one is mandatory
    WebSecurityAutoConfiguration.class,
    WebRestAutoConfiguration.class,
    WebWfsAutoConfiguration.class,
    WebWmsAutoConfiguration.class,
    WebWcsAutoConfiguration.class,
    WebWpsAutoConfiguration.class,
    WebExtensionsAutoConfiguration.class,
    WebDemosAutoConfiguration.class,
    WebToolsAutoConfiguration.class
})
public class WebUIApplicationAutoConfiguration {

    @Bean
    WebUIInitializer webUIDefaultsInitializer(Environment env) {
        return new WebUIInitializer(env);
    }

    /**
     * A {@link WicketComponentFilter} that effectively hides pages (returns 404)
     * for components that are disabled by {@link WebUIConfigurationProperties}
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    FilterRegistrationBean<WicketComponentFilter> wicketComponentFilter(WebUIConfigurationProperties config) {

        FilterRegistrationBean<WicketComponentFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new WicketComponentFilter(config));
        registrationBean.addUrlPatterns("/web/*");

        return registrationBean;
    }
}
