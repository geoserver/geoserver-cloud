/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.jndi;

import java.util.Objects;

/**
 * @since 1.0
 */
class NameClassPair extends javax.naming.NameClassPair {

    private static final long serialVersionUID = 1L;

    public NameClassPair(String name, String className) {
        super(name, className);
    }

    public @Override boolean equals(Object o) {
        if (!(o instanceof javax.naming.NameClassPair)) return false;
        javax.naming.NameClassPair p = (javax.naming.NameClassPair) o;
        return Objects.equals(getName(), p.getName())
                && Objects.equals(getClassName(), p.getClassName());
    }

    public @Override String toString() {
        return super.toString();
    }
}
