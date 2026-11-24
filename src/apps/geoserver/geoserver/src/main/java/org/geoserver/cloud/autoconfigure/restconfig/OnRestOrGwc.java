/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.restconfig;

import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerGWC;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerREST;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;

/**
 * Matches when either the REST configuration service or the GWC service is enabled, the two services that answer over
 * GeoServer's REST infrastructure.
 *
 * <p>Evaluated at {@link ConfigurationPhase#PARSE_CONFIGURATION} because
 * {@link GeoServerRestFrameworkAutoConfiguration} has to be decided before Spring Boot's Web MVC auto-configuration is
 * parsed. Both nested conditions read the classpath and the {@code geoserver.services} property, neither of which
 * depends on other beans, hence the earlier phase costs nothing.
 */
class OnRestOrGwc extends AnyNestedCondition {

    OnRestOrGwc() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnGeoServerREST
    static class RestEnabled {}

    @ConditionalOnGeoServerGWC
    static class GwcEnabled {}
}
