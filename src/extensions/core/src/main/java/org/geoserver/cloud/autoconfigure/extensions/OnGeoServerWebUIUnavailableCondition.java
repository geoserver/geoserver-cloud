/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions;

import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;

/**
 * Condition backing {@link ConditionalOnGeoServerWebUIUnavailable}: matches when the {@code webui} service is not
 * enabled.
 *
 * <p>Implemented as the logical negation of {@link ConditionalOnGeoServerWebUI} rather than a direct classpath check.
 * This stays correct both when the Web UI jar is simply absent, and when the Web UI classes are always on the classpath
 * but the service itself is disabled, as in an application that bundles every service and picks the active ones through
 * configuration.
 */
class OnGeoServerWebUIUnavailableCondition extends NoneNestedConditions {

    OnGeoServerWebUIUnavailableCondition() {
        super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnGeoServerWebUI
    static class WebUIAvailable {}
}
