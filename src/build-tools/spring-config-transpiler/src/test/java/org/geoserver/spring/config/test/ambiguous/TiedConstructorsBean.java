/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.test.ambiguous;

/** Two constructors accepting a {@code String} equally: Spring would pick one of them in reflection order. */
public class TiedConstructorsBean {

    public TiedConstructorsBean(CharSequence value) {
        // the value is irrelevant to the tests
    }

    public TiedConstructorsBean(Comparable<?> value) {
        // the value is irrelevant to the tests
    }
}
