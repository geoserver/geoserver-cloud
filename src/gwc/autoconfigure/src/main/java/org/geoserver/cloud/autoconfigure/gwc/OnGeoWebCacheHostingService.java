/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc;

import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerGWC;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerREST;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWMS;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;

/**
 * Matches when the running instance serves one of the GeoServer roles that host the GeoWebCache integration: wms, gwc,
 * webui, or restconfig.
 *
 * <p>On a combined app classpath that unions every GeoServer service, {@code gwc.enabled} alone no longer identifies
 * which roles should run GeoWebCache, since the gwc jars are present regardless of which services are running. The
 * per-service applications drew that line through their dependencies: wms, gwc, webui and restconfig shipped the gwc
 * starter, being the roles that serve tiles or originate the catalog changes tile layers must follow, while wfs, wcs
 * and wps never did.
 *
 * <p>The line matters beyond footprint. The GWC mediator is built with a {@code coalescedRequestSplitter} that renders
 * the non-cached members of a multi-layer tile request through the WMS {@code GetMap} bean, and the minimal WMS that
 * supplies it where the WMS service itself is absent is a near-complete WMS 1.1.1 that must not leak into the roles
 * that never had one.
 *
 * @since 3.1.0
 */
class OnGeoWebCacheHostingService extends AnyNestedCondition {

    OnGeoWebCacheHostingService() {
        super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnGeoServerWMS
    static class OnWms {}

    @ConditionalOnGeoServerGWC
    static class OnGwc {}

    @ConditionalOnGeoServerWebUI
    static class OnWebUi {}

    @ConditionalOnGeoServerREST
    static class OnRestConfig {}
}
