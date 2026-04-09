/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.test.components;

import org.springframework.stereotype.Component;

/** Component with a package-private no-arg constructor for testing reflection-based instantiation. */
@Component
public class PackagePrivateConstructorComponent {

    PackagePrivateConstructorComponent() {}
}
