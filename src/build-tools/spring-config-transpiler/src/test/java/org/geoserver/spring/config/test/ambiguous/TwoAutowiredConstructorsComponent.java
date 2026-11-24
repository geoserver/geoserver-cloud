/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.test.ambiguous;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Two optional autowired constructors: Spring chooses by the beans available when it creates the component. */
@Component
public class TwoAutowiredConstructorsComponent {

    @Autowired(required = false)
    public TwoAutowiredConstructorsComponent(GeoServer geoServer) {
        // the arguments are irrelevant to the tests
    }

    @Autowired(required = false)
    public TwoAutowiredConstructorsComponent(GeoServer geoServer, GeoServerResourceLoader resourceLoader) {
        // the arguments are irrelevant to the tests
    }
}
