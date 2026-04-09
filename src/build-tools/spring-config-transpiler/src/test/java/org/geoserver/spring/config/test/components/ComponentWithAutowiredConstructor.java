/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.test.components;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Component with multiple constructors where one is @Autowired, and a @Qualifier on one parameter. Tests that GENERATE
 * mode picks the @Autowired constructor and propagates @Qualifier annotations.
 */
@Component
public class ComponentWithAutowiredConstructor {

    private final GeoServer geoServer;
    private final GeoServerResourceLoader resourceLoader;

    @Autowired
    public ComponentWithAutowiredConstructor(
            @Qualifier("geoServer") GeoServer geoServer, GeoServerResourceLoader resourceLoader) {
        this.geoServer = geoServer;
        this.resourceLoader = resourceLoader;
    }

    public ComponentWithAutowiredConstructor(GeoServer geoServer) {
        this(geoServer, null);
    }

    public GeoServer getGeoServer() {
        return geoServer;
    }

    public GeoServerResourceLoader getResourceLoader() {
        return resourceLoader;
    }
}
