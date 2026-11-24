/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.app;

import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerGWC;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUIUnavailable;
import org.geoserver.gwc.dispatch.GeoServerGWCDispatcherController;
import org.geoserver.rest.catalog.AdminRequestCallback;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the GeoWebCache service in the combined GeoServer application.
 *
 * <p>Registers the GWC dispatcher controller and the admin-request callback that the GWC REST endpoints depend on.
 *
 * <p>Two beans the GWC REST endpoints also need are deliberately not declared here, because the REST configuration
 * service needs them too and each service declaring its own would conflict when both run in the same application
 * context. GeoServer's {@code RestConfiguration}, which configures Spring MVC for both, comes from
 * {@code GeoServerRestFrameworkAutoConfiguration}, and the {@code suffixStripFilter} bean from
 * {@code SuffixStripFilterAutoConfiguration}.
 */
@AutoConfiguration
@ConditionalOnGeoServerGWC
public class GwcApplicationAutoConfiguration {

    /**
     * Replaces component scanning on package {@code org.geoserver.gwc.dispatch} from
     * {@literal jar:gs-gwc-<version>.jar!/geowebcache-servlet.xml}
     */
    @Bean
    GeoServerGWCDispatcherController gwcDispatcherController() {
        return new GeoServerGWCDispatcherController();
    }

    @Bean
    @ConditionalOnMissingBean
    AdminRequestCallback adminRequestCallback() {
        return new AdminRequestCallback();
    }

    @Bean
    @ConditionalOnGeoServerWebUIUnavailable
    RootRedirectController gwcRootRedirectController() {
        return new RootRedirectController();
    }
}
