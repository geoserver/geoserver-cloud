/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.wicket.protocol.http.mock.MockServletContext;
import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.web.GeoServerApplication;

/**
 * Creates {@link WicketTester}s with an explicit webapp root.
 *
 * <p>Without one, Wicket's {@link MockServletContext} resolves {@code getRealPath("/")} through the classpath, where
 * the first {@code META-INF/resources} entry found may sit inside a jar, and GeoServer's home page turns that path into
 * a {@link java.io.File}, which fails with "URI is not hierarchical". A directory of its own also keeps the data
 * directory from looking embedded in the webapp.
 */
public final class WicketTesters {

    private WicketTesters() {
        // static factory holder
    }

    /**
     * @param app the application under test
     * @param directory the test's temporary directory, under which the webapp root is created
     */
    public static WicketTester create(GeoServerApplication app, Path directory) throws IOException {
        Path webappRoot = Files.createDirectories(directory.resolve("webapp"));
        return new WicketTester(app, new MockServletContext(app, webappRoot.toString()), true);
    }
}
