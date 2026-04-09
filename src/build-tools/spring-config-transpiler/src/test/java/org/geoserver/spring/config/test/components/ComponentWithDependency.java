/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.test.components;

import org.geoserver.config.GeoServer;
import org.springframework.stereotype.Component;

/** Component with single-parameter constructor for testing constructor autowiring in GENERATE mode. */
@Component
public class ComponentWithDependency {

    private final GeoServer geoServer;

    public ComponentWithDependency(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    public GeoServer getGeoServer() {
        return geoServer;
    }
}
