/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.autoconfigure.vectorformats;

/**
 * A simple mock class for testing the filtering system.
 * We don't implement DataAccessFactory due to incompatibility issues with the classpath.
 */
public class MockDataAccessFactory {
    private final String name;

    public MockDataAccessFactory(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return name;
    }

    public String getDescription() {
        return "Test factory: " + name;
    }
}
