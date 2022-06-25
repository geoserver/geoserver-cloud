/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.platform.config;

/**
 * @since 1.0
 */
public interface UpdateSequence {

    /**
     * @return the currently observed update sequence value
     */
    long currValue();

    long nextValue();
}
