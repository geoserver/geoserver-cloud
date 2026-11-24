/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.core;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.geoserver.cloud.autoconfigure.core.GeoServerMainAutoConfiguration;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.cloud.autoconfigure.web.demo.WebDemosAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.security.WebSecCoreAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.tools.WebToolsAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.wcs.WebWcsAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.wfs.WebWfsAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.wms.WebWmsAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.wps.WebWpsAutoConfiguration;
import org.geoserver.cloud.logging.profiles.LoggingProfiles;
import org.geoserver.configuration.core.web.WebCoreConfiguration;
import org.geoserver.configuration.core.web.WebRestConfiguration;
import org.geoserver.configuration.core.web.WebThemeConfiguration;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.HeaderContribution;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.Controller;

/**
 * @see WebCoreConfiguration
 * @see WebRestConfiguration
 * @see WebSecCoreAutoConfiguration
 * @see WebWmsAutoConfiguration
 */
@AutoConfiguration(after = {GeoServerMainAutoConfiguration.class})
@ConditionalOnGeoServerWebUI
@EnableConfigurationProperties(WebUIConfigurationProperties.class)
@Import({ //
    WebCoreConfiguration.class, // this one is mandatory
    WebThemeConfiguration.class,
    WebRestConfiguration.class,
    WebSecCoreAutoConfiguration.class,
    WebWfsAutoConfiguration.class,
    WebWmsAutoConfiguration.class,
    WebWcsAutoConfiguration.class,
    WebWpsAutoConfiguration.class,
    WebDemosAutoConfiguration.class,
    WebToolsAutoConfiguration.class
})
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class WebUIApplicationAutoConfiguration {

    /** Order of GeoServer's {@code webDispatcherMapping}, which sends the application root to its file publisher */
    private static final int WEB_DISPATCHER_MAPPING_ORDER = -50;

    @Bean
    WebUIInitializer webUIDefaultsInitializer(Environment env) {
        return new WebUIInitializer(env);
    }

    /** Lists the logging profiles of GeoServer Cloud in the Global Settings page */
    @Bean
    LoggingProfileChoices loggingProfileChoices(LoggingProfiles loggingProfiles) {
        return new LoggingProfileChoices(loggingProfiles);
    }

    /**
     * Defaults {@code GEOSERVER_NODE_OPTS} to a per-instance color badge, shown by the web UI to tell instances apart
     * in a cluster, unless an operator already set it.
     */
    @EventListener(ApplicationReadyEvent.class)
    void defaultNodeOpts(ApplicationReadyEvent event) {
        if (System.getProperty("GEOSERVER_NODE_OPTS") != null) {
            return;
        }
        ConfigurableEnvironment env = event.getApplicationContext().getEnvironment();
        String instanceId = env.getProperty("info.instance-id");
        String nodeOpts = "id:%s;color:FF0000".formatted(instanceId);
        System.setProperty("GEOSERVER_NODE_OPTS", nodeOpts);
    }

    /**
     * A {@link WicketComponentFilter} that effectively hides pages (returns 404) for components that are disabled by
     * {@link WebUIConfigurationProperties}
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    FilterRegistrationBean<WicketComponentFilter> wicketComponentFilter(WebUIConfigurationProperties config) {

        FilterRegistrationBean<WicketComponentFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new WicketComponentFilter(config));
        registrationBean.addUrlPatterns("/web/*");

        return registrationBean;
    }

    /**
     * Redirects the application root and {@code /index.html} to the web UI.
     *
     * <p>GeoServer's {@code webDispatcherMapping} sends both paths to its file publisher, ahead of the annotated
     * controllers. The publisher redirects the root to {@code index.html} and then looks for that page among the
     * servlet context resources: GeoServer's war file has it there, this application does not. This mapping comes first
     * and redirects to {@code web/}, as that page does.
     *
     * <p>The location is relative, as in that page. The browser resolves it against the address it requested, which
     * also holds behind a proxy serving the application under another host or path.
     */
    @Bean
    SimpleUrlHandlerMapping webUiRootRedirectMapping() {
        Controller redirectToWebUi = (request, response) -> {
            // not sendRedirect: the servlet container would make the location absolute
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader(HttpHeaders.LOCATION, "web/");
            return null;
        };
        Map<String, Controller> urlMap = Map.of("/", redirectToWebUi, "/index.html", redirectToWebUi);
        return new SimpleUrlHandlerMapping(urlMap, WEB_DISPATCHER_MAPPING_ORDER - 1);
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
