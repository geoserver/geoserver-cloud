/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.jndi;

import java.util.Objects;

/**
 * @since 1.0
 */
class SimpleNameClassPair extends javax.naming.NameClassPair {

    private static final long serialVersionUID = 1L;

    public SimpleNameClassPair(String name, String className) {
        super(name, className);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof javax.naming.NameClassPair p) {
            return Objects.equals(getName(), p.getName()) && Objects.equals(getClassName(), p.getClassName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getClassName());
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
