/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.test.inaccessible;

/** Public bean class with a package-private constructor taking an argument. */
public class PackagePrivateArgConstructorComponent {

    private final String name;

    PackagePrivateArgConstructorComponent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
